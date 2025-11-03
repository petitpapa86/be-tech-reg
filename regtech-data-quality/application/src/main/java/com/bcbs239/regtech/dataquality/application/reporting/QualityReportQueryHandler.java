package com.bcbs239.regtech.dataquality.application.reporting;

import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.dataquality.domain.report.IQualityReportRepository;
import com.bcbs239.regtech.dataquality.domain.report.QualityReport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Query handler for retrieving quality reports.
 * Handles queries for individual quality reports and converts them to DTOs.
 */
@Component
@Transactional(readOnly = true)
public class QualityReportQueryHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(QualityReportQueryHandler.class);
    
    private final IQualityReportRepository qualityReportRepository;
    
    public QualityReportQueryHandler(IQualityReportRepository qualityReportRepository) {
        this.qualityReportRepository = qualityReportRepository;
    }
    
    /**
     * Handles the GetQualityReportQuery to retrieve a quality report by batch ID.
     */
    public Result<QualityReportDto> handle(GetQualityReportQuery query) {
        try {
            query.validate();
            
            logger.debug("Retrieving quality report for batch {}", query.batchId().value());
            
            Optional<QualityReport> reportOptional = qualityReportRepository.findByBatchId(query.batchId());
            
            if (reportOptional.isEmpty()) {
                logger.debug("Quality report not found for batch {}", query.batchId().value());
                return Result.failure(ErrorDetail.of(
                    "QUALITY_REPORT_NOT_FOUND",
                    "Quality report not found for batch: " + query.batchId().value(),
                    "batchId"
                ));
            }
            
            QualityReport report = reportOptional.get();
            QualityReportDto dto = QualityReportDto.fromDomain(report);
            
            logger.debug("Successfully retrieved quality report {} for batch {}", 
                report.getReportId().value(), query.batchId().value());
            
            return Result.success(dto);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid query parameters: {}", e.getMessage());
            return Result.failure(ErrorDetail.of(
                "INVALID_QUERY_PARAMETERS",
                e.getMessage(),
                "query"
            ));
        } catch (Exception e) {
            logger.error("Failed to retrieve quality report for batch {}: {}", 
                query.batchId().value(), e.getMessage(), e);
            return Result.failure(ErrorDetail.of(
                "QUERY_EXECUTION_FAILED",
                "Failed to retrieve quality report: " + e.getMessage(),
                "query"
            ));
        }
    }
    
    /**
     * Checks if a quality report exists for the given batch ID.
     */
    public Result<Boolean> exists(GetQualityReportQuery query) {
        try {
            query.validate();
            
            boolean exists = qualityReportRepository.existsByBatchId(query.batchId());
            
            logger.debug("Quality report exists for batch {}: {}", query.batchId().value(), exists);
            
            return Result.success(exists);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid query parameters: {}", e.getMessage());
            return Result.failure(ErrorDetail.of(
                "INVALID_QUERY_PARAMETERS",
                e.getMessage(),
                "query"
            ));
        } catch (Exception e) {
            logger.error("Failed to check quality report existence for batch {}: {}", 
                query.batchId().value(), e.getMessage(), e);
            return Result.failure(ErrorDetail.of(
                "QUERY_EXECUTION_FAILED",
                "Failed to check quality report existence: " + e.getMessage(),
                "query"
            ));
        }
    }
}