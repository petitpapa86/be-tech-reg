package com.bcbs239.regtech.core.domain.security.authorization;

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
    
    // RegTech BCBS239 Specific Permissions
    public static final String BCBS239_UPLOAD_FILES = "bcbs239:upload-files";
    public static final String BCBS239_DOWNLOAD_FILES = "bcbs239:download-files";
    public static final String BCBS239_DELETE_FILES = "bcbs239:delete-files";
    public static final String BCBS239_GENERATE_REPORTS = "bcbs239:generate-reports";
    public static final String BCBS239_VIEW_REPORTS = "bcbs239:view-reports";
    public static final String BCBS239_EXPORT_REPORTS = "bcbs239:export-reports";
    public static final String BCBS239_SCHEDULE_REPORTS = "bcbs239:schedule-reports";
    public static final String BCBS239_CONFIGURE_PARAMETERS = "bcbs239:configure-parameters";
    public static final String BCBS239_MANAGE_TEMPLATES = "bcbs239:manage-templates";
    public static final String BCBS239_CONFIGURE_WORKFLOWS = "bcbs239:configure-workflows";
    public static final String BCBS239_MANAGE_VIOLATIONS = "bcbs239:manage-violations";
    public static final String BCBS239_APPROVE_VIOLATIONS = "bcbs239:approve-violations";
    public static final String BCBS239_VIEW_VIOLATIONS = "bcbs239:view-violations";
    public static final String BCBS239_ADMINISTER_USERS = "bcbs239:administer-users";
    public static final String BCBS239_ASSIGN_ROLES = "bcbs239:assign-roles";
    public static final String BCBS239_VIEW_AUDIT_LOGS = "bcbs239:view-audit-logs";
    public static final String BCBS239_VALIDATE_DATA = "bcbs239:validate-data";
    public static final String BCBS239_APPROVE_DATA = "bcbs239:approve-data";
    public static final String BCBS239_REJECT_DATA = "bcbs239:reject-data";
    public static final String BCBS239_MANAGE_SYSTEM_CONFIG = "bcbs239:manage-system-config";
    public static final String BCBS239_MONITOR_SYSTEM = "bcbs239:monitor-system";
    public static final String BCBS239_BACKUP_RESTORE = "bcbs239:backup-restore";
    public static final String BCBS239_SUBMIT_REGULATORY_REPORTS = "bcbs239:submit-regulatory-reports";
    public static final String BCBS239_REVIEW_SUBMISSIONS = "bcbs239:review-submissions";
    public static final String BCBS239_TRACK_SUBMISSIONS = "bcbs239:track-submissions";
    public static final String BCBS239_MANAGE_BANK_CONFIG = "bcbs239:manage-bank-config";
    public static final String BCBS239_VIEW_CROSS_BANK_DATA = "bcbs239:view-cross-bank-data";
    public static final String BCBS239_CONSOLIDATE_REPORTS = "bcbs239:consolidate-reports";
    
    // System Administration
    public static final String SYSTEM_ADMIN = "system:admin";
    public static final String SYSTEM_MONITOR = "system:monitor";
    public static final String SYSTEM_CONFIG = "system:config";
    
    private Permission() {
        // Utility class
    }
}

