package com.bcbs239.regtech.ingestion.application.batch.upload;

import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import com.bcbs239.regtech.core.domain.shared.FieldError;

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
    private final AsyncBatchProcessor asyncBatchProcessor;
    private final ApplicationEventPublisher eventPublisher;
    private static final Logger log = LoggerFactory.getLogger(UploadAndProcessFileCommandHandler.class);

    public UploadAndProcessFileCommandHandler(
            IIngestionBatchRepository ingestionBatchRepository,
            IBankInfoRepository bankInfoRepository,
            TemporaryFileStorageService temporaryFileStorage,
            AsyncBatchProcessor asyncBatchProcessor,
            ApplicationEventPublisher eventPublisher) {
        this.ingestionBatchRepository = ingestionBatchRepository;
        this.bankInfoRepository = bankInfoRepository;
        this.temporaryFileStorage = temporaryFileStorage;
        this.asyncBatchProcessor = asyncBatchProcessor;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Result<BatchId> handle(UploadAndProcessFileCommand command) {
        long startTime = System.currentTimeMillis();

        BankId bankId = command.bankId();

        // Validate bank info
        var bankInfo = bankInfoRepository.findByBankId(bankId);
        if (bankInfo.isEmpty()) {
            return Result.failure(ErrorDetail.of("BANK_NOT_FOUND", ErrorType.VALIDATION_ERROR,
                String.format("Bank with ID '%s' not found", bankId.value()), "bank.not_found"));
        }
        if (!bankInfo.get().isActive()) {
            return Result.failure(ErrorDetail.of("BANK_INACTIVE", ErrorType.VALIDATION_ERROR,
                String.format("Bank with ID '%s' is not active", bankId.value()), "bank.inactive"));
        }
        // Validate file metadata
        Result<FileName> fileNameResult = FileName.create(command.fileName());
        Result<ContentType> contentTypeResult = ContentType.create(command.contentType());
        Result<FileSize> fileSizeResult = FileSize.create(command.fileSizeBytes());

        // Collect validation failures into a list and return all at once
        List<FieldError> validationErrors = new ArrayList<>();

        if (fileNameResult.isFailure()) {
            validationErrors.add(new FieldError("fileName",
                    fileNameResult.getError().map(ErrorDetail::getMessage).orElse("Invalid file name"),
                    "file.name.invalid"));
        }

        if (contentTypeResult.isFailure()) {
            validationErrors.add(new FieldError("contentType",
                    contentTypeResult.getError().map(ErrorDetail::getMessage).orElse("Invalid content type"),
                    "file.content_type.invalid"));
        }

        if (fileSizeResult.isFailure()) {
            validationErrors.add(new FieldError("fileSize",
                    fileSizeResult.getError().map(ErrorDetail::getMessage).orElse("Invalid file size"),
                    "file.size.invalid"));
        }

        // Get the validated value objects
        FileName validatedFileName = fileNameResult.getValue().orElseThrow();
        ContentType validatedContentType = contentTypeResult.getValue().orElseThrow();
        FileSize validatedFileSize = fileSizeResult.getValue().orElseThrow();

        // Validate file extension based on content type
        if (validatedContentType.isJson() && !validatedFileName.hasJsonExtension()) {
            validationErrors.add(new FieldError("fileName",
                    "JSON files must have .json extension",
                    "file.invalid_extension"));
        }
        if (validatedContentType.isExcel() && !validatedFileName.hasExcelExtension()) {
            validationErrors.add(new FieldError("fileName",
                    "Excel files must have .xlsx or .xls extension",
                    "file.invalid_extension"));
        }

        if (!validationErrors.isEmpty()) {
            return Result.failure(ErrorDetail.validationError(validationErrors));
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

            // Calculate MD5 checksum from stored file data (not from the already-consumed stream)
            Result<TemporaryFileStorageService.FileData> fileDataResult = temporaryFileStorage.retrieveFile(tempFileKey);
            if (fileDataResult.isFailure()) {
                temporaryFileStorage.removeFile(tempFileKey);
                return Result.failure(fileDataResult.getError().orElseThrow());
            }

            TemporaryFileStorageService.FileData fileData = fileDataResult.getValue().orElseThrow();
            Result<String> md5Result = calculateMD5ChecksumFromBytes(fileData.data());
            if (md5Result.isFailure()) {
                temporaryFileStorage.removeFile(tempFileKey);
                return Result.failure(md5Result.getError().orElseThrow());
            }
            String md5Checksum = md5Result.getValue().orElseThrow();

            // Create file metadata
            FileMetadata fileMetadata = new FileMetadata(
                command.fileName(),
                command.contentType(),
                validatedFileSize.getBytes(),
                md5Checksum,
                null  // S3 reference will be set during processing
            );

            BatchId batchId = BatchId.generate();
            IngestionBatch batch = new IngestionBatch(batchId, bankId, fileMetadata);

            log.info("Upload and process started; details={}", Map.of("batchId", batchId.value(), "bankId", bankId.value(),
                       "fileName", command.fileName(), "fileSize", command.fileSizeBytes()));

            Result<IngestionBatch> saveResult = ingestionBatchRepository.save(batch);
            if (saveResult.isFailure()) {
                temporaryFileStorage.removeFile(tempFileKey);
                return Result.failure(saveResult.getError().orElse(
                        ErrorDetail.of("DATABASE_ERROR", ErrorType.SYSTEM_ERROR,
                            "Failed to save batch record", "database.error")
                ));
            }

            // Start batch processing asynchronously AFTER the upload transaction commits.
            // This avoids the "UnexpectedRollbackException" pattern where an exception is caught
            // inside a @Transactional method, leaving the transaction marked rollback-only.
            Runnable startProcessing = () -> {
                CompletableFuture<Result<Void>> processFuture = asyncBatchProcessor.processBatchWithTempFile(batchId, tempFileKey);

                // Handle async processing completion (fire and forget)
                processFuture.whenComplete((processResult, throwable) -> {
                    try {
                        if (throwable != null) {
                            log.error("Async batch processing failed with exception; details={}", Map.of("batchId", batchId.value()), throwable);
                            publishProcessingFailureEvent(batchId, bankId, command.fileName(), throwable.getMessage(), "EXCEPTION", tempFileKey);
                        } else if (processResult != null && processResult.isFailure()) {
                            ErrorDetail error = processResult.getError().orElse(null);
                            String errorMessage = error != null ? error.getMessage() : "Unknown error";
                            String errorType = error != null ? error.getErrorType().name() : "UNKNOWN";
                            log.info("Async batch processing failed; details={}", Map.of("batchId", batchId.value(), "error", errorMessage));
                            publishProcessingFailureEvent(batchId, bankId, command.fileName(), errorMessage, errorType, tempFileKey);
                        }
                    } finally {
                        // IMPORTANT: cleanup temp file only after processing completes
                        temporaryFileStorage.removeFile(tempFileKey);
                    }
                });
            };

            if (TransactionSynchronizationManager.isActualTransactionActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        startProcessing.run();
                    }
                });
            } else {
                startProcessing.run();
            }

            long duration = System.currentTimeMillis() - startTime;
            log.info("Upload completed, batch processing started asynchronously; details={}", Map.of("batchId", batchId.value(), "bankId", bankId.value(), "duration", duration));

            return Result.success(batchId);

        } catch (Exception e) {

            temporaryFileStorage.removeFile(tempFileKey);
            throw e;
        }
    }

    private Result<String> calculateMD5ChecksumFromBytes(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(data);
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return Result.success(sb.toString());
        } catch (NoSuchAlgorithmException e) {
            return Result.failure(ErrorDetail.of("MD5_NOT_AVAILABLE", ErrorType.SYSTEM_ERROR,
                    "MD5 algorithm not available", "checksum.md5_unavailable"));
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