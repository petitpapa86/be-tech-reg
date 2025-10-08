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
        Permission.SYSTEM_CONFIG
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