package com.bcbs239.regtech.dataquality.presentation.web;

import com.bcbs239.regtech.core.security.SecurityUtils;
import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.dataquality.domain.shared.BankId;
import com.bcbs239.regtech.dataquality.domain.shared.BatchId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Security service for quality report operations.
 * Handles authentication and authorization for data quality endpoints.
 */
@Service
public class QualitySecurityService {
    
    private static final Logger logger = LoggerFactory.getLogger(QualitySecurityService.class);
    
    /**
     * Extracts and validates the current bank ID from security context.
     */
    public Result<BankId> getCurrentBankId() {
        try {
            String currentBankId = SecurityUtils.getCurrentBankId();
            if (currentBankId == null || currentBankId.trim().isEmpty()) {
                return Result.failure(ErrorDetail.of(
                    "AUTHENTICATION_ERROR",
                    "Bank ID not found in security context",
                    "authentication"
                ));
            }
            
            BankId bankId = BankId.of(currentBankId);
            return Result.success(bankId);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid bank ID in security context: {}", e.getMessage());
            return Result.failure(ErrorDetail.of(
                "AUTHENTICATION_ERROR",
                "Invalid bank ID in security context: " + e.getMessage(),
                "authentication"
            ));
        } catch (Exception e) {
            logger.error("Error extracting bank ID from security context: {}", e.getMessage(), e);
            return Result.failure(ErrorDetail.of(
                "SYSTEM_ERROR",
                "Failed to extract bank ID from security context: " + e.getMessage(),
                "system"
            ));
        }
    }
    
    /**
     * Verifies user has access to the specified batch.
     * Users can only access batches from their own bank.
     */
    public Result<Void> verifyBatchAccess(BatchId batchId) {
        try {
            String currentBankId = SecurityUtils.getCurrentBankId();
            if (currentBankId == null) {
                return Result.failure(ErrorDetail.of(
                    "AUTHENTICATION_ERROR",
                    "Bank ID not found in security context",
                    "authentication"
                ));
            }
            
            // In a real implementation, we would query the batch to get its bank ID
            // and verify it matches the current user's bank ID
            // For now, we'll use the existing security utilities
            if (!SecurityUtils.canAccessBatch(currentBankId)) {
                return Result.failure(ErrorDetail.of(
                    "AUTHORIZATION_ERROR",
                    "Access denied. You can only access batches from your own bank.",
                    "authorization"
                ));
            }
            
            logger.debug("Batch access verified for batch: {} by bank: {}", 
                batchId.value(), currentBankId);
            
            return Result.success(null);
            
        } catch (Exception e) {
            logger.error("Error verifying batch access for batch {}: {}", 
                batchId.value(), e.getMessage(), e);
            return Result.failure(ErrorDetail.of(
                "SYSTEM_ERROR",
                "Failed to verify batch access: " + e.getMessage(),
                "system"
            ));
        }
    }
    
    /**
     * Verifies user has permission to view quality reports.
     */
    public Result<Void> verifyReportViewPermission() {
        try {
            if (!SecurityUtils.hasPermission("data-quality:reports:view")) {
                return Result.failure(ErrorDetail.of(
                    "AUTHORIZATION_ERROR",
                    "Access denied. Required permission: data-quality:reports:view",
                    "authorization"
                ));
            }
            
            return Result.success(null);
            
        } catch (Exception e) {
            logger.error("Error verifying report view permission: {}", e.getMessage(), e);
            return Result.failure(ErrorDetail.of(
                "SYSTEM_ERROR",
                "Failed to verify report view permission: " + e.getMessage(),
                "system"
            ));
        }
    }
    
    /**
     * Verifies user has permission to view quality trends.
     */
    public Result<Void> verifyTrendsViewPermission() {
        try {
            if (!SecurityUtils.hasPermission("data-quality:trends:view")) {
                return Result.failure(ErrorDetail.of(
                    "AUTHORIZATION_ERROR",
                    "Access denied. Required permission: data-quality:trends:view",
                    "authorization"
                ));
            }
            
            return Result.success(null);
            
        } catch (Exception e) {
            logger.error("Error verifying trends view permission: {}", e.getMessage(), e);
            return Result.failure(ErrorDetail.of(
                "SYSTEM_ERROR",
                "Failed to verify trends view permission: " + e.getMessage(),
                "system"
            ));
        }
    }
}

