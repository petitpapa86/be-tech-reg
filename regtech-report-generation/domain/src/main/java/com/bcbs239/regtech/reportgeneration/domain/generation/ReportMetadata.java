package com.bcbs239.regtech.reportgeneration.domain.generation;

import com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects.BankId;
import com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects.BatchId;
import com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects.ReportingDate;
import lombok.NonNull;

/**
 * Report Metadata value object
 * 
 * Contains metadata required for report generation including identifiers
 * and contextual information.
 */
public record ReportMetadata(
    @NonNull BatchId batchId,
    @NonNull BankId bankId,
    @NonNull ReportingDate reportingDate
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
        if (reportingDate == null) {
            throw new IllegalArgumentException("Reporting date cannot be null");
        }
    }
    
    /**
     * Create report metadata from raw values
     */
    public static ReportMetadata create(String batchId, String bankId, String reportingDate) {
        return new ReportMetadata(
            new BatchId(batchId),
            new BankId(bankId),
            ReportingDate.fromString(reportingDate)
        );
    }
}
