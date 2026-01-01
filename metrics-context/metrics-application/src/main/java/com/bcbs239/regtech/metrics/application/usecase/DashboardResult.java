package com.bcbs239.regtech.metrics.application.usecase;

import com.bcbs239.regtech.metrics.domain.model.ComplianceFile;
import java.util.List;

public class DashboardResult {
    public final Summary summary;
    public final List<ComplianceFile> files;
    public final Integer lastBatchViolations;

    public DashboardResult(Summary summary, List<ComplianceFile> files, Integer lastBatchViolations) {
        this.summary = summary;
        this.files = files;
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
