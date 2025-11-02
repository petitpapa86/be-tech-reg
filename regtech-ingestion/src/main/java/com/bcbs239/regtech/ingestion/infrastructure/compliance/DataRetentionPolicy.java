package com.bcbs239.regtech.ingestion.infrastructure.compliance;

import lombok.Builder;
import lombok.Value;

import java.time.Duration;

/**
 * Represents a data retention policy with regulatory compliance requirements.
 */
@Value
@Builder
public class DataRetentionPolicy {
    
    /**
     * Unique identifier for the retention policy.
     */
    String policyId;
    
    /**
     * Human-readable name for the policy.
     */
    String policyName;
    
    /**
     * Description of the regulatory requirement this policy addresses.
     */
    String regulatoryRequirement;
    
    /**
     * Duration to keep data in standard storage before transitioning.
     */
    Duration standardStorageDuration;
    
    /**
     * Duration to keep data in archive storage (Glacier) before deletion.
     */
    Duration archiveStorageDuration;
    
    /**
     * Total retention period before data can be permanently deleted.
     */
    Duration totalRetentionPeriod;
    
    /**
     * Whether this policy allows early deletion under certain circumstances.
     */
    boolean allowsEarlyDeletion;
    
    /**
     * Legal hold requirements that prevent deletion.
     */
    boolean hasLegalHoldRequirements;
    
    /**
     * Audit trail requirements for this policy.
     */
    AuditRequirements auditRequirements;
    
    @Value
    @Builder
    public static class AuditRequirements {
        /**
         * Whether access to data must be logged.
         */
        boolean logDataAccess;
        
        /**
         * Whether retention policy changes must be logged.
         */
        boolean logPolicyChanges;
        
        /**
         * Whether data deletion must be logged.
         */
        boolean logDataDeletion;
        
        /**
         * Duration to retain audit logs.
         */
        Duration auditLogRetentionPeriod;
    }
    
    /**
     * Creates a default regulatory compliance policy for financial data.
     */
    public static DataRetentionPolicy createDefaultFinancialDataPolicy() {
        return DataRetentionPolicy.builder()
                .policyId("FINANCIAL_DATA_DEFAULT")
                .policyName("Default Financial Data Retention Policy")
                .regulatoryRequirement("Basel III, BCBS 239 - Risk Data Aggregation")
                .standardStorageDuration(Duration.ofDays(180)) // 6 months in standard storage
                .archiveStorageDuration(Duration.ofDays(1825)) // 5 years in archive storage
                .totalRetentionPeriod(Duration.ofDays(2555)) // 7 years total retention
                .allowsEarlyDeletion(false)
                .hasLegalHoldRequirements(true)
                .auditRequirements(AuditRequirements.builder()
                        .logDataAccess(true)
                        .logPolicyChanges(true)
                        .logDataDeletion(true)
                        .auditLogRetentionPeriod(Duration.ofDays(3650)) // 10 years for audit logs
                        .build())
                .build();
    }
    
    /**
     * Creates a policy for temporary processing data with shorter retention.
     */
    public static DataRetentionPolicy createTemporaryProcessingDataPolicy() {
        return DataRetentionPolicy.builder()
                .policyId("TEMP_PROCESSING_DATA")
                .policyName("Temporary Processing Data Retention Policy")
                .regulatoryRequirement("Internal Data Management - Temporary Processing")
                .standardStorageDuration(Duration.ofDays(30)) // 1 month in standard storage
                .archiveStorageDuration(Duration.ofDays(90)) // 3 months in archive storage
                .totalRetentionPeriod(Duration.ofDays(365)) // 1 year total retention
                .allowsEarlyDeletion(true)
                .hasLegalHoldRequirements(false)
                .auditRequirements(AuditRequirements.builder()
                        .logDataAccess(false)
                        .logPolicyChanges(true)
                        .logDataDeletion(true)
                        .auditLogRetentionPeriod(Duration.ofDays(1095)) // 3 years for audit logs
                        .build())
                .build();
    }
    
    /**
     * Validates that the policy meets minimum regulatory requirements.
     */
    public boolean isCompliantWithRegulations() {
        // Financial data must be retained for at least 7 years per Basel III
        if (policyId.contains("FINANCIAL") && totalRetentionPeriod.toDays() < 2555) {
            return false;
        }
        
        // Audit logs must be retained longer than the data itself
        if (auditRequirements.auditLogRetentionPeriod.compareTo(totalRetentionPeriod) < 0) {
            return false;
        }
        
        // Archive storage duration should be reasonable
        if (archiveStorageDuration.toDays() < 90) {
            return false;
        }
        
        return true;
    }
}