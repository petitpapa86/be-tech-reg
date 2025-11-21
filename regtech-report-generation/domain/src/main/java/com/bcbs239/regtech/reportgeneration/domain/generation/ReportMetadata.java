package com.bcbs239.regtech.reportgeneration.domain.generation;

import com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects.BankId;
import com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects.BatchId;
import com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects.ReportingDate;
import lombok.NonNull;

import java.time.Instant;

/**
 * Report Metadata value object
 * 
 * Contains metadata required for report generation including identifiers
 * and contextual information.
 */
public record ReportMetadata(
    @NonNull BatchId batchId,
    @NonNull BankId bankId,
    @NonNull String bankName,
    @NonNull ReportingDate reportingDate,
    @NonNull Instant generatedAt
) {
    
    /**
     * Compact constructor with validation
     */
    public ReportMetadata {
        if (batchId == null) {
            throw new IllegalArgumentException("Batch ID cannot be null");
        }
        if (bankId == null) {
            throw new IllegalArgumentException("Bank ID cannot be null");
        }
        if (bankName == null || bankName.isBlank()) {
            throw new IllegalArgumentException("Bank name cannot be null or blank");
        }
        if (reportingDate == null) {
            throw new IllegalArgumentException("Reporting date cannot be null");
        }
        if (generatedAt == null) {
            throw new IllegalArgumentException("Generated at timestamp cannot be null");
        }
    }
    
    /**
     * Create report metadata from raw values with current timestamp
     */
    public static ReportMetadata create(String batchId, String bankId, String bankName, String reportingDate) {
        return new ReportMetadata(
            new BatchId(batchId),
            new BankId(bankId),
            bankName,
            ReportingDate.fromString(reportingDate),
            Instant.now()
        );
    }
    
    /**
     * Create report metadata with explicit timestamp
     */
    public static ReportMetadata create(String batchId, String bankId, String bankName, 
                                       String reportingDate, Instant generatedAt) {
        return new ReportMetadata(
            new BatchId(batchId),
            new BankId(bankId),
            bankName,
            ReportingDate.fromString(reportingDate),
            generatedAt
        );
    }
}
