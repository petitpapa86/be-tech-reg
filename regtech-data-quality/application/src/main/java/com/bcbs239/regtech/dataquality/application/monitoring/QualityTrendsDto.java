package com.bcbs239.regtech.dataquality.application.monitoring;

import com.bcbs239.regtech.dataquality.domain.shared.BankId;

import java.time.Instant;
import java.util.List;

/**
 * DTO for quality trends analysis over time.
 * Contains aggregated statistics and trend information for a bank.
 */
public record QualityTrendsDto(
    String bankId,
    Instant startTime,
    Instant endTime,
    int totalReports,
    List<QualityReportSummaryDto> reports,
    double averageOverallScore,
    double averageCompletenessScore,
    double averageAccuracyScore,
    double averageConsistencyScore,
    double averageTimelinessScore,
    double averageUniquenessScore,
    double averageValidityScore,
    double complianceRate,
    String trendDirection
) {
    
    /**
     * Creates an empty trends DTO for when no reports are found.
     */
    public static QualityTrendsDto empty(BankId bankId, Instant startTime, Instant endTime) {
        return new QualityTrendsDto(
            bankId.value(),
            startTime,
            endTime,
            0,
            List.of(),
            0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
            0.0,
            "STABLE"
        );
    }
    
    /**
     * Builder for QualityTrendsDto.
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Gets the compliance rate as a percentage string.
     */
    public String getComplianceRatePercentage() {
        return String.format("%.1f%%", complianceRate * 100.0);
    }
    
    /**
     * Gets the trend direction as a user-friendly string.
     */
    public String getTrendDescription() {
        return switch (trendDirection) {
            case "IMPROVING" -> "Quality scores are improving over time";
            case "DECLINING" -> "Quality scores are declining over time";
            case "STABLE" -> "Quality scores are stable over time";
            default -> "Trend direction unknown";
        };
    }
    
    /**
     * Checks if the trend is positive.
     */
    public boolean isImprovingTrend() {
        return "IMPROVING".equals(trendDirection);
    }
    
    /**
     * Checks if the trend is negative.
     */
    public boolean isDecliningTrend() {
        return "DECLINING".equals(trendDirection);
    }
    
    /**
     * Gets the time period duration in days.
     */
    public long getPeriodDays() {
        if (startTime == null || endTime == null) {
            return 0;
        }
        return (endTime.toEpochMilli() - startTime.toEpochMilli()) / (24 * 60 * 60 * 1000);
    }
    
    /**
     * Gets the average reports per day.
     */
    public double getAverageReportsPerDay() {
        long days = getPeriodDays();
        return days > 0 ? (double) totalReports / days : 0.0;
    }
    
    public static class Builder {
        private BankId bankId;
        private Instant startTime;
        private Instant endTime;
        private int totalReports;
        private List<QualityReportSummaryDto> reports = List.of();
        private double averageOverallScore;
        private double averageCompletenessScore;
        private double averageAccuracyScore;
        private double averageConsistencyScore;
        private double averageTimelinessScore;
        private double averageUniquenessScore;
        private double averageValidityScore;
        private double complianceRate;
        private String trendDirection = "STABLE";
        
        public Builder bankId(BankId bankId) {
            this.bankId = bankId;
            return this;
        }
        
        public Builder startTime(Instant startTime) {
            this.startTime = startTime;
            return this;
        }
        
        public Builder endTime(Instant endTime) {
            this.endTime = endTime;
            return this;
        }
        
        public Builder totalReports(int totalReports) {
            this.totalReports = totalReports;
            return this;
        }
        
        public Builder reports(List<QualityReportSummaryDto> reports) {
            this.reports = reports != null ? reports : List.of();
            return this;
        }
        
        public Builder averageOverallScore(double averageOverallScore) {
            this.averageOverallScore = averageOverallScore;
            return this;
        }
        
        public Builder averageCompletenessScore(double averageCompletenessScore) {
            this.averageCompletenessScore = averageCompletenessScore;
            return this;
        }
        
        public Builder averageAccuracyScore(double averageAccuracyScore) {
            this.averageAccuracyScore = averageAccuracyScore;
            return this;
        }
        
        public Builder averageConsistencyScore(double averageConsistencyScore) {
            this.averageConsistencyScore = averageConsistencyScore;
            return this;
        }
        
        public Builder averageTimelinessScore(double averageTimelinessScore) {
            this.averageTimelinessScore = averageTimelinessScore;
            return this;
        }
        
        public Builder averageUniquenessScore(double averageUniquenessScore) {
            this.averageUniquenessScore = averageUniquenessScore;
            return this;
        }
        
        public Builder averageValidityScore(double averageValidityScore) {
            this.averageValidityScore = averageValidityScore;
            return this;
        }
        
        public Builder complianceRate(double complianceRate) {
            this.complianceRate = complianceRate;
            return this;
        }
        
        public Builder trendDirection(String trendDirection) {
            this.trendDirection = trendDirection;
            return this;
        }
        
        public QualityTrendsDto build() {
            return new QualityTrendsDto(
                bankId.value(),
                startTime,
                endTime,
                totalReports,
                reports,
                averageOverallScore,
                averageCompletenessScore,
                averageAccuracyScore,
                averageConsistencyScore,
                averageTimelinessScore,
                averageUniquenessScore,
                averageValidityScore,
                complianceRate,
                trendDirection
            );
        }
    }
}