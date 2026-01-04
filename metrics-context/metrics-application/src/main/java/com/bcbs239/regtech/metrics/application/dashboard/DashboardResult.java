package com.bcbs239.regtech.metrics.application.dashboard;

import com.bcbs239.regtech.metrics.domain.ComplianceFile;
import com.bcbs239.regtech.metrics.domain.ComplianceReport;

import java.util.List;

public class DashboardResult {
    public final Summary summary;
    public final Compliance compliance;
    public final List<ComplianceFile> files;
    public final List<ComplianceReport> reports;
    public final Integer lastBatchViolations;

    public DashboardResult(Summary summary, List<ComplianceFile> files, List<ComplianceReport> reports, Integer lastBatchViolations) {
        this.summary = summary;
        this.compliance = null;
        this.files = files;
        this.reports = reports;
        this.lastBatchViolations = lastBatchViolations;
    }

    public DashboardResult(Summary summary, Compliance compliance, List<ComplianceFile> files, List<ComplianceReport> reports, Integer lastBatchViolations) {
        this.summary = summary;
        this.compliance = compliance;
        this.files = files;
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

    public static class Compliance {
        public final Double overall;
        public final Double dataQuality;
        public final Double bcbs;
        public final Double completeness;

        public Compliance(Double overall, Double dataQuality, Double bcbs, Double completeness) {
            this.overall = overall;
            this.dataQuality = dataQuality;
            this.bcbs = bcbs;
            this.completeness = completeness;
        }
    }
}
