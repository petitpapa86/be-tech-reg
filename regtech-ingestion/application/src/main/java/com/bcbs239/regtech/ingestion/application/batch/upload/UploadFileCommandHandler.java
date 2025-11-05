package com.bcbs239.regtech.ingestion.application.batch.upload;

import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.ingestion.domain.bankinfo.BankId;
import com.bcbs239.regtech.ingestion.domain.batch.BatchId;
import com.bcbs239.regtech.ingestion.domain.batch.FileMetadata;
import com.bcbs239.regtech.ingestion.domain.batch.IIngestionBatchRepository;
import com.bcbs239.regtech.ingestion.domain.batch.IngestionBatch;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Command handler for uploading files for ingestion processing.
 * Handles validation, rate limiting, batch creation, and initial storage.
 */
@Component
public class UploadFileCommandHandler {

    private final IIngestionBatchRepository ingestionBatchRepository;
    private final FileUploadValidationService fileUploadValidationService;
    private final JwtTokenService jwtTokenService;
    private final RateLimitingService rateLimitingService;
    private final IngestionLoggingService loggingService;

    public UploadFileCommandHandler(
            IIngestionBatchRepository ingestionBatchRepository,
            FileUploadValidationService fileUploadValidationService,
            JwtTokenService jwtTokenService,
            RateLimitingService rateLimitingService,
            IngestionLoggingService loggingService) {
        this.ingestionBatchRepository = ingestionBatchRepository;
        this.fileUploadValidationService = fileUploadValidationService;
        this.jwtTokenService = jwtTokenService;
        this.rateLimitingService = rateLimitingService;
        this.loggingService = loggingService;
    }

    /**
     * Handle the upload file command.
     * 1. Validate JWT token and extract bank ID
     * 2. Apply rate limiting per bank
     * 3. Validate file size and content type
     * 4. Create IngestionBatch aggregate
     * 5. Store initial batch record
     * 6. Return batch ID for status tracking
     */
    public Result<BatchId> handle(UploadFileCommand command) {
        long startTime = System.currentTimeMillis();


        // 1. Validate JWT token and extract bank ID using JWT token service
        Result<BankId> bankIdResult = jwtTokenService.validateTokenAndExtractBankId(command.authToken());
        if (bankIdResult.isFailure()) {
            return Result.failure(bankIdResult.getError().orElseThrow());
        }

        BankId bankId = bankIdResult.getValue().orElseThrow();

        // 2. Apply rate limiting per bank
        Result<Void> rateLimitCheck = rateLimitingService.checkRateLimit(bankId);
        if (rateLimitCheck.isFailure()) {
            return Result.failure(rateLimitCheck.getError().orElseThrow());
        }

        // 3. Validate file size and content type
        Result<Void> fileValidation = fileUploadValidationService.validateUpload(
                command.fileName(),
                command.contentType(),
                command.fileSizeBytes()
        );
        if (fileValidation.isFailure()) {
            return Result.failure(fileValidation.getError().orElseThrow());
        }

        // 4. Generate unique batch ID
        BatchId batchId = BatchId.generate();

        // 5. Calculate file checksum for integrity
        String md5Checksum = calculateMD5Checksum(command.fileStream());

        // 6. Create file metadata
        FileMetadata fileMetadata = new FileMetadata(
                command.fileName(),
                command.contentType(),
                command.fileSizeBytes(),
                md5Checksum,
                null
        );

        // 7. Create IngestionBatch aggregate
        IngestionBatch batch = new IngestionBatch(batchId, bankId, fileMetadata);

        // 8. Log file upload started for audit trail
        loggingService.logFileUploadStarted(batchId, bankId, command.fileName(),
                command.fileSizeBytes(), command.contentType());

        // 9. Store initial batch record
        Result<IngestionBatch> saveResult = ingestionBatchRepository.save(batch);
        if (saveResult.isFailure()) {
            return Result.failure(saveResult.getError().orElse(
                    new ErrorDetail("DATABASE_ERROR", "Failed to save batch record")
            ));
        }

        // 10. Log successful upload completion for audit trail
        long duration = System.currentTimeMillis() - startTime;
        loggingService.logFileUploadCompleted(batchId, bankId, duration);

        // 11. Return batch ID for status tracking
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

    // These services will be migrated to infrastructure layer
    // For now, creating placeholder interfaces
    public interface FileUploadValidationService {
        Result<Void> validateUpload(String fileName, String contentType, long fileSizeBytes);
    }

    public interface JwtTokenService {
        Result<BankId> validateTokenAndExtractBankId(String token);
    }

    public interface RateLimitingService {
        Result<Void> checkRateLimit(BankId bankId);
    }

    public interface IngestionLoggingService {
        void logFileUploadStarted(BatchId batchId, BankId bankId, String fileName, long fileSizeBytes, String contentType);

        void logFileUploadCompleted(BatchId batchId, BankId bankId, long duration);
    }
}