package com.bcbs239.regtech.modules.ingestion.presentation.compliance.reports;

import com.bcbs239.regtech.core.shared.BaseController;
import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.core.shared.ResponseUtils;
import com.bcbs239.regtech.modules.ingestion.infrastructure.compliance.ComplianceReportData;
import com.bcbs239.regtech.modules.ingestion.infrastructure.compliance.DataRetentionService;
import com.bcbs239.regtech.modules.ingestion.presentation.common.IEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.springframework.web.servlet.function.RequestPredicates.GET;
import static org.springframework.web.servlet.function.RouterFunctions.route;

/**
 * Functional endpoint for compliance reporting operations.
 * Handles generation and retrieval of compliance reports.
 */
@Component
@Slf4j
public class ComplianceReportsController extends BaseController implements IEndpoint {
    
    private final DataRetentionService dataRetentionService;
    
    public ComplianceReportsController(DataRetentionService dataRetentionService) {
        this.dataRetentionService = dataRetentionService;
    }
    
    @Override
    public RouterFunction<ServerResponse> mapEndpoint() {
        return route(GET("/api/v1/ingestion/compliance/report"), this::generateComplianceReport)
            .and(route(GET("/api/v1/ingestion/compliance/status"), this::getComplianceStatus))
            .withAttribute("tags", new String[]{"Compliance Reports", "Ingestion"})
            .withAttribute("permissions", new String[]{"ingestion:compliance:view"});
    }
    
    private ServerResponse generateComplianceReport(ServerRequest request) {
        try {
            // Extract query parameters
            String startDateStr = request.queryParam("startDate").orElse(null);
            String endDateStr = request.queryParam("endDate").orElse(null);
            
            if (startDateStr == null || endDateStr == null) {
                return ServerResponse.badRequest()
                    .body(ResponseUtils.validationError(
                        java.util.List.of(),
                        "Both startDate and endDate query parameters are required (format: YYYY-MM-DD)"
                    ));
            }
            
            LocalDate startDate = LocalDate.parse(startDateStr);
            LocalDate endDate = LocalDate.parse(endDateStr);
            
            log.info("Generating compliance report for period {} to {}", startDate, endDate);
            
            Instant startInstant = startDate.atStartOfDay().toInstant(ZoneOffset.UTC);
            Instant endInstant = endDate.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
            
            Result<ComplianceReportData> result = dataRetentionService.generateComplianceReport(
                    startInstant, endInstant);
            
            if (result.isSuccess()) {
                ComplianceReportData report = result.getValue();
                log.info("Generated compliance report: {}", report.getComplianceSummary());
                return ServerResponse.ok()
                    .body(ResponseUtils.success(report, "Compliance report generated successfully"));
            } else {
                log.error("Failed to generate compliance report: {}", result.getError().orElse(null));
                ResponseEntity<?> responseEntity = handleResult(result, 
                    "Compliance report generated", "compliance.report.success");
                return ServerResponse.status(responseEntity.getStatusCode())
                    .body(responseEntity.getBody());
            }
        } catch (Exception e) {
            log.error("Error generating compliance report: {}", e.getMessage(), e);
            return ServerResponse.status(500)
                .body(ResponseUtils.systemError("Failed to generate compliance report: " + e.getMessage()));
        }
    }
    
    private ServerResponse getComplianceStatus(ServerRequest request) {
        try {
            log.info("Retrieving compliance status summary");
            
            // Generate a quick report for the last 30 days
            Instant endDate = Instant.now();
            Instant startDate = endDate.minus(30, java.time.temporal.ChronoUnit.DAYS);
            
            Result<ComplianceReportData> result = dataRetentionService.generateComplianceReport(
                    startDate, endDate);
            
            if (result.isSuccess()) {
                ComplianceReportData report = result.getValue();
                ComplianceStatusSummary summary = ComplianceStatusSummary.builder()
                        .isCompliant(report.isCompliant())
                        .complianceScore(report.calculateComplianceScore())
                        .totalFilesUnderRetention(report.getTotalFilesUnderRetention())
                        .totalViolations(report.getComplianceViolations().size())
                        .filesApproachingExpiry(report.getFilesApproachingExpiry().size())
                        .filesEligibleForDeletion(report.getFilesEligibleForDeletion().size())
                        .lastReportGenerated(report.getReportGeneratedAt())
                        .summary(report.getComplianceSummary())
                        .build();
                
                return ServerResponse.ok()
                    .body(ResponseUtils.success(summary, "Compliance status retrieved successfully"));
            } else {
                log.error("Failed to get compliance status: {}", result.getError().orElse(null));
                ResponseEntity<?> responseEntity = handleResult(result, 
                    "Compliance status retrieved", "compliance.status.success");
                return ServerResponse.status(responseEntity.getStatusCode())
                    .body(responseEntity.getBody());
            }
        } catch (Exception e) {
            log.error("Error retrieving compliance status: {}", e.getMessage(), e);
            return ServerResponse.status(500)
                .body(ResponseUtils.systemError("Failed to retrieve compliance status: " + e.getMessage()));
        }
    }
}