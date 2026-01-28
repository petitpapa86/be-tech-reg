package com.bcbs239.regtech.metrics.domain;

import com.bcbs239.regtech.core.domain.shared.valueobjects.QualityReportId;
import lombok.Getter;

/**
 * Domain model representing file-level metrics (lightweight DTO for domain layer).
 */
@Getter
public class ComplianceFile {
    private final Long id;
    private final String filename;
    private final String date;
    private final Double score;
    private final Double completenessScore;
    private final String status;
    private final String batchId;
    private final BankId bankId;
    private final QualityReportId reportId;


    public ComplianceFile(Long id, String filename, String date, Double score, Double completenessScore, String status, String batchId, BankId bankId, QualityReportId reportId) {
        this.id = id;
        this.filename = filename;
        this.date = date;
        this.score = score;
        this.completenessScore = completenessScore;
        this.status = status;
        this.batchId = batchId;
        this.bankId = bankId;
        this.reportId = reportId;
    }

    /**
     * Domain helper: whether this file is considered compliant.
     */
    public boolean isCompliant() {
        return "COMPLIANT".equalsIgnoreCase(status);
    }

}
