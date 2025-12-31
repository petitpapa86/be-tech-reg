package com.bcbs239.regtech.metrics.presentation.dashboard.dto;

import java.util.List;

public class DashboardResponse {
    public final Summary summary;
    public final List<FileItem> files;
    public final ComplianceState compliance;
    public final List<ReportItem> reports;
    public final Integer lastBatchViolations;

    public DashboardResponse(Summary summary, List<FileItem> files, ComplianceState compliance, List<ReportItem> reports, Integer lastBatchViolations) {
        this.summary = summary;
        this.files = files;
        this.compliance = compliance;
        this.reports = reports;
        this.lastBatchViolations = lastBatchViolations;
    }

    public static class Summary {
        public final Integer filesProcessed;
        public final Double avgScore;
        public final Integer violations;
        public final Integer reports;

        public Summary(Integer filesProcessed, Double avgScore, Integer violations, Integer reports) {
            this.filesProcessed = filesProcessed;
            this.avgScore = avgScore;
            this.violations = violations;
            this.reports = reports;
        }
    }
}
