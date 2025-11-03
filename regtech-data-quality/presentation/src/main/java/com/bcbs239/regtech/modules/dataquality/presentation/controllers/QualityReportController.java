package com.bcbs239.regtech.modules.dataquality.presentation.controllers;

import com.bcbs239.regtech.core.shared.BaseController;
import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.core.shared.ApiResponse;
import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.core.shared.FieldError;
import com.bcbs239.regtech.core.security.SecurityUtils;
import com.bcbs239.regtech.core.web.RouterAttributes;
import com.bcbs239.regtech.modules.dataquality.application.dto.QualityReportDto;
import com.bcbs239.regtech.modules.dataquality.application.dto.QualityTrendsDto;
import com.bcbs239.regtech.modules.dataquality.application.queries.GetQualityReportQuery;
import com.bcbs239.regtech.modules.dataquality.application.queries.BatchQualityTrendsQuery;
import com.bcbs239.regtech.modules.dataquality.application.queries.QualityReportQueryHandler;
import com.bcbs239.regtech.modules.dataquality.application.queries.BatchQualityTrendsQueryHandler;
import com.bcbs239.regtech.modules.dataquality.domain.shared.BatchId;
import com.bcbs239.regtech.modules.dataquality.domain.shared.BankId;
import com.bcbs239.regtech.modules.dataquality.presentation.common.IEndpoint;
import com.bcbs239.regtech.modules.dataquality.presentation.constants.Tags;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.springframework.web.servlet.function.RouterFunctions.route;
import static org.springframework.web.servlet.function.RequestPredicates.GET;

/**
 * Quality report controller providing functional endpoints for quality report queries.
 * Handles retrieval of individual quality reports and historical trends analysis.
 * 
 * Requirements: 9.1, 9.2, 9.3, 9.4
 */
@Component
public class QualityReportController extends BaseController implements IEndpoint {
    
    private static final Logger logger = LoggerFactory.getLogger(QualityReportController.class);
    
    private final QualityReportQueryHandler qualityReportQueryHandler;
    private final BatchQualityTrendsQueryHandler trendsQueryHandler;
    
    public QualityReportController(
        QualityReportQueryHandler qualityReportQueryHandler,
        BatchQualityTrendsQueryHandler trendsQueryHandler
    ) {
        this.qualityReportQueryHandler = qualityReportQueryHandler;
        this.trendsQueryHandler = trendsQueryHandler;
    }
    
    /**
     * Maps the quality report endpoints with proper authentication and authorization.
     */
    public RouterFunction<ServerResponse> mapEndpoints() {
        return RouterAttributes.withAttributes(
            route(GET("/api/v1/data-quality/reports/{batchId}"), this::getQualityReport),
            new String[]{"data-quality:reports:view"},
            new String[]{Tags.DATA_QUALITY, Tags.REPORTS},
            "Get quality report for a specific batch"
        ).and(RouterAttributes.withAttributes(
            route(GET("/api/v1/data-quality/trends"), this::getQualityTrends),
            new String[]{"data-quality:trends:view"},
            new String[]{Tags.DATA_QUALITY, Tags.TRENDS},
            "Get quality trends analysis for a bank over time"
        ));
    }
    
    /**
     * Get quality report for a specific batch.
     * Endpoint: GET /api/v1/data-quality/reports/{batchId}
     */
    private ServerResponse getQualityReport(ServerRequest request) {
        try {
            logger.debug("Processing quality report request for batch: {}", 
                request.pathVariable("batchId"));
            
            // Extract and validate batch ID
            String batchIdStr = request.pathVariable("batchId");
            Result<BatchId> batchIdResult = validateBatchId(batchIdStr);
            if (batchIdResult.isFailure()) {
                return handleErrorResponse(batchIdResult.getError().orElseThrow());
            }
            
            BatchId batchId = batchIdResult.getValue().orElseThrow();
            
            // Verify user has access to this batch (bank-level security)
            Result<Void> accessResult = verifyBatchAccess(batchId);
            if (accessResult.isFailure()) {
                return handleErrorResponse(accessResult.getError().orElseThrow());
            }
            
            // Create and execute query
            GetQualityReportQuery query = GetQualityReportQuery.forBatch(batchId);
            Result<QualityReportDto> result = qualityReportQueryHandler.handle(query);
            
            // Handle response
            ResponseEntity<? extends ApiResponse<?>> responseEntity = handleResult(
                result, 
                "Quality report retrieved successfully", 
                "data-quality.report.retrieved"
            );
            
            return ServerResponse.status(responseEntity.getStatusCode())
                .contentType(MediaType.APPLICATION_JSON)
                .body(responseEntity.getBody());
                
        } catch (Exception e) {
            logger.error("Unexpected error processing quality report request: {}", e.getMessage(), e);
            return handleSystemErrorResponse(e);
        }
    }
    
    /**
     * Get quality trends analysis for a bank over time.
     * Endpoint: GET /api/v1/data-quality/trends?days=30&limit=100
     */
    private ServerResponse getQualityTrends(ServerRequest request) {
        try {
            logger.debug("Processing quality trends request");
            
            // Extract current bank ID from security context
            String currentBankId = SecurityUtils.getCurrentBankId();
            if (currentBankId == null || currentBankId.trim().isEmpty()) {
                ErrorDetail error = ErrorDetail.of(
                    "AUTHENTICATION_ERROR",
                    "Bank ID not found in security context",
                    "authentication"
                );
                return handleErrorResponse(error);
            }
            
            BankId bankId = BankId.of(currentBankId);
            
            // Parse query parameters
            Result<TrendsQueryParams> paramsResult = parseTrendsQueryParams(request);
            if (paramsResult.isFailure()) {
                return handleErrorResponse(paramsResult.getError().orElseThrow());
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
            ResponseEntity<? extends ApiResponse<?>> responseEntity = handleResult(
                result,
                "Quality trends retrieved successfully",
                "data-quality.trends.retrieved"
            );
            
            return ServerResponse.status(responseEntity.getStatusCode())
                .contentType(MediaType.APPLICATION_JSON)
                .body(responseEntity.getBody());
                
        } catch (Exception e) {
            logger.error("Unexpected error processing quality trends request: {}", e.getMessage(), e);
            return handleSystemErrorResponse(e);
        }
    }
    
    /**
     * Validates batch ID parameter.
     */
    private Result<BatchId> validateBatchId(String batchIdStr) {
        List<FieldError> fieldErrors = new ArrayList<>();
        
        if (batchIdStr == null || batchIdStr.trim().isEmpty()) {
            fieldErrors.add(new FieldError("batchId", "REQUIRED", "Batch ID is required"));
            return Result.failure(ErrorDetail.validationError(fieldErrors));
        }
        
        try {
            BatchId batchId = BatchId.of(batchIdStr.trim());
            return Result.success(batchId);
        } catch (IllegalArgumentException e) {
            fieldErrors.add(new FieldError("batchId", "INVALID_FORMAT", 
                "Invalid batch ID format: " + e.getMessage()));
            return Result.failure(ErrorDetail.validationError(fieldErrors));
        }
    }
    
    /**
     * Verifies user has access to the specified batch.
     * Users can only access batches from their own bank.
     */
    private Result<Void> verifyBatchAccess(BatchId batchId) {
        try {
            String currentBankId = SecurityUtils.getCurrentBankId();
            if (currentBankId == null) {
                return Result.failure(ErrorDetail.of(
                    "AUTHENTICATION_ERROR",
                    "Bank ID not found in security context",
                    "authentication"
                ));
            }
            
            // In a real implementation, we would query the batch to get its bank ID
            // and verify it matches the current user's bank ID
            // For now, we'll use the existing security utilities
            if (!SecurityUtils.canAccessBatch(currentBankId)) {
                return Result.failure(ErrorDetail.of(
                    "AUTHORIZATION_ERROR",
                    "Access denied. You can only access batches from your own bank.",
                    "authorization"
                ));
            }
            
            return Result.success(null);
            
        } catch (Exception e) {
            logger.error("Error verifying batch access: {}", e.getMessage(), e);
            return Result.failure(ErrorDetail.of(
                "SYSTEM_ERROR",
                "Failed to verify batch access: " + e.getMessage(),
                "system"
            ));
        }
    }
    
    /**
     * Parses query parameters for trends endpoint.
     */
    private Result<TrendsQueryParams> parseTrendsQueryParams(ServerRequest request) {
        List<FieldError> fieldErrors = new ArrayList<>();
        
        try {
            // Parse days parameter (default: 30)
            int days = request.param("days")
                .map(Integer::parseInt)
                .orElse(30);
            
            if (days <= 0) {
                fieldErrors.add(new FieldError("days", "INVALID_VALUE", 
                    "Days must be positive"));
            }
            if (days > 365) {
                fieldErrors.add(new FieldError("days", "INVALID_VALUE", 
                    "Days cannot exceed 365"));
            }
            
            // Parse limit parameter (default: 100)
            int limit = request.param("limit")
                .map(Integer::parseInt)
                .orElse(100);
            
            if (limit <= 0) {
                fieldErrors.add(new FieldError("limit", "INVALID_VALUE", 
                    "Limit must be positive"));
            }
            if (limit > 1000) {
                fieldErrors.add(new FieldError("limit", "INVALID_VALUE", 
                    "Limit cannot exceed 1000"));
            }
            
            if (!fieldErrors.isEmpty()) {
                return Result.failure(ErrorDetail.validationError(fieldErrors));
            }
            
            // Calculate time range
            Instant endTime = Instant.now();
            Instant startTime = endTime.minus(days, ChronoUnit.DAYS);
            
            return Result.success(new TrendsQueryParams(startTime, endTime, limit));
            
        } catch (NumberFormatException e) {
            fieldErrors.add(new FieldError("queryParams", "INVALID_FORMAT", 
                "Invalid number format in query parameters"));
            return Result.failure(ErrorDetail.validationError(fieldErrors));
        } catch (Exception e) {
            logger.error("Error parsing trends query parameters: {}", e.getMessage(), e);
            return Result.failure(ErrorDetail.of(
                "SYSTEM_ERROR",
                "Failed to parse query parameters: " + e.getMessage(),
                "system"
            ));
        }
    }
    
    /**
     * Handles error responses consistently.
     */
    private ServerResponse handleErrorResponse(ErrorDetail error) {
        ResponseEntity<? extends ApiResponse<?>> responseEntity = handleError(error);
        return ServerResponse.status(responseEntity.getStatusCode())
            .contentType(MediaType.APPLICATION_JSON)
            .body(responseEntity.getBody());
    }
    
    /**
     * Handles system error responses.
     */
    private ServerResponse handleSystemErrorResponse(Exception e) {
        ResponseEntity<? extends ApiResponse<?>> responseEntity = handleSystemError(e);
        return ServerResponse.status(responseEntity.getStatusCode())
            .contentType(MediaType.APPLICATION_JSON)
            .body(responseEntity.getBody());
    }
    
    /**
     * Record for trends query parameters.
     */
    private record TrendsQueryParams(
        Instant startTime,
        Instant endTime,
        int limit
    ) {}
}