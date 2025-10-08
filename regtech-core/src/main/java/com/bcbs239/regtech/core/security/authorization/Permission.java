package com.bcbs239.regtech.core.security.authorization;

/**
 * Standard permissions across all modules.
 * Each module can define its own permissions following this pattern.
 */
public final class Permission {
    
    // User Management Permissions (IAM Module)
    public static final String USER_CREATE = "user:create";
    public static final String USER_READ = "user:read";
    public static final String USER_UPDATE = "user:update";
    public static final String USER_DELETE = "user:delete";
    public static final String USER_ADMIN = "user:admin";
    
    // Billing Permissions (Billing Module)
    public static final String BILLING_READ = "billing:read";
    public static final String BILLING_PROCESS_PAYMENT = "billing:process-payment";
    public static final String BILLING_MANAGE_SUBSCRIPTIONS = "billing:manage-subscriptions";
    public static final String BILLING_VIEW_INVOICES = "billing:view-invoices";
    public static final String BILLING_ADMIN = "billing:admin";
    public static final String BILLING_WEBHOOK_MANAGE = "billing:webhook-manage";
    
    // Reporting Permissions (Future Reporting Module)
    public static final String REPORT_VIEW = "report:view";
    public static final String REPORT_CREATE = "report:create";
    public static final String REPORT_EXPORT = "report:export";
    public static final String REPORT_ADMIN = "report:admin";
    
    // Compliance Permissions (Future Compliance Module)
    public static final String COMPLIANCE_VIEW = "compliance:view";
    public static final String COMPLIANCE_MANAGE = "compliance:manage";
    public static final String COMPLIANCE_AUDIT = "compliance:audit";
    
    // System Administration
    public static final String SYSTEM_ADMIN = "system:admin";
    public static final String SYSTEM_MONITOR = "system:monitor";
    public static final String SYSTEM_CONFIG = "system:config";
    
    private Permission() {
        // Utility class
    }
}