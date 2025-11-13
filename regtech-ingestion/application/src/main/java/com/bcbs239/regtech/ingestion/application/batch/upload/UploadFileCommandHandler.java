package com.bcbs239.regtech.ingestion.application.batch.upload;

import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Command handler for uploading files for ingestion processing.
 * Handles validation, batch creation, and initial storage.
 */
@Component
public class UploadFileCommandHandler {

    private static final Logger log = LoggerFactory.getLogger(UploadFileCommandHandler.class);

    private final IIngestionBatchRepository ingestionBatchRepository;
    private final IBankInfoRepository bankInfoRepository;

    public UploadFileCommandHandler(
            IIngestionBatchRepository ingestionBatchRepository,
            IBankInfoRepository bankInfoRepository) {
        this.ingestionBatchRepository = ingestionBatchRepository;
        this.bankInfoRepository = bankInfoRepository;
    }

    /**
     * Handle the upload file command.
     * 1. Load bank information from database
     * 2. Validate file size and content type
     * 3. Create IngestionBatch aggregate
     * 4. Store initial batch record
     * 5. Return batch ID for status tracking
     */
    public Result<BatchId> handle(UploadFileCommand command) {
        long startTime = System.currentTimeMillis();

        BankId bankId = command.bankId();

        // 1. Load bank information from database
        var bankInfo = bankInfoRepository.findByBankId(bankId);
        if (bankInfo.isEmpty()) {
            return Result.failure(ErrorDetail.of("BANK_NOT_FOUND", ErrorType.VALIDATION_ERROR,
                String.format("Bank with ID '%s' not found", bankId.value()), "bank.not_found"));
        }

        if (!bankInfo.get().isActive()) {
            return Result.failure(ErrorDetail.of("BANK_INACTIVE", ErrorType.VALIDATION_ERROR,
                String.format("Bank with ID '%s' is not active", bankId.value()), "bank.inactive"));
        }

        // 2. Validate file size and content type
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
            return Result.failure(ErrorDetail.of("INVALID_FILE_EXTENSION", ErrorType.SYSTEM_ERROR, "JSON files must have .json extension", "generic.error"));
        } else if (validatedContentType.isExcel() && !validatedFileName.hasExcelExtension()) {
            return Result.failure(ErrorDetail.of("INVALID_FILE_EXTENSION", ErrorType.SYSTEM_ERROR, "Excel files must have .xlsx or .xls extension", "generic.error"));
        }

        // Log warnings for large files
        if (validatedContentType.isJson() && validatedFileSize.shouldWarnForJson()) {
            log.warn("Large JSON file detected: {} ({} bytes)", command.fileName(), command.fileSizeBytes());
        } else if (validatedContentType.isExcel() && validatedFileSize.shouldWarnForExcel()) {
            log.warn("Large Excel file detected: {} ({} bytes)", command.fileName(), command.fileSizeBytes());
        }

        // 3. Generate unique batch ID
        BatchId batchId = BatchId.generate();

        // 4. Calculate file checksum for integrity
        String md5Checksum = calculateMD5Checksum(command.fileStream());

        // 5. Create file metadata
        FileMetadata fileMetadata = new FileMetadata(
                command.fileName(),
                command.contentType(),
                command.fileSizeBytes(),
                md5Checksum,
                null
        );

        // 6. Create IngestionBatch aggregate
        IngestionBatch batch = new IngestionBatch(batchId, bankId, fileMetadata);

        // 7. Log file upload started for audit trail
        log.info("File upload started - BatchId: {}, BankId: {}, FileName: {}, Size: {} bytes, ContentType: {}",
                batchId.value(), bankId.value(), command.fileName(), command.fileSizeBytes(), command.contentType());

        // 8. Store initial batch record
        Result<IngestionBatch> saveResult = ingestionBatchRepository.save(batch);
        if (saveResult.isFailure()) {
            return Result.failure(saveResult.getError().orElse(
                    ErrorDetail.of("DATABASE_ERROR", ErrorType.SYSTEM_ERROR, "Failed to save batch record", "database.error")
            ));
        }

        // 9. Log successful upload completion for audit trail
        long duration = System.currentTimeMillis() - startTime;
        log.info("File upload completed - BatchId: {}, BankId: {}, Duration: {}ms", batchId.value(), bankId.value(), duration);

        // 10. Return batch ID for status tracking
        return Result.success(batchId);
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
            // In production, this should be handled more robustly
            return "checksum_calculation_failed";
        }
    }

    // Note: JWT token validation and rate limiting are now handled at application level
    // Logging is done directly using SLF4J logger
}

