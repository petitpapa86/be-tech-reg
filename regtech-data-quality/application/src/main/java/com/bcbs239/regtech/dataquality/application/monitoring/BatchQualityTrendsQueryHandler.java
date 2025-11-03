package com.bcbs239.regtech.dataquality.application.monitoring;

import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.dataquality.application.reporting.QualityReportSummaryDto;
import com.bcbs239.regtech.dataquality.domain.shared.BankId;
import com.bcbs239.regtech.dataquality.domain.report.IQualityReportRepository;
import com.bcbs239.regtech.dataquality.domain.report.QualityReport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Query handler for retrieving quality trends and historical analysis.
 * Handles queries for quality trends over time periods.
 */
@Component
@Transactional(readOnly = true)
public class BatchQualityTrendsQueryHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(BatchQualityTrendsQueryHandler.class);
    
    private final IQualityReportRepository qualityReportRepository;
    
    public BatchQualityTrendsQueryHandler(IQualityReportRepository qualityReportRepository) {
        this.qualityReportRepository = qualityReportRepository;
    }
    
    /**
     * Handles the BatchQualityTrendsQuery to retrieve quality trends for a bank.
     */
    public Result<QualityTrendsDto> handle(BatchQualityTrendsQuery query) {
        try {
            query.validate();
            
            logger.debug("Retrieving quality trends for bank {} from {} to {}", 
                query.bankId().value(), query.startTime(), query.endTime());
            
            // Get quality reports for the bank within the time period
            List<QualityReport> reports = qualityReportRepository.findByBankIdAndCreatedAtBetween(
                query.bankId(), query.startTime(), query.endTime());
            
            // Limit results if necessary
            if (reports.size() > query.limit()) {
                reports = reports.stream()
                    .sorted((r1, r2) -> r2.getCreatedAt().compareTo(r1.getCreatedAt())) // Most recent first
                    .limit(query.limit())
                    .collect(Collectors.toList());
            }
            
            // Convert to summary DTOs
            List<QualityReportSummaryDto> reportSummaries = reports.stream()
                .map(QualityReportSummaryDto::fromDomain)
                .collect(Collectors.toList());
            
            // Calculate trend statistics
            QualityTrendsDto trendsDto = calculateTrends(query, reportSummaries);
            
            logger.debug("Successfully retrieved {} quality reports for trends analysis", reports.size());
            
            return Result.success(trendsDto);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid query parameters: {}", e.getMessage());
            return Result.failure(ErrorDetail.of(
                "INVALID_QUERY_PARAMETERS",
                e.getMessage(),
                "query"
            ));
        } catch (Exception e) {
            logger.error("Failed to retrieve quality trends for bank {}: {}", 
                query.bankId().value(), e.getMessage(), e);
            return Result.failure(ErrorDetail.of(
                "QUERY_EXECUTION_FAILED",
                "Failed to retrieve quality trends: " + e.getMessage(),
                "query"
            ));
        }
    }
    
    /**
     * Gets quality reports with scores below a threshold for a bank.
     */
    public Result<List<QualityReportSummaryDto>> getReportsBelowThreshold(
        BankId bankId,
        double threshold
    ) {
        try {
            logger.debug("Retrieving quality reports below threshold {} for bank {}", 
                threshold, bankId.value());
            
            List<QualityReport> reports = qualityReportRepository.findByBankIdAndOverallScoreBelow(
                bankId, threshold);
            
            List<QualityReportSummaryDto> summaries = reports.stream()
                .map(QualityReportSummaryDto::fromDomain)
                .sorted((s1, s2) -> s2.createdAt().compareTo(s1.createdAt())) // Most recent first
                .collect(Collectors.toList());
            
            logger.debug("Found {} quality reports below threshold {} for bank {}", 
                summaries.size(), threshold, bankId.value());
            
            return Result.success(summaries);
            
        } catch (Exception e) {
            logger.error("Failed to retrieve reports below threshold for bank {}: {}", 
                bankId.value(), e.getMessage(), e);
            return Result.failure(ErrorDetail.of(
                "QUERY_EXECUTION_FAILED",
                "Failed to retrieve reports below threshold: " + e.getMessage(),
                "query"
            ));
        }
    }
    
    /**
     * Gets the most recent quality report for a bank.
     */
    public Result<QualityReportSummaryDto> getMostRecentReport(
        BankId bankId
    ) {
        try {
            logger.debug("Retrieving most recent quality report for bank {}", bankId.value());
            
            var reportOptional = qualityReportRepository.findMostRecentByBankId(bankId);
            
            if (reportOptional.isEmpty()) {
                logger.debug("No quality reports found for bank {}", bankId.value());
                return Result.failure(ErrorDetail.of(
                    "NO_REPORTS_FOUND",
                    "No quality reports found for bank: " + bankId.value(),
                    "bankId"
                ));
            }
            
            QualityReportSummaryDto summary = QualityReportSummaryDto.fromDomain(reportOptional.get());
            
            logger.debug("Retrieved most recent quality report {} for bank {}", 
                summary.reportId(), bankId.value());
            
            return Result.success(summary);
            
        } catch (Exception e) {
            logger.error("Failed to retrieve most recent report for bank {}: {}", 
                bankId.value(), e.getMessage(), e);
            return Result.failure(ErrorDetail.of(
                "QUERY_EXECUTION_FAILED",
                "Failed to retrieve most recent report: " + e.getMessage(),
                "query"
            ));
        }
    }
    
    private QualityTrendsDto calculateTrends(
        BatchQualityTrendsQuery query, 
        List<QualityReportSummaryDto> reports
    ) {
        if (reports.isEmpty()) {
            return QualityTrendsDto.empty(query.bankId(), query.startTime(), query.endTime());
        }
        
        // Calculate average scores
        double avgOverallScore = reports.stream()
            .filter(r -> r.scores() != null)
            .mapToDouble(r -> r.scores().overallScore())
            .average()
            .orElse(0.0);
        
        double avgCompletenessScore = reports.stream()
            .filter(r -> r.scores() != null)
            .mapToDouble(r -> r.scores().completenessScore())
            .average()
            .orElse(0.0);
        
        double avgAccuracyScore = reports.stream()
            .filter(r -> r.scores() != null)
            .mapToDouble(r -> r.scores().accuracyScore())
            .average()
            .orElse(0.0);
        
        double avgConsistencyScore = reports.stream()
            .filter(r -> r.scores() != null)
            .mapToDouble(r -> r.scores().consistencyScore())
            .average()
            .orElse(0.0);
        
        double avgTimelinessScore = reports.stream()
            .filter(r -> r.scores() != null)
            .mapToDouble(r -> r.scores().timelinessScore())
            .average()
            .orElse(0.0);
        
        double avgUniquenessScore = reports.stream()
            .filter(r -> r.scores() != null)
            .mapToDouble(r -> r.scores().uniquenessScore())
            .average()
            .orElse(0.0);
        
        double avgValidityScore = reports.stream()
            .filter(r -> r.scores() != null)
            .mapToDouble(r -> r.scores().validityScore())
            .average()
            .orElse(0.0);
        
        // Calculate compliance rate
        long compliantCount = reports.stream()
            .filter(r -> r.scores() != null && r.scores().isCompliant())
            .count();
        
        double complianceRate = reports.isEmpty() ? 0.0 : (double) compliantCount / reports.size();
        
        // Calculate trend direction (comparing first half vs second half)
        String trendDirection = calculateTrendDirection(reports);
        
        return QualityTrendsDto.builder()
            .bankId(query.bankId())
            .startTime(query.startTime())
            .endTime(query.endTime())
            .totalReports(reports.size())
            .reports(reports)
            .averageOverallScore(avgOverallScore)
            .averageCompletenessScore(avgCompletenessScore)
            .averageAccuracyScore(avgAccuracyScore)
            .averageConsistencyScore(avgConsistencyScore)
            .averageTimelinessScore(avgTimelinessScore)
            .averageUniquenessScore(avgUniquenessScore)
            .averageValidityScore(avgValidityScore)
            .complianceRate(complianceRate)
            .trendDirection(trendDirection)
            .build();
    }
    
    private String calculateTrendDirection(List<QualityReportSummaryDto> reports) {
        if (reports.size() < 2) {
            return "STABLE";
        }
        
        // Sort by creation time
        List<QualityReportSummaryDto> sortedReports = reports.stream()
            .sorted((r1, r2) -> r1.createdAt().compareTo(r2.createdAt()))
            .collect(Collectors.toList());
        
        int midPoint = sortedReports.size() / 2;
        
        // Calculate average for first half
        double firstHalfAvg = sortedReports.subList(0, midPoint).stream()
            .filter(r -> r.scores() != null)
            .mapToDouble(r -> r.scores().overallScore())
            .average()
            .orElse(0.0);
        
        // Calculate average for second half
        double secondHalfAvg = sortedReports.subList(midPoint, sortedReports.size()).stream()
            .filter(r -> r.scores() != null)
            .mapToDouble(r -> r.scores().overallScore())
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