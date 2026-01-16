package com.bcbs239.regtech.dataquality.application.monitoring;

import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.dataquality.application.reporting.QualityReportSummaryDto;
import com.bcbs239.regtech.dataquality.domain.quality.QualityTrendsData;
import com.bcbs239.regtech.dataquality.domain.quality.QualityTrendsService;
import com.bcbs239.regtech.dataquality.domain.report.IQualityReportRepository;
import com.bcbs239.regtech.dataquality.domain.report.QualityReport;
import com.bcbs239.regtech.dataquality.domain.shared.BankId;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Query handler for retrieving quality trends and historical analysis.
 * Handles queries for quality trends over time periods.
 */
@Component
public class BatchQualityTrendsQueryHandler {
    private static final Logger logger = LoggerFactory.getLogger(BatchQualityTrendsQueryHandler.class);
    
    private final IQualityReportRepository qualityReportRepository;
    private final QualityTrendsService qualityTrendsService;
    
    public BatchQualityTrendsQueryHandler(
        IQualityReportRepository qualityReportRepository,
        QualityTrendsService qualityTrendsService
    ) {
        this.qualityReportRepository = qualityReportRepository;
        this.qualityTrendsService = qualityTrendsService;
    }
    
    /**
     * Handles the BatchQualityTrendsQuery to retrieve quality trends for a bank.
     */
    public Result<QualityTrendsDto> handle(BatchQualityTrendsQuery query) {
        try {
            query.validate();
            logger.info("Handling BatchQualityTrendsQuery for bankId={} start={} end={} limit={}",
                query.bankId().value(), query.startTime(), query.endTime(), query.limit());
            
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
            
            // Calculate trend statistics using domain service
            QualityTrendsData trendsData = qualityTrendsService.calculateTrends(
                query.bankId(), query.startTime(), query.endTime(), reports);
            
            // Convert to DTO
            QualityTrendsDto trendsDto = QualityTrendsDto.fromDomain(trendsData, reportSummaries);

            logger.info("BatchQualityTrendsQuery completed for bankId={} reportsReturned={}",
                query.bankId().value(), reportSummaries.size());

            return Result.success(trendsDto);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid BatchQualityTrendsQuery parameters: {}", e.getMessage());
            return Result.failure(ErrorDetail.of(
                "INVALID_QUERY_PARAMETERS",
                ErrorType.VALIDATION_ERROR,
                e.getMessage(),
                "query.validation.parameters"
            ));
        } catch (Exception e) {
            logger.error("Failed to execute BatchQualityTrendsQuery: {}", e.getMessage(), e);
            return Result.failure(ErrorDetail.of(
                "QUERY_EXECUTION_FAILED",
                ErrorType.SYSTEM_ERROR,
                "Failed to retrieve quality trends: " + e.getMessage(),
                "query.execution.trends"
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
            logger.debug("Getting reports below threshold for bankId={} threshold={}", bankId.value(), threshold);
            List<QualityReport> reports = qualityReportRepository.findByBankIdAndOverallScoreBelow(
                bankId, threshold);
            
            List<QualityReportSummaryDto> summaries = reports.stream()
                .map(QualityReportSummaryDto::fromDomain)
                .sorted((s1, s2) -> s2.createdAt().compareTo(s1.createdAt())) // Most recent first
                .collect(Collectors.toList());
            
            logger.info("Found {} reports below threshold={} for bankId={}", summaries.size(), threshold, bankId.value());
            return Result.success(summaries);
            
        } catch (Exception e) {
            logger.error("Failed to retrieve reports below threshold for bankId={}: {}", bankId.value(), e.getMessage(), e);
            return Result.failure(ErrorDetail.of(
                "QUERY_EXECUTION_FAILED",
                ErrorType.SYSTEM_ERROR,
                "Failed to retrieve reports below threshold: " + e.getMessage(),
                "query.execution.threshold"
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
            logger.debug("Getting most recent report for bankId={}", bankId.value());
            var reportOptional = qualityReportRepository.findMostRecentByBankId(bankId);
            
            if (reportOptional.isEmpty()) {
                return Result.failure(ErrorDetail.of(
                    "NO_REPORTS_FOUND",
                    ErrorType.NOT_FOUND_ERROR,
                    "No quality reports found for bank: " + bankId.value(),
                    "query.reports.not_found"
                ));
            }
            
            QualityReportSummaryDto summary = QualityReportSummaryDto.fromDomain(reportOptional.get());
            
            logger.info("Most recent report found for bankId={} reportId={}", bankId.value(), reportOptional.get().getReportId().value());
            return Result.success(summary);
            
        } catch (Exception e) {
            logger.error("Failed to retrieve most recent report for bankId={}: {}", bankId.value(), e.getMessage(), e);
            return Result.failure(ErrorDetail.of(
                "QUERY_EXECUTION_FAILED",
                ErrorType.SYSTEM_ERROR,
                "Failed to retrieve most recent report: " + e.getMessage(),
                "query.execution.recent"
            ));
        }
    }
}

