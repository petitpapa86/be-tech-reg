package com.bcbs239.regtech.ingestion.infrastructure.security;

import com.bcbs239.regtech.core.security.PermissionService;
import com.bcbs239.regtech.core.security.SecurityContext;
import com.bcbs239.regtech.core.security.SecurityUtils;
import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.ingestion.domain.bankinfo.BankId;
import com.bcbs239.regtech.ingestion.domain.batch.BatchId;
import com.bcbs239.regtech.ingestion.infrastructure.monitoring.IngestionLoggingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Security service for the ingestion module that leverages existing security infrastructure.
 * Provides JWT authentication, authorization, and audit logging for ingestion operations.
 * 
 * Requirements: 10.3, 10.4
 */
@Service
public class IngestionSecurityService {

    private static final Logger logger = LoggerFactory.getLogger(IngestionSecurityService.class);
    
    private final PermissionService permissionService;
    private final IngestionLoggingService loggingService;

    public IngestionSecurityService(PermissionService permissionService, 
                                  IngestionLoggingService loggingService) {
        this.permissionService = permissionService;
        this.loggingService = loggingService;
    }

    /**
     * Validate JWT token and extract bank ID using existing security infrastructure.
     * Logs all access attempts for audit trail.
     */
    public Result<BankId> validateTokenAndExtractBankId(String authToken) {
        long startTime = System.currentTimeMillis();
        String clientIp = getCurrentClientIp();
        
        try {
            // Log access attempt
            logAccessAttempt("TOKEN_VALIDATION", authToken, clientIp, "started");
            
            // Validate token using existing infrastructure
            if (!permissionService.isValidToken(authToken)) {
                long duration = System.currentTimeMillis() - startTime;
                logAccessAttempt("TOKEN_VALIDATION", authToken, clientIp, "failed", "Invalid token", duration, null);
                return Result.failure(new ErrorDetail("AUTHENTICATION_ERROR", "Invalid or expired JWT token"));
            }
            
            // Extract bank ID
            String bankIdValue = permissionService.getBankId(authToken);
            if (bankIdValue == null || bankIdValue.trim().isEmpty()) {
                long duration = System.currentTimeMillis() - startTime;
                logAccessAttempt("TOKEN_VALIDATION", authToken, clientIp, "failed", "Missing bank ID in token", duration, null);
                return Result.failure(new ErrorDetail("AUTHENTICATION_ERROR", "Bank ID not found in JWT token"));
            }
            
            BankId bankId = BankId.of(bankIdValue);
            long duration = System.currentTimeMillis() - startTime;
            
            // Log successful validation
            logAccessAttempt("TOKEN_VALIDATION", authToken, clientIp, "success", null, duration, bankId);
            
            return Result.success(bankId);
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            logAccessAttempt("TOKEN_VALIDATION", authToken, clientIp, "error", e.getMessage(), duration, null);
            logger.error("Error during token validation: {}", e.getMessage(), e);
            return Result.failure(new ErrorDetail("SYSTEM_ERROR", "Token validation failed: " + e.getMessage()));
        }
    }

    /**
     * Verify bank permissions for file access using existing security utilities.
     * Users can only access files from their own bank unless they have admin permissions.
     */
    public Result<Void> verifyBankPermissions(BankId requestedBankId) {
        long startTime = System.currentTimeMillis();
        String currentUser = SecurityUtils.getCurrentUserContext();
        
        try {
            // Log permission check attempt
            logPermissionCheck("BANK_ACCESS", requestedBankId.value(), currentUser, "started");
            
            // Use existing security utilities for bank access check
            if (!SecurityUtils.canAccessBank(requestedBankId.value())) {
                long duration = System.currentTimeMillis() - startTime;
                logPermissionCheck("BANK_ACCESS", requestedBankId.value(), currentUser, "denied", duration);
                
                return Result.failure(new ErrorDetail("AUTHORIZATION_ERROR", 
                    "Access denied. You can only access resources for your own bank."));
            }
            
            long duration = System.currentTimeMillis() - startTime;
            logPermissionCheck("BANK_ACCESS", requestedBankId.value(), currentUser, "granted", duration);
            
            return Result.success(null);
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            logPermissionCheck("BANK_ACCESS", requestedBankId.value(), currentUser, "error", duration, e.getMessage());
            logger.error("Error during bank permission check: {}", e.getMessage(), e);
            return Result.failure(new ErrorDetail("SYSTEM_ERROR", "Permission check failed: " + e.getMessage()));
        }
    }

    /**
     * Verify ingestion permissions using existing security infrastructure.
     */
    public Result<Void> verifyIngestionPermissions(String operation) {
        long startTime = System.currentTimeMillis();
        String currentUser = SecurityUtils.getCurrentUserContext();
        
        try {
            // Log permission check attempt
            logPermissionCheck("INGESTION_OPERATION", operation, currentUser, "started");
            
            boolean hasPermission = switch (operation.toLowerCase()) {
                case "upload" -> SecurityUtils.canPerformIngestion();
                case "status" -> SecurityUtils.canViewIngestionStatus();
                case "process" -> SecurityContext.hasPermission("ingestion:process");
                default -> false;
            };
            
            if (!hasPermission) {
                long duration = System.currentTimeMillis() - startTime;
                logPermissionCheck("INGESTION_OPERATION", operation, currentUser, "denied", duration);
                
                return Result.failure(new ErrorDetail("AUTHORIZATION_ERROR", 
                    String.format("Access denied. Required permission for operation: %s", operation)));
            }
            
            long duration = System.currentTimeMillis() - startTime;
            logPermissionCheck("INGESTION_OPERATION", operation, currentUser, "granted", duration);
            
            return Result.success(null);
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            logPermissionCheck("INGESTION_OPERATION", operation, currentUser, "error", duration, e.getMessage());
            logger.error("Error during ingestion permission check: {}", e.getMessage(), e);
            return Result.failure(new ErrorDetail("SYSTEM_ERROR", "Permission check failed: " + e.getMessage()));
        }
    }

    /**
     * Verify batch access permissions - users can only access batches from their own bank.
     */
    public Result<Void> verifyBatchAccess(BatchId batchId, BankId batchBankId) {
        long startTime = System.currentTimeMillis();
        String currentUser = SecurityUtils.getCurrentUserContext();
        
        try {
            // Log batch access attempt
            logBatchAccessAttempt(batchId, batchBankId, currentUser, "started");
            
            // Use existing security utilities for batch access check
            if (!SecurityUtils.canAccessBatch(batchBankId.value())) {
                long duration = System.currentTimeMillis() - startTime;
                logBatchAccessAttempt(batchId, batchBankId, currentUser, "denied", duration);
                
                return Result.failure(new ErrorDetail("AUTHORIZATION_ERROR", 
                    "Access denied. You can only access batches from your own bank."));
            }
            
            long duration = System.currentTimeMillis() - startTime;
            logBatchAccessAttempt(batchId, batchBankId, currentUser, "granted", duration);
            
            return Result.success(null);
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            logBatchAccessAttempt(batchId, batchBankId, currentUser, "error", duration, e.getMessage());
            logger.error("Error during batch access check: {}", e.getMessage(), e);
            return Result.failure(new ErrorDetail("SYSTEM_ERROR", "Batch access check failed: " + e.getMessage()));
        }
    }

    /**
     * Get current user context for logging and auditing.
     */
    public String getCurrentUserContext() {
        return SecurityUtils.getCurrentUserContext();
    }

    /**
     * Get current user permissions for detailed logging.
     */
    public Set<String> getCurrentUserPermissions() {
        return SecurityContext.getCurrentUserPermissions();
    }

    /**
     * Get current bank ID from security context.
     */
    public String getCurrentBankId() {
        return SecurityContext.getCurrentBankId();
    }

    /**
     * Get current user ID from security context.
     */
    public String getCurrentUserId() {
        return SecurityContext.getCurrentUserId();
    }

    // Private helper methods for audit logging

    private void logAccessAttempt(String operation, String authToken, String clientIp, 
                                String status, String errorMessage, Long durationMs, BankId bankId) {
        Map<String, Object> details = new HashMap<>();
        details.put("operation", operation);
        details.put("tokenPrefix", maskToken(authToken));
        details.put("clientIp", clientIp);
        details.put("status", status);
        details.put("timestamp", Instant.now().toString());
        details.put("userContext", SecurityUtils.getCurrentUserContext());
        
        if (errorMessage != null) {
            details.put("errorMessage", errorMessage);
        }
        if (durationMs != null) {
            details.put("durationMs", durationMs);
        }
        if (bankId != null) {
            details.put("bankId", maskBankId(bankId.value()));
        }
        
        loggingService.logRequestFlowStep("SECURITY_ACCESS_ATTEMPT", operation, details);
    }

    private void logAccessAttempt(String operation, String authToken, String clientIp, String status) {
        logAccessAttempt(operation, authToken, clientIp, status, null, null, null);
    }

    private void logPermissionCheck(String checkType, String resource, String userContext, 
                                  String result, Long durationMs, String errorMessage) {
        Map<String, Object> details = new HashMap<>();
        details.put("checkType", checkType);
        details.put("resource", resource);
        details.put("userContext", userContext);
        details.put("result", result);
        details.put("timestamp", Instant.now().toString());
        
        if (durationMs != null) {
            details.put("durationMs", durationMs);
        }
        if (errorMessage != null) {
            details.put("errorMessage", errorMessage);
        }
        
        loggingService.logRequestFlowStep("SECURITY_PERMISSION_CHECK", checkType, details);
    }

    private void logPermissionCheck(String checkType, String resource, String userContext, String result) {
        logPermissionCheck(checkType, resource, userContext, result, null, null);
    }

    private void logPermissionCheck(String checkType, String resource, String userContext, 
                                  String result, Long durationMs) {
        logPermissionCheck(checkType, resource, userContext, result, durationMs, null);
    }

    private void logBatchAccessAttempt(BatchId batchId, BankId batchBankId, String userContext, 
                                     String result, Long durationMs, String errorMessage) {
        Map<String, Object> details = new HashMap<>();
        details.put("batchId", batchId.value());
        details.put("batchBankId", maskBankId(batchBankId.value()));
        details.put("userContext", userContext);
        details.put("result", result);
        details.put("timestamp", Instant.now().toString());
        
        if (durationMs != null) {
            details.put("durationMs", durationMs);
        }
        if (errorMessage != null) {
            details.put("errorMessage", errorMessage);
        }
        
        loggingService.logRequestFlowStep("SECURITY_BATCH_ACCESS", "BATCH_ACCESS_CHECK", details);
    }

    private void logBatchAccessAttempt(BatchId batchId, BankId batchBankId, String userContext, String result) {
        logBatchAccessAttempt(batchId, batchBankId, userContext, result, null, null);
    }

    private void logBatchAccessAttempt(BatchId batchId, BankId batchBankId, String userContext, 
                                     String result, Long durationMs) {
        logBatchAccessAttempt(batchId, batchBankId, userContext, result, durationMs, null);
    }

    private String maskToken(String token) {
        if (token == null || token.length() <= 8) {
            return "[MASKED]";
        }
        return token.substring(0, 4) + "***" + token.substring(token.length() - 4);
    }

    private String maskBankId(String bankId) {
        if (bankId == null || bankId.length() <= 4) {
            return bankId;
        }
        return bankId.substring(0, 2) + "***" + bankId.substring(bankId.length() - 2);
    }

    private String getCurrentClientIp() {
        // In a real implementation, this would extract the client IP from the request
        // For now, return a placeholder
        return "unknown";
    }
}

