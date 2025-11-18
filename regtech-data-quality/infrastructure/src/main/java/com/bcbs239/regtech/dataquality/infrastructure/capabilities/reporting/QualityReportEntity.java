package com.bcbs239.regtech.dataquality.infrastructure.deprecated.reporting;

import com.bcbs239.regtech.dataquality.domain.report.QualityStatus;

/**
 * Placeholder for deprecated QualityReportEntity.
 * Kept as a minimal POJO to avoid compile-time dependency issues during migration.
 */
@Deprecated
public class QualityReportEntity {
    private String reportId;
    private String batchId;
    private String bankId;
    private QualityStatus status;

    public String getReportId() { return reportId; }
    public void setReportId(String reportId) { this.reportId = reportId; }
    public String getBatchId() { return batchId; }
    public void setBatchId(String batchId) { this.batchId = batchId; }
    public String getBankId() { return bankId; }
    public void setBankId(String bankId) { this.bankId = bankId; }
    public QualityStatus getStatus() { return status; }
    public void setStatus(QualityStatus status) { this.status = status; }
}
