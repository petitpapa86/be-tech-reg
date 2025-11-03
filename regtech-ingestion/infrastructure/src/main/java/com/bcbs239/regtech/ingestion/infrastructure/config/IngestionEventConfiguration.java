package com.bcbs239.regtech.ingestion.infrastructure.config;

import com.bcbs239.regtech.ingestion.application.batch.process.ProcessBatchCommandHandler;
import com.bcbs239.regtech.ingestion.application.batch.queries.BatchStatusQueryHandler;
import com.bcbs239.regtech.ingestion.application.batch.upload.UploadFileCommandHandler;
import com.bcbs239.regtech.ingestion.domain.integrationevents.BatchIngestedEvent;
import com.bcbs239.regtech.ingestion.infrastructure.events.IngestionOutboxEventPublisher;
import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.ingestion.domain.bankinfo.BankId;
import com.bcbs239.regtech.ingestion.domain.batch.BatchId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.Map;

/**
 * Configuration for ingestion event publishing and security services.
 * Provides the beans required by ingestion application handlers.
 */
@Configuration
public class IngestionEventConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(IngestionEventConfiguration.class);

    /**
     * Provides the IngestionOutboxEventPublisher implementation required by ProcessBatchCommandHandler.
     * This bean delegates to the existing IngestionOutboxEventPublisher infrastructure component.
     */
    @Bean
    public ProcessBatchCommandHandler.IngestionOutboxEventPublisher processBatchIngestionOutboxEventPublisher(
            IngestionOutboxEventPublisher outboxEventPublisher) {

        return new ProcessBatchCommandHandler.IngestionOutboxEventPublisher() {
            @Override
            public void publishBatchIngestedEvent(BatchIngestedEvent event) {
                outboxEventPublisher.publishBatchIngestedEvent(event);
            }
        };
    }

    /**
     * Provides the JwtTokenService implementation required by BatchStatusQueryHandler.
     * This is a simple mock implementation for development purposes.
     */
    @Bean
    public BatchStatusQueryHandler.JwtTokenService jwtTokenService() {
        return new BatchStatusQueryHandler.JwtTokenService() {
            @Override
            public Result<BankId> validateTokenAndExtractBankId(String token) {
                try {
                    // Simple mock implementation - extract bank ID from token
                    // In a real implementation, this would validate the JWT and extract claims
                    if (token == null || token.trim().isEmpty()) {
                        return Result.failure(com.bcbs239.regtech.core.shared.ErrorDetail.of(
                            "INVALID_TOKEN", "Token is null or empty", "auth.invalid_token"));
                    }

                    // For development, we'll use a simple format: "bank-{bankId}"
                    if (token.startsWith("bank-")) {
                        String bankIdValue = token.substring(5);
                        BankId bankId = BankId.of(bankIdValue);
                        logger.debug("Extracted bank ID {} from token", bankId.value());
                        return Result.success(bankId);
                    }

                    // Default to a test bank ID for development
                    logger.warn("Using default bank ID for token: {}", token);
                    return Result.success(BankId.of("TEST_BANK"));

                } catch (Exception e) {
                    logger.error("Failed to validate token and extract bank ID: {}", e.getMessage(), e);
                    return Result.failure(com.bcbs239.regtech.core.shared.ErrorDetail.of(
                        "TOKEN_VALIDATION_FAILED", "Failed to validate token: " + e.getMessage(), "auth.token_validation"));
                }
            }
        };
    }

    /**
     * Provides the IngestionSecurityService implementation required by BatchStatusQueryHandler.
     * This is a simple mock implementation for development purposes.
     */
    @Bean
    public BatchStatusQueryHandler.IngestionSecurityService batchStatusIngestionSecurityService() {
        return new BatchStatusQueryHandler.IngestionSecurityService() {
            @Override
            public Result<BankId> validateTokenAndExtractBankId(String token) {
                // Delegate to the JWT token service
                return jwtTokenService().validateTokenAndExtractBankId(token);
            }

            @Override
            public Result<Void> verifyBatchAccess(BatchId batchId, BankId bankId) {
                try {
                    // Simple mock implementation - in a real system this would check
                    // if the bank has access to the specific batch
                    if (batchId == null || bankId == null) {
                        return Result.failure(com.bcbs239.regtech.core.shared.ErrorDetail.of(
                            "INVALID_ACCESS_REQUEST", "Batch ID and Bank ID cannot be null", "auth.invalid_request"));
                    }

                    // For development, we'll allow access if the batch ID contains the bank ID
                    // This is a simple mock - real implementation would check database permissions
                    if (batchId.value().contains(bankId.value()) || batchId.value().startsWith("test-")) {
                        logger.debug("Access granted for bank {} to batch {}", bankId.value(), batchId.value());
                        return Result.success(null);
                    }

                    logger.warn("Access denied for bank {} to batch {}", bankId.value(), batchId.value());
                    return Result.failure(com.bcbs239.regtech.core.shared.ErrorDetail.of(
                        "ACCESS_DENIED", "Bank does not have access to this batch", "auth.access_denied"));

                } catch (Exception e) {
                    logger.error("Failed to verify batch access for batchId: {}, bankId: {}", batchId, bankId, e);
                    return Result.failure(com.bcbs239.regtech.core.shared.ErrorDetail.of(
                        "ACCESS_VERIFICATION_FAILED", "Failed to verify batch access: " + e.getMessage(), "auth.verification_error"));
                }
            }
        };
    }

    /**
     * Provides the IngestionLoggingService implementation required by BatchStatusQueryHandler.
     * This is a simple mock implementation for development purposes.
     */
    @Bean
    public BatchStatusQueryHandler.IngestionLoggingService batchStatusQueryHandlerLoggingService() {
        return new BatchStatusQueryHandler.IngestionLoggingService() {
            @Override
            public void logRequestFlowStep(String operation, String step, Map<String, Object> context) {
                logger.info("Request flow - Operation: {}, Step: {}, Context: {}", operation, step, context);
            }
        };
    }

    /**
     * Provides the IngestionLoggingService implementation required by UploadFileCommandHandler.
     * This is a simple mock implementation for development purposes.
     */
    @Bean
    public UploadFileCommandHandler.IngestionLoggingService uploadFileCommandHandlerLoggingService() {
        return new UploadFileCommandHandler.IngestionLoggingService() {
            @Override
            public void logFileUploadStarted(BatchId batchId, BankId bankId, String fileName, long fileSizeBytes, String contentType) {
                logger.info("File upload started - BatchId: {}, BankId: {}, FileName: {}, Size: {}, ContentType: {}",
                           batchId.value(), bankId.value(), fileName, fileSizeBytes, contentType);
            }

            @Override
            public void logFileUploadCompleted(BatchId batchId, BankId bankId, long duration) {
                logger.info("File upload completed - BatchId: {}, BankId: {}, Duration: {}ms",
                           batchId.value(), bankId.value(), duration);
            }
        };
    }
}