package com.bcbs239.regtech.riskcalculation.presentation.exceptions;

import com.bcbs239.regtech.core.presentation.apiresponses.ApiResponse;
import com.bcbs239.regtech.core.presentation.apiresponses.ResponseUtils;
import com.bcbs239.regtech.riskcalculation.presentation.mappers.MappingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for the risk calculation presentation layer.
 * Provides consistent error responses and logging for all risk calculation endpoints.
 * 
 * Requirements: 2.5, 7.1, 7.2, 7.3, 7.4, 7.5
 */
@ControllerAdvice(basePackages = "com.bcbs239.regtech.riskcalculation.presentation")
public class RiskCalculationErrorHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(RiskCalculationErrorHandler.class);
    
    private static final String ERROR_CODE_BATCH_NOT_FOUND = "BATCH_NOT_FOUND";
    private static final String ERROR_CODE_CALCULATION_NOT_COMPLETE = "CALCULATION_NOT_COMPLETE";
    private static final String ERROR_CODE_MAPPING_ERROR = "MAPPING_ERROR";
    private static final String ERROR_CODE_INVALID_REQUEST = "INVALID_REQUEST";
    private static final String ERROR_CODE_SYSTEM_ERROR = "SYSTEM_ERROR";
    
    /**
     * Handles BatchNotFoundException.
     * Returns HTTP 404 Not Found with error details.
     * 
     * @param ex the exception
     * @param request the web request
     * @return ResponseEntity with error response
     */
    @ExceptionHandler(BatchNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleBatchNotFoundException(
            BatchNotFoundException ex, 
            WebRequest request) {
        
        logger.warn("Batch not found: batchId={}, path={}", 
            ex.getBatchId(), 
            request.getDescription(false));
        
        Map<String, Object> meta = createErrorMeta(ERROR_CODE_BATCH_NOT_FOUND);
        meta.put("batchId", ex.getBatchId());
        
        ApiResponse<Void> response = ApiResponse.error()
                .message(ex.getMessage())
                .type(com.bcbs239.regtech.core.domain.shared.ErrorType.NOT_FOUND_ERROR)
                .meta(meta)
                .build();
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }
    
    /**
     * Handles CalculationNotCompletedException.
     * Returns HTTP 202 Accepted to indicate processing is still in progress.
     * 
     * @param ex the exception
     * @param request the web request
     * @return ResponseEntity with error response
     */
    @ExceptionHandler(CalculationNotCompletedException.class)
    public ResponseEntity<ApiResponse<Void>> handleCalculationNotCompletedException(
            CalculationNotCompletedException ex, 
            WebRequest request) {
        
        logger.info("Calculation not yet complete: batchId={}, state={}, path={}", 
            ex.getBatchId(), 
            ex.getCurrentState(),
            request.getDescription(false));
        
        Map<String, Object> meta = createErrorMeta(ERROR_CODE_CALCULATION_NOT_COMPLETE);
        meta.put("batchId", ex.getBatchId());
        meta.put("currentState", ex.getCurrentState().toString());
        meta.put("retryAfter", 30); // Suggest retry after 30 seconds
        
        ApiResponse<Void> response = ApiResponse.error()
                .message(ex.getMessage())
                .type(com.bcbs239.regtech.core.domain.shared.ErrorType.BUSINESS_RULE_ERROR)
                .meta(meta)
                .build();
        
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }
    
    /**
     * Handles MappingException.
     * Returns HTTP 500 Internal Server Error as mapping failures are system errors.
     * 
     * @param ex the exception
     * @param request the web request
     * @return ResponseEntity with error response
     */
    @ExceptionHandler(MappingException.class)
    public ResponseEntity<ApiResponse<Void>> handleMappingException(
            MappingException ex, 
            WebRequest request) {
        
        logger.error("Mapping error occurred: message={}, path={}", 
            ex.getMessage(), 
            request.getDescription(false), 
            ex);
        
        Map<String, Object> meta = createErrorMeta(ERROR_CODE_MAPPING_ERROR);
        
        ApiResponse<Void> response = ApiResponse.error()
                .message("An error occurred while processing the data")
                .type(com.bcbs239.regtech.core.domain.shared.ErrorType.SYSTEM_ERROR)
                .meta(meta)
                .build();
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
    
    /**
     * Handles IllegalArgumentException.
     * Returns HTTP 400 Bad Request for invalid input parameters.
     * 
     * @param ex the exception
     * @param request the web request
     * @return ResponseEntity with error response
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(
            IllegalArgumentException ex, 
            WebRequest request) {
        
        logger.warn("Invalid request parameter: message={}, path={}", 
            ex.getMessage(), 
            request.getDescription(false));
        
        Map<String, Object> meta = createErrorMeta(ERROR_CODE_INVALID_REQUEST);
        
        ApiResponse<Void> response = ApiResponse.error()
                .message(ex.getMessage())
                .type(com.bcbs239.regtech.core.domain.shared.ErrorType.VALIDATION_ERROR)
                .meta(meta)
                .build();
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    /**
     * Handles all other unexpected exceptions.
     * Returns HTTP 500 Internal Server Error with generic message.
     * Logs full exception details for debugging.
     * 
     * @param ex the exception
     * @param request the web request
     * @return ResponseEntity with error response
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(
            Exception ex, 
            WebRequest request) {
        
        logger.error("Unexpected error occurred: message={}, type={}, path={}", 
            ex.getMessage(), 
            ex.getClass().getSimpleName(),
            request.getDescription(false), 
            ex);
        
        Map<String, Object> meta = createErrorMeta(ERROR_CODE_SYSTEM_ERROR);
        
        ApiResponse<Void> response = ApiResponse.error()
                .message("An unexpected error occurred. Please try again later.")
                .type(com.bcbs239.regtech.core.domain.shared.ErrorType.SYSTEM_ERROR)
                .meta(meta)
                .build();
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
    
    /**
     * Creates error metadata with timestamp, version, and error code.
     * 
     * @param errorCode the error code
     * @return map containing error metadata
     */
    private Map<String, Object> createErrorMeta(String errorCode) {
        Map<String, Object> meta = new HashMap<>();
        meta.put("timestamp", Instant.now().toString());
        meta.put("version", "1.0");
        meta.put("apiVersion", "v1");
        meta.put("errorCode", errorCode);
        return meta;
    }
}
