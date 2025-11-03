package com.bcbs239.regtech.iam.domain.users;

import com.bcbs239.regtech.core.security.authorization.Permission;
import java.util.Set;

/**
 * Enhanced BCBS 239 Roles with hierarchical permissions.
 * Each role has a level and specific permissions for RegTech compliance operations.
 * Maps to core Permission constants for cross-module compatibility.
 */
public enum Bcbs239Role {
    
    /**
     * Basic viewer - can only view reports and data
     */
    VIEWER(1, Set.of(
        Permission.BCBS239_VIEW_REPORTS,
        Permission.BCBS239_VIEW_VIOLATIONS
    )),
    
    /**
     * Data analyst - can upload files and view reports
     */
    DATA_ANALYST(2, Set.of(
        Permission.BCBS239_UPLOAD_FILES,
        Permission.BCBS239_DOWNLOAD_FILES,
        Permission.BCBS239_VIEW_REPORTS,
        Permission.BCBS239_GENERATE_REPORTS,
        Permission.BCBS239_VIEW_VIOLATIONS,
        Permission.BCBS239_VALIDATE_DATA
    )),
    
    /**
     * Risk manager - can manage violations and generate reports
     */
    RISK_MANAGER(3, Set.of(
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
    
    /**
     * Compliance officer - full compliance management capabilities
     */
    COMPLIANCE_OFFICER(4, Set.of(
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
    
    /**
     * System administrator - full system access
     */
    SYSTEM_ADMIN(5, Set.of(
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
    )),
    
    /**
     * Bank administrator - manages bank-specific configurations
     */
    BANK_ADMIN(4, Set.of(
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
    
    /**
     * Auditor - read-only access with audit capabilities
     */
    AUDITOR(3, Set.of(
        Permission.BCBS239_VIEW_REPORTS,
        Permission.BCBS239_EXPORT_REPORTS,
        Permission.BCBS239_VIEW_VIOLATIONS,
        Permission.BCBS239_VIEW_AUDIT_LOGS,
        Permission.BCBS239_TRACK_SUBMISSIONS,
        Permission.BCBS239_MONITOR_SYSTEM
    )),
    
    /**
     * Holding company user - can view across multiple banks
     */
    HOLDING_COMPANY_USER(4, Set.of(
        Permission.BCBS239_VIEW_REPORTS,
        Permission.BCBS239_GENERATE_REPORTS,
        Permission.BCBS239_EXPORT_REPORTS,
        Permission.BCBS239_VIEW_CROSS_BANK_DATA,
        Permission.BCBS239_CONSOLIDATE_REPORTS,
        Permission.BCBS239_VIEW_VIOLATIONS,
        Permission.BCBS239_TRACK_SUBMISSIONS
    ));
    
    private final int level;
    private final Set<String> permissions;
    
    Bcbs239Role(int level, Set<String> permissions) {
        this.level = level;
        this.permissions = Set.copyOf(permissions);
    }
    
    public int getLevel() {
        return level;
    }
    
    public Set<String> getPermissions() {
        return Set.copyOf(permissions);
    }
    
    public boolean hasPermission(String permission) {
        return permissions.contains(permission);
    }
    
    /**
     * Check if this role has higher or equal level than another role
     */
    public boolean hasLevelOrHigher(Bcbs239Role otherRole) {
        return this.level >= otherRole.level;
    }
    
    /**
     * Get all permissions as strings for authorization service
     */
    public Set<String> getPermissionStrings() {
        return permissions;
    }
    
    /**
     * Check if role can perform a specific operation
     */
    public boolean canPerform(String operation) {
        return permissions.contains(operation);
    }
}