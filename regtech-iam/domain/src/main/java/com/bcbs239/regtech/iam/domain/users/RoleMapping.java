package com.bcbs239.regtech.iam.domain.users;


import com.bcbs239.regtech.core.domain.security.authorization.Role;

/**
 * Maps business-specific BCBS239 roles to core system roles.
 * This provides a clean separation between business domain and technical authorization.
 */
public class RoleMapping {
    
    /**
     * Convert BCBS239 business role to core system role
     */
    public static Role toCoreRole(Bcbs239Role bcbs239Role) {
        return switch (bcbs239Role) {
            case VIEWER -> Role.BCBS239_VIEWER;
            case DATA_ANALYST -> Role.BCBS239_DATA_ANALYST;
            case RISK_MANAGER -> Role.BCBS239_RISK_MANAGER;
            case COMPLIANCE_OFFICER -> Role.BCBS239_COMPLIANCE_OFFICER;
            case BANK_ADMIN -> Role.BCBS239_BANK_ADMIN;
            case AUDITOR -> Role.BCBS239_AUDITOR;
            case HOLDING_COMPANY_USER -> Role.BCBS239_HOLDING_COMPANY_USER;
            case SYSTEM_ADMIN -> Role.ADMIN;
        };
    }
    
    /**
     * Convert core system role to BCBS239 business role (if applicable)
     */
    public static Bcbs239Role fromCoreRole(Role coreRole) {
        return switch (coreRole) {
            case BCBS239_VIEWER -> Bcbs239Role.VIEWER;
            case BCBS239_DATA_ANALYST -> Bcbs239Role.DATA_ANALYST;
            case BCBS239_RISK_MANAGER -> Bcbs239Role.RISK_MANAGER;
            case BCBS239_COMPLIANCE_OFFICER -> Bcbs239Role.COMPLIANCE_OFFICER;
            case BCBS239_BANK_ADMIN -> Bcbs239Role.BANK_ADMIN;
            case BCBS239_AUDITOR -> Bcbs239Role.AUDITOR;
            case BCBS239_HOLDING_COMPANY_USER -> Bcbs239Role.HOLDING_COMPANY_USER;
            case ADMIN -> Bcbs239Role.SYSTEM_ADMIN;
            default -> throw new IllegalArgumentException("Core role " + coreRole + " has no BCBS239 equivalent");
        };
    }
    
    /**
     * Check if a core role is a BCBS239 role
     */
    public static boolean isBcbs239Role(Role coreRole) {
        return switch (coreRole) {
            case BCBS239_VIEWER, BCBS239_DATA_ANALYST, BCBS239_RISK_MANAGER, 
                 BCBS239_COMPLIANCE_OFFICER, BCBS239_BANK_ADMIN, BCBS239_AUDITOR, 
                 BCBS239_HOLDING_COMPANY_USER -> true;
            case ADMIN -> true; // System admin can act as BCBS239 admin
            default -> false;
        };
    }
}


