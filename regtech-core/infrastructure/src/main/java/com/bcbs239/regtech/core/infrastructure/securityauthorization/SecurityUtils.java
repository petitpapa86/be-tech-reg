package com.bcbs239.regtech.core.infrastructure.securityauthorization;

/**
 * Utility class with common security operations and permission checks.
 * Provides convenient methods for common authorization patterns.
 */
public class SecurityUtils {

    /**
     * Check if current user can access resources for a specific bank.
     * Users can only access resources for their own bank unless they have admin permissions.
     */
    public static boolean canAccessBank(String targetBankId) {
        // Admin users can access any bank
        if (SecurityContext.hasPermission("admin:all")) {
            return true;
        }

        // Users can access their own bank
        String currentBankId = SecurityContext.getCurrentBankId();
        return currentBankId != null && currentBankId.equals(targetBankId);
    }

    /**
     * Check if current user can perform ingestion operations.
     */
    public static boolean canPerformIngestion() {
        return SecurityContext.hasAnyPermission("ingestion:upload", "ingestion:process");
    }

    /**
     * Check if current user can view ingestion status.
     */
    public static boolean canViewIngestionStatus() {
        return SecurityContext.hasPermission("ingestion:status:view");
    }

    /**
     * Check if current user can access billing information.
     */
    public static boolean canAccessBilling() {
        return SecurityContext.hasAnyPermission("billing:read", "billing:write");
    }

    /**
     * Check if current user can manage users.
     */
    public static boolean canManageUsers() {
        return SecurityContext.hasAnyPermission("users:create", "users:update", "users:delete");
    }

    /**
     * Get current user context for logging and auditing.
     */
    public static String getCurrentUserContext() {
        String userId = SecurityContext.getCurrentUserId();
        String bankId = SecurityContext.getCurrentBankId();
        
        if (userId != null && bankId != null) {
            return String.format("User: %s, Bank: %s", userId, bankId);
        } else if (userId != null) {
            return String.format("User: %s", userId);
        } else {
            return "Anonymous";
        }
    }

    /**
     * Convenience accessor to get current bank id from security context.
     */
    public static String getCurrentBankId() {
        return SecurityContext.getCurrentBankId();
    }

    /**
     * Convenience wrapper to check a single permission.
     */
    public static boolean hasPermission(String permission) {
        return SecurityContext.hasPermission(permission);
    }

    /**
     * Validate that current user can access a specific batch.
     * Users can only access batches from their own bank.
     */
    public static boolean canAccessBatch(String batchBankId) {
        return canAccessBank(batchBankId);
    }
}

