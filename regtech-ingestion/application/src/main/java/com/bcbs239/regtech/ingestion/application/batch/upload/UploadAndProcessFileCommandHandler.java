package com.bcbs239.regtech.ingestion.application.batch.upload;

import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.ingestion.application.batch.process.ProcessBatchCommand;
import com.bcbs239.regtech.ingestion.application.batch.process.ProcessBatchCommandHandler;
import com.bcbs239.regtech.ingestion.domain.bankinfo.Bank;
import com.bcbs239.regtech.ingestion.domain.bankinfo.BankId;
import com.bcbs239.regtech.ingestion.domain.bankinfo.IBankInfoRepository;
import com.bcbs239.regtech.ingestion.domain.batch.BatchId;
import com.bcbs239.regtech.ingestion.domain.batch.FileMetadata;
import com.bcbs239.regtech.ingestion.domain.batch.IIngestionBatchRepository;
import com.bcbs239.regtech.ingestion.domain.batch.IngestionBatch;
import com.bcbs239.regtech.ingestion.domain.file.ContentType;
import com.bcbs239.regtech.ingestion.domain.file.FileName;
import com.bcbs239.regtech.ingestion.domain.file.FileSize;
import com.bcbs239.regtech.ingestion.application.batch.upload.TemporaryFileStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Command handler for uploading and immediately processing a file.
 * Combines the upload and process operations into a single transaction.
 */
@Component
@Slf4j
public class UploadAndProcessFileCommandHandler {

    private final IIngestionBatchRepository ingestionBatchRepository;
    private final IBankInfoRepository bankInfoRepository;
    private final TemporaryFileStorageService temporaryFileStorage;
    private final ProcessBatchCommandHandler processBatchCommandHandler;

    public UploadAndProcessFileCommandHandler(
            IIngestionBatchRepository ingestionBatchRepository,
            IBankInfoRepository bankInfoRepository,
            TemporaryFileStorageService temporaryFileStorage,
            ProcessBatchCommandHandler processBatchCommandHandler) {
        this.ingestionBatchRepository = ingestionBatchRepository;
        this.bankInfoRepository = bankInfoRepository;
        this.temporaryFileStorage = temporaryFileStorage;
        this.processBatchCommandHandler = processBatchCommandHandler;
    }

    /**
     * Handle the upload and process file command.
     * 1. Validate file and bank information
     * 2. Create IngestionBatch aggregate
     * 3. Store file temporarily
     * 4. Save initial batch record
     * 5. Immediately process the batch
     * 6. Clean up temporary file
     * 7. Return batch ID for status tracking
     */
    @Transactional
    public Result<BatchId> handle(UploadAndProcessFileCommand command) {
        long startTime = System.currentTimeMillis();

        BankId bankId = command.bankId();

        try {
            // 1. Load and validate bank information
            Result<Void> bankValidationResult = validateBankInfo(bankId);
            if (bankValidationResult.isFailure()) {
                return Result.failure(bankValidationResult.getError().orElseThrow());
            }

            // 2. Validate file metadata
            Result<FileMetadata> fileValidationResult = validateFileMetadata(command);
            if (fileValidationResult.isFailure()) {
                return Result.failure(fileValidationResult.getError().orElseThrow());
            }

            FileMetadata fileMetadata = fileValidationResult.getValue().orElseThrow();

            // 3. Generate unique batch ID
            BatchId batchId = BatchId.generate();

            // 4. Store file temporarily and get reference key
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
                // 5. Create IngestionBatch aggregate
                IngestionBatch batch = new IngestionBatch(batchId, bankId, fileMetadata);

                // 6. Log operation started
                log.info("Upload and process started - BatchId: {}, BankId: {}, FileName: {}, Size: {} bytes",
                        batchId.value(), bankId.value(), command.fileName(), command.fileSizeBytes());

                // 7. Save initial batch record
                Result<IngestionBatch> saveResult = ingestionBatchRepository.save(batch);
                if (saveResult.isFailure()) {
                    return Result.failure(saveResult.getError().orElse(
                            ErrorDetail.of("DATABASE_ERROR", ErrorType.SYSTEM_ERROR,
                                "Failed to save batch record", "database.error")
                    ));
                }

                // 8. Immediately process the batch using the temporary file
                Result<Void> processResult = processBatchWithTempFile(batchId, tempFileKey);
                if (processResult.isFailure()) {
                    // Batch processing failed, but batch record is already saved
                    // The batch will be in FAILED status, which is acceptable
                    log.warn("Batch processing failed for BatchId: {}, but batch record saved", batchId.value());
                    return Result.failure(processResult.getError().orElseThrow());
                }

                // 9. Log successful completion
                long duration = System.currentTimeMillis() - startTime;
                log.info("Upload and process completed - BatchId: {}, BankId: {}, Duration: {}ms",
                        batchId.value(), bankId.value(), duration);

                // 10. Return batch ID for status tracking
                return Result.success(batchId);

            } finally {
                // 11. Always clean up temporary file
                temporaryFileStorage.removeFile(tempFileKey);
            }

        } catch (Exception e) {
            log.error("Unexpected error during upload and process: {}", e.getMessage(), e);
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

    private Result<FileMetadata> validateFileMetadata(UploadAndProcessFileCommand command) {
        // Create value objects
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
            log.warn("Large JSON file detected: {} ({} bytes)", command.fileName(), command.fileSizeBytes());
        } else if (validatedContentType.isExcel() && validatedFileSize.shouldWarnForExcel()) {
            log.warn("Large Excel file detected: {} ({} bytes)", command.fileName(), command.fileSizeBytes());
        }

        // Calculate file checksum for integrity
        String md5Checksum = calculateMD5Checksum(command.fileStream());

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

    private Result<Void> processBatchWithTempFile(BatchId batchId, String tempFileKey) {
        try {
            // Retrieve the temporary file
            Result<TemporaryFileStorageService.FileData> fileDataResult =
                temporaryFileStorage.retrieveFile(tempFileKey);
            if (fileDataResult.isFailure()) {
                return Result.failure(fileDataResult.getError().orElseThrow());
            }

            TemporaryFileStorageService.FileData fileData = fileDataResult.getValue().orElseThrow();

            // Create process command with the temporary file stream
            ProcessBatchCommand processCommand = new ProcessBatchCommand(
                batchId,
                fileData.getInputStream()
            );

            // Execute the process command
            return processBatchCommandHandler.handle(processCommand);

        } catch (Exception e) {
            log.error("Failed to process batch with temporary file: {}", e.getMessage(), e);
            return Result.failure(ErrorDetail.of("PROCESS_ERROR", ErrorType.SYSTEM_ERROR,
                "Failed to process batch: " + e.getMessage(), "batch.process.error"));
        }
    }

    private String calculateMD5Checksum(java.io.InputStream inputStream) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[8192];
            int bytesRead;

            // Reset stream if possible
            if (inputStream.markSupported()) {
                inputStream.mark(Integer.MAX_VALUE);
            }

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                md.update(buffer, 0, bytesRead);
            }

            // Reset stream for later use
            if (inputStream.markSupported()) {
                inputStream.reset();
            }

            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();

        } catch (NoSuchAlgorithmException | IOException e) {
            // Return a placeholder checksum if calculation fails
            return "checksum_calculation_failed";
        }
    }
}