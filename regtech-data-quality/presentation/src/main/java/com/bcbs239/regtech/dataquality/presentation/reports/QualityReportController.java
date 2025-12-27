package com.bcbs239.regtech.dataquality.presentation.reports;

import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.dataquality.application.monitoring.BatchQualityTrendsQuery;
import com.bcbs239.regtech.dataquality.application.monitoring.BatchQualityTrendsQueryHandler;
import com.bcbs239.regtech.dataquality.application.monitoring.QualityTrendsDto;
import com.bcbs239.regtech.dataquality.application.reporting.QualityReportPresentationService;
import com.bcbs239.regtech.dataquality.domain.shared.BankId;
import com.bcbs239.regtech.dataquality.domain.shared.BatchId;
import com.bcbs239.regtech.dataquality.domain.model.presentation.QualityReportPresentation;
import com.bcbs239.regtech.dataquality.domain.report.QualityStatus;
import com.bcbs239.regtech.dataquality.presentation.common.IEndpoint;
import com.bcbs239.regtech.dataquality.presentation.web.QualityRequestValidator;
import com.bcbs239.regtech.dataquality.presentation.web.QualityRequestValidator.TrendsQueryParams;
import com.bcbs239.regtech.dataquality.presentation.web.QualityResponseHandler;
import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;


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

        private final QualityReportPresentationService presentationService;
    private final BatchQualityTrendsQueryHandler trendsQueryHandler;
    private final QualityRequestValidator requestValidator;
    //private final QualitySecurityService securityService;
    private final QualityResponseHandler responseHandler;
    
    public QualityReportController(
                QualityReportPresentationService presentationService,
        BatchQualityTrendsQueryHandler trendsQueryHandler,
        QualityRequestValidator requestValidator,
      //  QualitySecurityService securityService,
                QualityResponseHandler responseHandler
    ) {
                this.presentationService = presentationService;
        this.trendsQueryHandler = trendsQueryHandler;
        this.requestValidator = requestValidator;
      //  this.securityService = securityService;
        this.responseHandler = responseHandler;
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
     * Endpoint: GET /api/v1/data-quality/reports?bankId=...
     *
     * Returns the most recent COMPLETED report for the given bank.
     */
    @Observed(name = "data-quality.api.report.get", contextualName = "get-quality-report")
    public ServerResponse getQualityReport(ServerRequest request) {
        try {
            String bankIdStr = request.param("bankId").orElse(null);

            logger.debug("Processing quality report request for bankId={}", bankIdStr);

            // Extract and validate bankId
            Result<BankId> bankIdResult = requestValidator.validateBankId(bankIdStr);
            if (bankIdResult.isFailure()) {
                return responseHandler.handleErrorResponse(bankIdResult.getError().orElseThrow());
            }
            BankId bankId = bankIdResult.getValue().orElseThrow();

            // Verify user has access to this batch (bank-level security)
//            Result<Void> accessResult = securityService.verifyBatchAccess(batchId);
//            if (accessResult.isFailure()) {
//                return responseHandler.handleErrorResponse(accessResult.getError().orElseThrow());
//            }

            // Generate presentation model (new frontend payload)
            QualityReportPresentation presentation = presentationService.getLatestFrontendPresentation(bankId);

            // Handle response
            return responseHandler.handleSuccessResult(
                Result.success(presentation),
                "Quality report retrieved successfully",
                "data-quality.report.retrieved"
            );

        } catch (Exception e) {
            logger.error("Unexpected error processing quality report request: {}", e.getMessage(), e);
            return responseHandler.handleSystemErrorResponse(e);
        }
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

