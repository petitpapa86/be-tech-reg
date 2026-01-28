package com.bcbs239.regtech.dataquality.application.reporting;

import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;
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
            
            logger.info("Handling GetQualityReportQuery for batch {}", query.batchId().value());
            
            Optional<QualityReport> reportOptional = qualityReportRepository.findByBatchId(query.batchId());
            
            if (reportOptional.isEmpty()) {
                logger.info("Quality report not found for batch {}", query.batchId().value());
                return Result.failure(ErrorDetail.of(
                    "QUALITY_REPORT_NOT_FOUND",
                    ErrorType.NOT_FOUND_ERROR,
                    "Quality report not found for batch: " + query.batchId().value(),
                    "query.report.not_found"
                ));
            }
            
            QualityReport report = reportOptional.get();
            QualityReportDto dto = QualityReportDto.fromDomain(report);
            
            logger.info("Successfully retrieved quality report {} for batch {}", 
                report.getReportId().value(), query.batchId().value());
            
            return Result.success(dto);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid query parameters: {}", e.getMessage());
            return Result.failure(ErrorDetail.of(
                "INVALID_QUERY_PARAMETERS",
                ErrorType.VALIDATION_ERROR,
                e.getMessage(),
                "query.validation.parameters"
            ));
        } catch (Exception e) {
            logger.error("Failed to retrieve quality report for batch {}: {}", 
                query.batchId().value(), e.getMessage(), e);
            return Result.failure(ErrorDetail.of(
                "QUERY_EXECUTION_FAILED",
                ErrorType.SYSTEM_ERROR,
                "Failed to retrieve quality report: " + e.getMessage(),
                "query.execution.report"
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
            return Result.failure(ErrorDetail.of(
                "INVALID_QUERY_PARAMETERS",
                ErrorType.VALIDATION_ERROR,
                e.getMessage(),
                "query.validation.parameters"
            ));
        } catch (Exception e) {
            return Result.failure(ErrorDetail.of(
                "QUERY_EXECUTION_FAILED",
                ErrorType.SYSTEM_ERROR,
                "Failed to check quality report existence: " + e.getMessage(),
                "query.execution.exists"
            ));
        }
    }
}

