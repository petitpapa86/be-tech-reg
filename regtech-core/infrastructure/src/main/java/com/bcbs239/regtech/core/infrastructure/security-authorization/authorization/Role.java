package com.bcbs239.regtech.core.security.authorization;

import java.util.Set;

/**
 * Standard roles with their associated permissions.
 * Roles are defined centrally but can be extended by modules.
 */
public enum Role {
    
    // Basic user roles
    USER("User", Set.of(
        Permission.USER_READ,
        Permission.BILLING_READ,
        Permission.BILLING_VIEW_INVOICES,
        Permission.REPORT_VIEW
    )),
    
    // Premium user with payment capabilities
    PREMIUM_USER("Premium User", Set.of(
        Permission.USER_READ,
        Permission.BILLING_READ,
        Permission.BILLING_PROCESS_PAYMENT,
        Permission.BILLING_MANAGE_SUBSCRIPTIONS,
        Permission.BILLING_VIEW_INVOICES,
        Permission.REPORT_VIEW,
        Permission.REPORT_EXPORT
    )),
    
    // Billing administrator
    BILLING_ADMIN("Billing Administrator", Set.of(
        Permission.USER_READ,
        Permission.BILLING_READ,
        Permission.BILLING_PROCESS_PAYMENT,
        Permission.BILLING_MANAGE_SUBSCRIPTIONS,
        Permission.BILLING_VIEW_INVOICES,
        Permission.BILLING_ADMIN,
        Permission.BILLING_WEBHOOK_MANAGE,
        Permission.REPORT_VIEW,
        Permission.REPORT_CREATE
    )),
    
    // Compliance officer
    COMPLIANCE_OFFICER("Compliance Officer", Set.of(
        Permission.USER_READ,
        Permission.BILLING_READ,
        Permission.BILLING_VIEW_INVOICES,
        Permission.COMPLIANCE_VIEW,
        Permission.COMPLIANCE_MANAGE,
        Permission.COMPLIANCE_AUDIT,
        Permission.REPORT_VIEW,
        Permission.REPORT_CREATE,
        Permission.REPORT_EXPORT
    )),
    
    // RegTech BCBS239 Roles
    BCBS239_VIEWER("BCBS239 Viewer", Set.of(
        Permission.USER_READ,
        Permission.BCBS239_VIEW_REPORTS,
        Permission.BCBS239_VIEW_VIOLATIONS
    )),
    
    BCBS239_DATA_ANALYST("BCBS239 Data Analyst", Set.of(
        Permission.USER_READ,
        Permission.BCBS239_UPLOAD_FILES,
        Permission.BCBS239_DOWNLOAD_FILES,
        Permission.BCBS239_VIEW_REPORTS,
        Permission.BCBS239_GENERATE_REPORTS,
        Permission.BCBS239_VIEW_VIOLATIONS,
        Permission.BCBS239_VALIDATE_DATA
    )),
    
    BCBS239_RISK_MANAGER("BCBS239 Risk Manager", Set.of(
        Permission.USER_READ,
        Permission.BCBS239_UPLOAD_FILES,
        Permission.BCBS239_DOWNLOAD_FILES,
        Permission.BCBS239_DELETE_FILES,
        Permission.BCBS239_GENERATE_REPORTS,
        Permission.BCBS239_VIEW_REPORTS,
        Permission.BCBS239_EXPORT_REPORTS,
        Permission.BCBS239_SCHEDULE_REPORTS,
        Permission.BCBS239_MANAGE_VIOLATIONS,
        Permission.BCBS239_VIEW_VIOLATIONS,
        Permission.BCBS239_VALIDATE_DATA,
        Permission.BCBS239_APPROVE_DATA,
        Permission.BCBS239_REJECT_DATA,
        Permission.BCBS239_MANAGE_TEMPLATES
    )),
    
    BCBS239_COMPLIANCE_OFFICER("BCBS239 Compliance Officer", Set.of(
        Permission.USER_READ,
        Permission.COMPLIANCE_VIEW,
        Permission.COMPLIANCE_MANAGE,
        Permission.COMPLIANCE_AUDIT,
        Permission.BCBS239_UPLOAD_FILES,
        Permission.BCBS239_DOWNLOAD_FILES,
        Permission.BCBS239_DELETE_FILES,
        Permission.BCBS239_GENERATE_REPORTS,
        Permission.BCBS239_VIEW_REPORTS,
        Permission.BCBS239_EXPORT_REPORTS,
        Permission.BCBS239_SCHEDULE_REPORTS,
        Permission.BCBS239_CONFIGURE_PARAMETERS,
        Permission.BCBS239_MANAGE_TEMPLATES,
        Permission.BCBS239_CONFIGURE_WORKFLOWS,
        Permission.BCBS239_MANAGE_VIOLATIONS,
        Permission.BCBS239_APPROVE_VIOLATIONS,
        Permission.BCBS239_VIEW_VIOLATIONS,
        Permission.BCBS239_VALIDATE_DATA,
        Permission.BCBS239_APPROVE_DATA,
        Permission.BCBS239_REJECT_DATA,
        Permission.BCBS239_SUBMIT_REGULATORY_REPORTS,
        Permission.BCBS239_REVIEW_SUBMISSIONS,
        Permission.BCBS239_TRACK_SUBMISSIONS,
        Permission.BCBS239_VIEW_AUDIT_LOGS
    )),
    
    BCBS239_BANK_ADMIN("BCBS239 Bank Administrator", Set.of(
        Permission.USER_READ,
        Permission.USER_CREATE,
        Permission.USER_UPDATE,
        Permission.BCBS239_UPLOAD_FILES,
        Permission.BCBS239_DOWNLOAD_FILES,
        Permission.BCBS239_DELETE_FILES,
        Permission.BCBS239_GENERATE_REPORTS,
        Permission.BCBS239_VIEW_REPORTS,
        Permission.BCBS239_EXPORT_REPORTS,
        Permission.BCBS239_CONFIGURE_PARAMETERS,
        Permission.BCBS239_MANAGE_TEMPLATES,
        Permission.BCBS239_MANAGE_VIOLATIONS,
        Permission.BCBS239_VIEW_VIOLATIONS,
        Permission.BCBS239_ADMINISTER_USERS,
        Permission.BCBS239_ASSIGN_ROLES,
        Permission.BCBS239_MANAGE_BANK_CONFIG,
        Permission.BCBS239_VIEW_AUDIT_LOGS
    )),
    
    BCBS239_AUDITOR("BCBS239 Auditor", Set.of(
        Permission.USER_READ,
        Permission.COMPLIANCE_AUDIT,
        Permission.BCBS239_VIEW_REPORTS,
        Permission.BCBS239_EXPORT_REPORTS,
        Permission.BCBS239_VIEW_VIOLATIONS,
        Permission.BCBS239_VIEW_AUDIT_LOGS,
        Permission.BCBS239_TRACK_SUBMISSIONS,
        Permission.BCBS239_MONITOR_SYSTEM
    )),
    
    BCBS239_HOLDING_COMPANY_USER("BCBS239 Holding Company User", Set.of(
        Permission.USER_READ,
        Permission.BCBS239_VIEW_REPORTS,
        Permission.BCBS239_GENERATE_REPORTS,
        Permission.BCBS239_EXPORT_REPORTS,
        Permission.BCBS239_VIEW_CROSS_BANK_DATA,
        Permission.BCBS239_CONSOLIDATE_REPORTS,
        Permission.BCBS239_VIEW_VIOLATIONS,
        Permission.BCBS239_TRACK_SUBMISSIONS
    )),
    
    // System administrator
    ADMIN("Administrator", Set.of(
        Permission.USER_CREATE,
        Permission.USER_READ,
        Permission.USER_UPDATE,
        Permission.USER_DELETE,
        Permission.USER_ADMIN,
        Permission.BILLING_READ,
        Permission.BILLING_PROCESS_PAYMENT,
        Permission.BILLING_MANAGE_SUBSCRIPTIONS,
        Permission.BILLING_VIEW_INVOICES,
        Permission.BILLING_ADMIN,
        Permission.BILLING_WEBHOOK_MANAGE,
        Permission.COMPLIANCE_VIEW,
        Permission.COMPLIANCE_MANAGE,
        Permission.COMPLIANCE_AUDIT,
        Permission.REPORT_VIEW,
        Permission.REPORT_CREATE,
        Permission.REPORT_EXPORT,
        Permission.REPORT_ADMIN,
        Permission.SYSTEM_ADMIN,
        Permission.SYSTEM_MONITOR,
        Permission.SYSTEM_CONFIG,
        // All BCBS239 permissions for system admin
        Permission.BCBS239_UPLOAD_FILES,
        Permission.BCBS239_DOWNLOAD_FILES,
        Permission.BCBS239_DELETE_FILES,
        Permission.BCBS239_GENERATE_REPORTS,
        Permission.BCBS239_VIEW_REPORTS,
        Permission.BCBS239_EXPORT_REPORTS,
        Permission.BCBS239_SCHEDULE_REPORTS,
        Permission.BCBS239_CONFIGURE_PARAMETERS,
        Permission.BCBS239_MANAGE_TEMPLATES,
        Permission.BCBS239_CONFIGURE_WORKFLOWS,
        Permission.BCBS239_MANAGE_VIOLATIONS,
        Permission.BCBS239_APPROVE_VIOLATIONS,
        Permission.BCBS239_VIEW_VIOLATIONS,
        Permission.BCBS239_ADMINISTER_USERS,
        Permission.BCBS239_ASSIGN_ROLES,
        Permission.BCBS239_VIEW_AUDIT_LOGS,
        Permission.BCBS239_VALIDATE_DATA,
        Permission.BCBS239_APPROVE_DATA,
        Permission.BCBS239_REJECT_DATA,
        Permission.BCBS239_MANAGE_SYSTEM_CONFIG,
        Permission.BCBS239_MONITOR_SYSTEM,
        Permission.BCBS239_BACKUP_RESTORE,
        Permission.BCBS239_SUBMIT_REGULATORY_REPORTS,
        Permission.BCBS239_REVIEW_SUBMISSIONS,
        Permission.BCBS239_TRACK_SUBMISSIONS,
        Permission.BCBS239_MANAGE_BANK_CONFIG,
        Permission.BCBS239_VIEW_CROSS_BANK_DATA,
        Permission.BCBS239_CONSOLIDATE_REPORTS
    ));
    
    private final String displayName;
    private final Set<String> permissions;
    
    Role(String displayName, Set<String> permissions) {
        this.displayName = displayName;
        this.permissions = permissions;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public Set<String> getPermissions() {
        return permissions;
    }
    
    public boolean hasPermission(String permission) {
        return permissions.contains(permission);
    }
}
