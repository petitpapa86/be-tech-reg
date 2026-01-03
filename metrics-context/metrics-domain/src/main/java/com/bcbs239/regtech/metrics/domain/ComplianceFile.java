package com.bcbs239.regtech.metrics.domain;

/**
 * Domain model representing file-level metrics (lightweight DTO for domain layer).
 */
public class ComplianceFile {
    private final String filename;
    private final String date;
    private final Double score;
    private final String status;
    private final String batchId;
    private final BankId bankId;

    public ComplianceFile(String filename, String date, Double score, String status) {
        this(filename, date, score, status, null, null);
    }

    public ComplianceFile(String filename, String date, Double score, String status, String batchId, BankId bankId) {
        this.filename = filename;
        this.date = date;
        this.score = score;
        this.status = status;
        this.batchId = batchId;
        this.bankId = bankId;
    }

    public String getFilename() {
        return filename;
    }

    public String getDate() {
        return date;
    }

    public Double getScore() {
        return score;
    }

    public String getStatus() {
        return status;
    }

    /**
     * Domain helper: whether this file is considered compliant.
     */
    public boolean isCompliant() {
        return status != null && "COMPLIANT".equalsIgnoreCase(status);
    }

    public String getBatchId() {
        return batchId;
    }

    public BankId getBankId() {
        return bankId;
    }
}
