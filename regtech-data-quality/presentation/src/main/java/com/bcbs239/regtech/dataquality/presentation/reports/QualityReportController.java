package com.bcbs239.regtech.dataquality.presentation.reports;

import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.dataquality.application.monitoring.BatchQualityTrendsQuery;
import com.bcbs239.regtech.dataquality.application.monitoring.BatchQualityTrendsQueryHandler;
import com.bcbs239.regtech.dataquality.application.monitoring.QualityTrendsDto;
import com.bcbs239.regtech.dataquality.application.reporting.DetailedExposureResult;
import com.bcbs239.regtech.dataquality.application.reporting.DetailedQualityReportDto;
import com.bcbs239.regtech.dataquality.application.reporting.GetQualityReportQuery;
import com.bcbs239.regtech.dataquality.application.reporting.QualityReportDto;
import com.bcbs239.regtech.dataquality.application.reporting.QualityReportQueryHandler;
import com.bcbs239.regtech.dataquality.domain.shared.BankId;
import com.bcbs239.regtech.dataquality.domain.shared.BatchId;
import com.bcbs239.regtech.dataquality.presentation.common.IEndpoint;
import com.bcbs239.regtech.dataquality.presentation.web.QualityRequestValidator;
import com.bcbs239.regtech.dataquality.presentation.web.QualityRequestValidator.TrendsQueryParams;
import com.bcbs239.regtech.dataquality.presentation.web.QualityResponseHandler;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Quality report controller providing functional endpoints for quality report queries.
 * Handles retrieval of individual quality reports and historical trends analysis.
 *
 * This controller focuses on orchestrating the request flow by delegating to:
 * - QualityRequestValidator for input validation
 * - QualitySecurityService for authentication and authorization
 * - Query handlers for business logic execution
 * - QualityResponseHandler for response formatting
 *
 * Requirements: 9.1, 9.2, 9.3, 9.4
 */
@Component
public class QualityReportController implements IEndpoint {

    private static final Logger logger = LoggerFactory.getLogger(QualityReportController.class);

    private final QualityReportQueryHandler qualityReportQueryHandler;
    private final BatchQualityTrendsQueryHandler trendsQueryHandler;
    private final QualityRequestValidator requestValidator;
    //private final QualitySecurityService securityService;
    private final QualityResponseHandler responseHandler;
    private final ObjectMapper objectMapper;

    @Value("${data-quality.storage.local.base-path:./data/quality}")
    private String localStorageBasePath;
    
    public QualityReportController(
        QualityReportQueryHandler qualityReportQueryHandler,
        BatchQualityTrendsQueryHandler trendsQueryHandler,
        QualityRequestValidator requestValidator,
      //  QualitySecurityService securityService,
        QualityResponseHandler responseHandler,
        ObjectMapper objectMapper
    ) {
        this.qualityReportQueryHandler = qualityReportQueryHandler;
        this.trendsQueryHandler = trendsQueryHandler;
        this.requestValidator = requestValidator;
      //  this.securityService = securityService;
        this.responseHandler = responseHandler;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Maps the quality report endpoints with proper authentication and authorization.
     * Note: This method is implemented for the IEndpoint interface but routing is 
     * handled by QualityReportRoutes to avoid circular dependencies.
     */
    @Override
    public RouterFunction<ServerResponse> mapEndpoints() {
        // This is handled by QualityReportRoutes to avoid circular dependency
        throw new UnsupportedOperationException(
            "Endpoint mapping is handled by QualityReportRoutes component"
        );
    }
    
    /**
     * Get quality report for a specific batch with detailed exposure results.
     * Endpoint: GET /api/v1/data-quality/reports/{batchId}
     */
    @Observed(name = "data-quality.api.report.get", contextualName = "get-quality-report")
    public ServerResponse getQualityReport(ServerRequest request) {
        try {
            logger.debug("Processing quality report request for batch: {}",
                request.pathVariable("batchId"));

            // Extract and validate batch ID
            String batchIdStr = request.pathVariable("batchId");
            Result<BatchId> batchIdResult = requestValidator.validateBatchId(batchIdStr);
            if (batchIdResult.isFailure()) {
                return responseHandler.handleErrorResponse(batchIdResult.getError().orElseThrow());
            }

            BatchId batchId = batchIdResult.getValue().orElseThrow();

            // Verify user has access to this batch (bank-level security)
//            Result<Void> accessResult = securityService.verifyBatchAccess(batchId);
//            if (accessResult.isFailure()) {
//                return responseHandler.handleErrorResponse(accessResult.getError().orElseThrow());
//            }

            // Create and execute query
            GetQualityReportQuery query = GetQualityReportQuery.forBatch(batchId);
            Result<QualityReportDto> result = qualityReportQueryHandler.handle(query);

            if (result.isFailure()) {
                return responseHandler.handleErrorResponse(result.getError().orElseThrow());
            }

            QualityReportDto basicReport = result.getValue().orElseThrow();

            // Try to load detailed results if available
            List<DetailedExposureResult> detailedResults = new ArrayList<>();
            if (basicReport.detailsUri() != null && basicReport.isCompleted()) {
                try {
                    detailedResults = loadDetailedResults(basicReport.detailsUri());
                    logger.debug("Loaded {} detailed exposure results for batch {}",
                        detailedResults.size(), batchIdStr);
                } catch (Exception e) {
                    logger.warn("Failed to load detailed results for batch {}: {}",
                        batchIdStr, e.getMessage());
                    // Continue without detailed results
                }
            }

            // Create detailed response
            DetailedQualityReportDto detailedReport = DetailedQualityReportDto.fromDto(
                basicReport, detailedResults
            );

            // Handle response
            return responseHandler.handleSuccessResult(
                Result.success(detailedReport),
                "Quality report retrieved successfully",
                "data-quality.report.retrieved"
            );

        } catch (Exception e) {
            logger.error("Unexpected error processing quality report request: {}", e.getMessage(), e);
            return responseHandler.handleSystemErrorResponse(e);
        }
    }
    
    /**
     * Loads detailed exposure results from the JSON file.
     * Converts S3 URI to local file path and parses the JSON.
     */
    private List<DetailedExposureResult> loadDetailedResults(String detailsUri) throws IOException {
        if (detailsUri == null || !detailsUri.startsWith("s3://local/")) {
            return new ArrayList<>();
        }

        // Convert S3 URI to local file path
        // s3://local/quality/quality_batch_xxx.json -> ./data/quality/quality/quality_batch_xxx.json
        String relativePath = detailsUri.replace("s3://local/", "");
        Path filePath = Paths.get(localStorageBasePath, relativePath);

        if (!Files.exists(filePath)) {
            logger.warn("Detailed results file not found: {}", filePath);
            return new ArrayList<>();
        }

        // Read and parse JSON
        String jsonContent = Files.readString(filePath);
        JsonNode rootNode = objectMapper.readTree(jsonContent);

        // Extract exposureResults array
        JsonNode exposureResultsNode = rootNode.get("exposureResults");
        if (exposureResultsNode == null || !exposureResultsNode.isArray()) {
            return new ArrayList<>();
        }

        // Parse to DetailedExposureResult list
        return objectMapper.convertValue(
            exposureResultsNode,
            new TypeReference<List<DetailedExposureResult>>() {}
        );
    }
    @Observed(name = "data-quality.api.trends.get", contextualName = "get-quality-trends")
    public ServerResponse getQualityTrends(ServerRequest request) {
        try {
            logger.debug("Processing quality trends request");
            
            // Extract current bank ID from security context
//            Result<BankId> bankIdResult = securityService.getCurrentBankId();
//            if (bankIdResult.isFailure()) {
//                return responseHandler.handleErrorResponse(bankIdResult.getError().orElseThrow());
//            }
            
            BankId bankId = BankId.of("daa4a072");//bankIdResult.getValue().orElseThrow();
            
            // Parse query parameters
            Result<TrendsQueryParams> paramsResult = requestValidator.parseTrendsQueryParams(request);
            if (paramsResult.isFailure()) {
                return responseHandler.handleErrorResponse(paramsResult.getError().orElseThrow());
            }
            
            TrendsQueryParams params = paramsResult.getValue().orElseThrow();
            
            // Create and execute query
            BatchQualityTrendsQuery query = BatchQualityTrendsQuery.forBankAndPeriodWithLimit(
                bankId,
                params.startTime(),
                params.endTime(),
                params.limit()
            );
            
            Result<QualityTrendsDto> result = trendsQueryHandler.handle(query);
            
            // Handle response
            return responseHandler.handleSuccessResult(
                result,
                "Quality trends retrieved successfully",
                "data-quality.trends.retrieved"
            );
                
        } catch (Exception e) {
            logger.error("Unexpected error processing quality trends request: {}", e.getMessage(), e);
            return responseHandler.handleSystemErrorResponse(e);
        }
    }

}

