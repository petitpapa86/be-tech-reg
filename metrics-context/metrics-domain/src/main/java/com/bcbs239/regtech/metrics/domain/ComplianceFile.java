package com.bcbs239.regtech.metrics.domain;

import lombok.Getter;

/**
 * Domain model representing file-level metrics (lightweight DTO for domain layer).
 */
@Getter
public class ComplianceFile {
    private final String filename;
    private final String date;
    private final Double score;
    private final Double completenessScore;
    private final String status;
    private final String batchId;
    private final BankId bankId;

    public ComplianceFile(String filename, String date, Double score, Double completenessScore, String status) {
        this(filename, date, score, completenessScore, status, null, null);
    }

    public ComplianceFile(String filename, String date, Double score, Double completenessScore, String status, String batchId, BankId bankId) {
        this.filename = filename;
        this.date = date;
        this.score = score;
        this.completenessScore = completenessScore;
        this.status = status;
        this.batchId = batchId;
        this.bankId = bankId;
    }

    /**
     * Domain helper: whether this file is considered compliant.
     */
    public boolean isCompliant() {
        return "COMPLIANT".equalsIgnoreCase(status);
    }

}
