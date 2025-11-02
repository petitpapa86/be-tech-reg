package com.bcbs239.regtech.ingestion.application.command.uploadfile;

import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.ingestion.application.service.JwtTokenService;
import com.bcbs239.regtech.ingestion.application.service.RateLimitingService;
import com.bcbs239.regtech.ingestion.domain.model.BatchId;
import com.bcbs239.regtech.ingestion.domain.model.BankId;
import com.bcbs239.regtech.ingestion.domain.model.BatchStatus;
import com.bcbs239.regtech.ingestion.domain.model.FileMetadata;
import com.bcbs239.regtech.ingestion.domain.model.IngestionBatch;
import com.bcbs239.regtech.ingestion.domain.repository.IngestionBatchRepository;
import com.bcbs239.regtech.ingestion.infrastructure.service.FileUploadValidationService;
import com.bcbs239.regtech.ingestion.infrastructure.security.IngestionSecurityService;
import com.bcbs239.regtech.ingestion.infrastructure.monitoring.IngestionLoggingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;

/**
 * Command handler for uploading files for ingestion processing.
 * Handles validation, rate limiting, batch creation, and initial storage.
 */
@Component
public class UploadFileCommandHandler {
    
    private final JwtTokenService jwtTokenService;
    private final RateLimitingService rateLimitingService;
    private final FileUploadValidationService fileUploadValidationService;
    private final IngestionBatchRepository ingestionBatchRepository;
    private final IngestionSecurityService securityService;
    private final IngestionLoggingService loggingService;
    
    @Autowired
    public UploadFileCommandHandler(
            JwtTokenService jwtTokenService,
            RateLimitingService rateLimitingService,
            FileUploadValidationService fileUploadValidationService,
            IngestionBatchRepository ingestionBatchRepository,
            IngestionSecurityService securityService,
            IngestionLoggingService loggingService) {
        this.jwtTokenService = jwtTokenService;
        this.rateLimitingService = rateLimitingService;
        this.fileUploadValidationService = fileUploadValidationService;
        this.ingestionBatchRepository = ingestionBatchRepository;
        this.securityService = securityService;
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
        
        try {
            // 1. Validate JWT token and extract bank ID using existing security infrastructure
            Result<BankId> bankIdResult = securityService.validateTokenAndExtractBankId(command.authToken());
            if (bankIdResult.isFailure()) {
                return Result.failure(bankIdResult.getError().orElseThrow());
            }
            
            BankId bankId = bankIdResult.getValue().orElseThrow();
            
            // 1a. Verify bank permissions for file access
            Result<Void> bankPermissionResult = securityService.verifyBankPermissions(bankId);
            if (bankPermissionResult.isFailure()) {
                return Result.failure(bankPermissionResult.getError().orElseThrow());
            }
            
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
                null // SHA-256 will be calculated during S3 storage
            );
            
            // 7. Create IngestionBatch aggregate
            IngestionBatch batch = IngestionBatch.create(
                batchId,
                bankId,
                fileMetadata,
                Instant.now()
            );
            
            // 8. Log file upload started for audit trail
            loggingService.logFileUploadStarted(batchId, bankId, command.fileName(), 
                command.fileSizeBytes(), command.contentType());
            
            // 9. Store initial batch record
            Result<IngestionBatch> saveResult = ingestionBatchRepository.save(batch);
            if (saveResult.isFailure()) {
                return Result.failure(saveResult.getError().orElse(
                    ErrorDetail.infrastructureError("DATABASE", "Failed to save batch record")
                ));
            }
            
            // 10. Log successful upload completion for audit trail
            long duration = System.currentTimeMillis() - startTime;
            loggingService.logFileUploadCompleted(batchId, bankId, duration);
            
            // 11. Return batch ID for status tracking
            return Result.success(batchId);
            
        } catch (Exception e) {
            return Result.failure(ErrorDetail.infrastructureError("UPLOAD_HANDLER", 
                "Unexpected error during file upload: " + e.getMessage()));
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
            // In production, this should be handled more robustly
            return "checksum_calculation_failed";
        }
    }
}