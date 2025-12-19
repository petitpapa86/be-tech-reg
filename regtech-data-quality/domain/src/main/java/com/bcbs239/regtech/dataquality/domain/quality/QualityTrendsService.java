package com.bcbs239.regtech.dataquality.domain.quality;

import com.bcbs239.regtech.dataquality.domain.report.QualityReport;
import com.bcbs239.regtech.dataquality.domain.shared.BankId;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Domain service for calculating quality trends and analytics.
 * Contains business logic for trend analysis and statistical calculations.
 */
@Component
public class QualityTrendsService {

    /**
     * Calculates quality trends for a bank over a time period.
     */
    public QualityTrendsData calculateTrends(
        BankId bankId,
        Instant startTime,
        Instant endTime,
        List<QualityReport> reports
    ) {
        if (reports.isEmpty()) {
            return QualityTrendsData.empty(bankId, startTime, endTime);
        }

        // Calculate average scores
        double avgOverallScore = reports.stream()
            .filter(r -> r.getScores() != null)
            .mapToDouble(r -> r.getScores().overallScore())
            .average()
            .orElse(0.0);
        
        double avgCompletenessScore = reports.stream()
            .filter(r -> r.getScores() != null)
            .mapToDouble(r -> r.getScores().completenessScore())
            .average()
            .orElse(0.0);
        
        double avgAccuracyScore = reports.stream()
            .filter(r -> r.getScores() != null)
            .mapToDouble(r -> r.getScores().accuracyScore())
            .average()
            .orElse(0.0);
        
        double avgConsistencyScore = reports.stream()
            .filter(r -> r.getScores() != null)
            .mapToDouble(r -> r.getScores().consistencyScore())
            .average()
            .orElse(0.0);
        
        double avgTimelinessScore = reports.stream()
            .filter(r -> r.getScores() != null)
            .mapToDouble(r -> r.getScores().timelinessScore())
            .average()
            .orElse(0.0);
        
        double avgUniquenessScore = reports.stream()
            .filter(r -> r.getScores() != null)
            .mapToDouble(r -> r.getScores().uniquenessScore())
            .average()
            .orElse(0.0);
        
        double avgValidityScore = reports.stream()
            .filter(r -> r.getScores() != null)
            .mapToDouble(r -> r.getScores().validityScore())
            .average()
            .orElse(0.0);
        
        // Calculate compliance rate
        long compliantCount = reports.stream()
            .filter(r -> r.getScores() != null && r.getScores().isCompliant()).count();
        double complianceRate = reports.isEmpty() ? 0.0 : (double) compliantCount / reports.size();

        // Calculate trend direction
        String trendDirection = calculateTrendDirection(reports);

        return new QualityTrendsData(
            bankId,
            startTime,
            endTime,
            reports.size(),
            avgOverallScore,
            avgCompletenessScore,
            avgAccuracyScore,
            avgConsistencyScore,
            avgTimelinessScore,
            avgUniquenessScore,
            avgValidityScore,
            complianceRate,
            trendDirection
        );
    }

    private String calculateTrendDirection(List<QualityReport> reports) {
        if (reports.size() < 2) {
            return "STABLE";
        }

        // Sort by creation time
        List<QualityReport> sortedReports = reports.stream()
            .sorted((r1, r2) -> r1.getCreatedAt().compareTo(r2.getCreatedAt()))
            .collect(Collectors.toList());

        int midPoint = sortedReports.size() / 2;

        // Calculate average for first half
        double firstHalfAvg = sortedReports.subList(0, midPoint).stream()
            .filter(r -> r.getScores() != null)
            .mapToDouble(r -> r.getScores().overallScore())
            .average()
            .orElse(0.0);

        // Calculate average for second half
        double secondHalfAvg = sortedReports.subList(midPoint, sortedReports.size()).stream()
            .filter(r -> r.getScores() != null)
            .mapToDouble(r -> r.getScores().overallScore())
            .average()
            .orElse(0.0);

        double difference = secondHalfAvg - firstHalfAvg;

        if (Math.abs(difference) < 1.0) { // Less than 1% change
            return "STABLE";
        } else if (difference > 0) {
            return "IMPROVING";
        } else {
            return "DECLINING";
        }
    }
}