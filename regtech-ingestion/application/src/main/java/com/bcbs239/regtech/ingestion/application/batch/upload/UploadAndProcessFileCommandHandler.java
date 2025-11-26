package com.bcbs239.regtech.ingestion.application.batch.upload;

import org.springframework.context.ApplicationEventPublisher;
import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.ingestion.application.batch.process.ProcessBatchCommand;
import com.bcbs239.regtech.ingestion.application.batch.process.ProcessBatchCommandHandler;
import com.bcbs239.regtech.ingestion.application.common.TemporaryFileStorageService;
import com.bcbs239.regtech.ingestion.domain.bankinfo.BankId;
import com.bcbs239.regtech.ingestion.domain.bankinfo.IBankInfoRepository;
import com.bcbs239.regtech.ingestion.domain.batch.BatchId;
import com.bcbs239.regtech.ingestion.domain.batch.FileMetadata;
import com.bcbs239.regtech.ingestion.domain.batch.IIngestionBatchRepository;
import com.bcbs239.regtech.ingestion.domain.batch.IngestionBatch;
import com.bcbs239.regtech.ingestion.domain.batch.events.BatchProcessingFailedEvent;
import com.bcbs239.regtech.ingestion.domain.file.ContentType;
import com.bcbs239.regtech.ingestion.domain.file.FileName;
import com.bcbs239.regtech.ingestion.domain.file.FileSize;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Command handler for uploading and immediately processing a file.
 * Combines the upload and process operations into a single transaction.
 */
@Component
@EnableAsync
public class UploadAndProcessFileCommandHandler {

    private final IIngestionBatchRepository ingestionBatchRepository;
    private final IBankInfoRepository bankInfoRepository;
    private final TemporaryFileStorageService temporaryFileStorage;
    private final ProcessBatchCommandHandler processBatchCommandHandler;
    private final ApplicationEventPublisher eventPublisher;
    private static final Logger log = LoggerFactory.getLogger(UploadAndProcessFileCommandHandler.class);

    public UploadAndProcessFileCommandHandler(
            IIngestionBatchRepository ingestionBatchRepository,
            IBankInfoRepository bankInfoRepository,
            TemporaryFileStorageService temporaryFileStorage,
            ProcessBatchCommandHandler processBatchCommandHandler,
            ApplicationEventPublisher eventPublisher) {
        this.ingestionBatchRepository = ingestionBatchRepository;
        this.bankInfoRepository = bankInfoRepository;
        this.temporaryFileStorage = temporaryFileStorage;
        this.processBatchCommandHandler = processBatchCommandHandler;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Result<BatchId> handle(UploadAndProcessFileCommand command) {
        long startTime = System.currentTimeMillis();

        BankId bankId = command.bankId();

        try {
            Result<Void> bankValidationResult = validateBankInfo(bankId);
            if (bankValidationResult.isFailure()) {
                return Result.failure(bankValidationResult.getError().orElseThrow());
            }

            // Store file FIRST before any other operations that might consume the stream
            Result<String> tempStorageResult = temporaryFileStorage.storeFile(
                command.fileStream(),
                command.fileName(),
                command.contentType(),
                command.fileSizeBytes()
            );
            if (tempStorageResult.isFailure()) {
                return Result.failure(tempStorageResult.getError().orElseThrow());
            }

            String tempFileKey = tempStorageResult.getValue().orElseThrow();

            try {
                Result<FileMetadata> fileValidationResult = validateFileMetadata(command, tempFileKey);
                if (fileValidationResult.isFailure()) {
                    temporaryFileStorage.removeFile(tempFileKey); // Clean up on validation failure
                    return Result.failure(fileValidationResult.getError().orElseThrow());
                }

                FileMetadata fileMetadata = fileValidationResult.getValue().orElseThrow();

                BatchId batchId = BatchId.generate();
                IngestionBatch batch = new IngestionBatch(batchId, bankId, fileMetadata);

                log.info("Upload and process started; details={}", Map.of("batchId", batchId.value(), "bankId", bankId.value(),
                           "fileName", command.fileName(), "fileSize", command.fileSizeBytes()));

                Result<IngestionBatch> saveResult = ingestionBatchRepository.save(batch);
                if (saveResult.isFailure()) {
                    return Result.failure(saveResult.getError().orElse(
                            ErrorDetail.of("DATABASE_ERROR", ErrorType.SYSTEM_ERROR,
                                "Failed to save batch record", "database.error")
                    ));
                }

                // Start async batch processing
                CompletableFuture<Result<Void>> processFuture = processBatchWithTempFile(batchId, tempFileKey);

                // Handle async processing completion (fire and forget)
                processFuture.whenComplete((processResult, throwable) -> {
                    if (throwable != null) {
                        log.error("Async batch processing failed with exception; details={}", Map.of("batchId", batchId.value()), throwable);
                        publishProcessingFailureEvent(batchId, bankId, command.fileName(), throwable.getMessage(), "EXCEPTION", tempFileKey);
                    } else if (processResult != null && processResult.isFailure()) {
                        ErrorDetail error = processResult.getError().orElse(null);
                        String errorMessage = error != null ? error.getMessage() : "Unknown error";
                        String errorType = error != null ? error.getErrorType().name() : "UNKNOWN";
                        log.info("Async batch processing failed; details={}", Map.of("batchId", batchId.value(), "error", errorMessage));
                        publishProcessingFailureEvent(batchId, bankId, command.fileName(), errorMessage, errorType, tempFileKey);
                    } else {
                        log.info("Async batch processing completed successfully; details={}", Map.of("batchId", batchId.value()));
                    }
                });

                long duration = System.currentTimeMillis() - startTime;
                log.info("Upload completed, batch processing started asynchronously; details={}", Map.of("batchId", batchId.value(), "bankId", bankId.value(), "duration", duration));

                return Result.success(batchId);

            } finally {
                temporaryFileStorage.removeFile(tempFileKey);
            }

        } catch (Exception e) {
            log.error("Unexpected error during upload and process; details={}", Map.of("errorMessage", e.getMessage()), e);
            return Result.failure(ErrorDetail.of("UPLOAD_PROCESS_ERROR", ErrorType.SYSTEM_ERROR,
                "Unexpected error during file upload and processing: " + e.getMessage(),
                "upload.process.error"));
        }
    }

    private Result<Void> validateBankInfo(BankId bankId) {
        var bankInfo = bankInfoRepository.findByBankId(bankId);
        if (bankInfo.isEmpty()) {
            return Result.failure(ErrorDetail.of("BANK_NOT_FOUND", ErrorType.VALIDATION_ERROR,
                String.format("Bank with ID '%s' not found", bankId.value()), "bank.not_found"));
        }

        if (!bankInfo.get().isActive()) {
            return Result.failure(ErrorDetail.of("BANK_INACTIVE", ErrorType.VALIDATION_ERROR,
                String.format("Bank with ID '%s' is not active", bankId.value()), "bank.inactive"));
        }

        return Result.success(null);
    }

    private Result<FileMetadata> validateFileMetadata(UploadAndProcessFileCommand command, String tempFileKey) {
        Result<FileName> fileNameResult = FileName.create(command.fileName());
        if (fileNameResult.isFailure()) {
            return Result.failure(fileNameResult.getError().orElseThrow());
        }

        Result<ContentType> contentTypeResult = ContentType.create(command.contentType());
        if (contentTypeResult.isFailure()) {
            return Result.failure(contentTypeResult.getError().orElseThrow());
        }

        Result<FileSize> fileSizeResult = FileSize.create(command.fileSizeBytes());
        if (fileSizeResult.isFailure()) {
            return Result.failure(fileSizeResult.getError().orElseThrow());
        }

        // Get the value objects
        FileName validatedFileName = fileNameResult.getValue().orElseThrow();
        ContentType validatedContentType = contentTypeResult.getValue().orElseThrow();
        FileSize validatedFileSize = fileSizeResult.getValue().orElseThrow();

        // Validate file extension based on content type
        if (validatedContentType.isJson() && !validatedFileName.hasJsonExtension()) {
            return Result.failure(ErrorDetail.of("INVALID_FILE_EXTENSION", ErrorType.SYSTEM_ERROR,
                "JSON files must have .json extension", "generic.error"));
        } else if (validatedContentType.isExcel() && !validatedFileName.hasExcelExtension()) {
            return Result.failure(ErrorDetail.of("INVALID_FILE_EXTENSION", ErrorType.SYSTEM_ERROR,
                "Excel files must have .xlsx or .xls extension", "generic.error"));
        }

        // Log warnings for large files
        if (validatedContentType.isJson() && validatedFileSize.shouldWarnForJson()) {
            log.info("Large JSON file detected; details={}", Map.of("fileName", command.fileName(), "fileSize", command.fileSizeBytes()));
        } else if (validatedContentType.isExcel() && validatedFileSize.shouldWarnForExcel()) {
            log.info("Large Excel file detected; details={}", Map.of("fileName", command.fileName(), "fileSize", command.fileSizeBytes()));
        }

        // Calculate MD5 checksum from stored file data (not from the already-consumed stream)
        Result<TemporaryFileStorageService.FileData> fileDataResult = temporaryFileStorage.retrieveFile(tempFileKey);
        if (fileDataResult.isFailure()) {
            return Result.failure(fileDataResult.getError().orElseThrow());
        }
        
        TemporaryFileStorageService.FileData fileData = fileDataResult.getValue().orElseThrow();
        String md5Checksum = calculateMD5ChecksumFromBytes(fileData.data());

        // Create file metadata
        FileMetadata fileMetadata = new FileMetadata(
                command.fileName(),
                command.contentType(),
                command.fileSizeBytes(),
                md5Checksum,
                null  // S3 reference will be set during processing
        );

        return Result.success(fileMetadata);
    }

    @Async
    private CompletableFuture<Result<Void>> processBatchWithTempFile(BatchId batchId, String tempFileKey) {
        try {
            ProcessBatchCommand processCommand = new ProcessBatchCommand(
                batchId,
                tempFileKey
            );

            Result<Void> processResult = processBatchCommandHandler.handle(processCommand);
            return CompletableFuture.completedFuture(processResult);

        } catch (Exception e) {
            log.error("Failed to process batch with temporary file; details={}", Map.of("errorMessage", e.getMessage()), e);
            return CompletableFuture.completedFuture(Result.failure(ErrorDetail.of("PROCESS_ERROR", ErrorType.SYSTEM_ERROR,
                "Failed to process batch: " + e.getMessage(), "batch.process.error")));
        }
    }

    private String calculateMD5ChecksumFromBytes(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(data);
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not available", e);
        }
    }

    private void publishProcessingFailureEvent(
            BatchId batchId,
            BankId bankId,
            String fileName,
            String errorMessage,
            String errorType,
            String tempFileKey) {
        try {
            BatchProcessingFailedEvent event = new BatchProcessingFailedEvent(
                batchId.value(),
                bankId.value(),
                fileName,
                errorMessage,
                errorType,
                tempFileKey
            );
            eventPublisher.publishEvent(event);
            log.info("Published batch processing failure event; details={}", Map.of(
                "batchId", batchId.value(),
                "bankId", bankId.value(),
                "fileName", fileName
            ));
        } catch (Exception e) {
            log.error("Failed to publish batch processing failure event; details={}", Map.of(
                "batchId", batchId.value(),
                "errorMessage", e.getMessage()
            ), e);
        }
    }
}