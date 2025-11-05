package com.bcbs239.regtech.ingestion.presentation.compliance.reports;

import java.time.Instant;

/**
 * Summary data structure for compliance status.
 */
public record ComplianceStatusSummary(
        boolean isCompliant,
        double complianceScore,
        long totalFilesUnderRetention,
        int totalViolations,
        int filesApproachingExpiry,
        int filesEligibleForDeletion,
        Instant lastReportGenerated,
        String summary
) {
    public static ComplianceStatusSummaryBuilder builder() {
        return new ComplianceStatusSummaryBuilder();
    }
    
    public static class ComplianceStatusSummaryBuilder {
        private boolean isCompliant;
        private double complianceScore;
        private long totalFilesUnderRetention;
        private int totalViolations;
        private int filesApproachingExpiry;
        private int filesEligibleForDeletion;
        private Instant lastReportGenerated;
        private String summary;
        
        public ComplianceStatusSummaryBuilder isCompliant(boolean isCompliant) {
            this.isCompliant = isCompliant;
            return this;
        }
        
        public ComplianceStatusSummaryBuilder complianceScore(double complianceScore) {
            this.complianceScore = complianceScore;
            return this;
        }
        
        public ComplianceStatusSummaryBuilder totalFilesUnderRetention(long totalFilesUnderRetention) {
            this.totalFilesUnderRetention = totalFilesUnderRetention;
            return this;
        }
        
        public ComplianceStatusSummaryBuilder totalViolations(int totalViolations) {
            this.totalViolations = totalViolations;
            return this;
        }
        
        public ComplianceStatusSummaryBuilder filesApproachingExpiry(int filesApproachingExpiry) {
            this.filesApproachingExpiry = filesApproachingExpiry;
            return this;
        }
        
        public ComplianceStatusSummaryBuilder filesEligibleForDeletion(int filesEligibleForDeletion) {
            this.filesEligibleForDeletion = filesEligibleForDeletion;
            return this;
        }
        
        public ComplianceStatusSummaryBuilder lastReportGenerated(Instant lastReportGenerated) {
            this.lastReportGenerated = lastReportGenerated;
            return this;
        }
        
        public ComplianceStatusSummaryBuilder summary(String summary) {
            this.summary = summary;
            return this;
        }
        
        public ComplianceStatusSummary build() {
            return new ComplianceStatusSummary(
                    isCompliant,
                    complianceScore,
                    totalFilesUnderRetention,
                    totalViolations,
                    filesApproachingExpiry,
                    filesEligibleForDeletion,
                    lastReportGenerated,
                    summary
            );
        }
    }
}

