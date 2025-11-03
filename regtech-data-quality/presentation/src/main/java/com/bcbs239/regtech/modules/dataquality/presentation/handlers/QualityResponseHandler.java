package com.bcbs239.regtech.modules.dataquality.presentation.handlers;

import com.bcbs239.regtech.core.shared.BaseController;
import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.core.shared.ApiResponse;
import com.bcbs239.regtech.core.shared.ErrorDetail;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * Handler for converting application results to HTTP responses.
 * Provides consistent response formatting for quality report endpoints.
 */
@Component
public class QualityResponseHandler extends BaseController {
    
    /**
     * Converts a Result to a ServerResponse with success handling.
     */
    public <T> ServerResponse handleSuccessResult(Result<T> result, String successMessage, String messageKey) {
        ResponseEntity<? extends ApiResponse<?>> responseEntity = handleResult(
            result, 
            successMessage, 
            messageKey
        );
        
        return ServerResponse.status(responseEntity.getStatusCode())
            .contentType(MediaType.APPLICATION_JSON)
            .body(responseEntity.getBody());
    }
    
    /**
     * Converts an ErrorDetail to a ServerResponse.
     */
    public ServerResponse handleErrorResponse(ErrorDetail error) {
        ResponseEntity<? extends ApiResponse<?>> responseEntity = handleError(error);
        return ServerResponse.status(responseEntity.getStatusCode())
            .contentType(MediaType.APPLICATION_JSON)
            .body(responseEntity.getBody());
    }
    
    /**
     * Converts an Exception to a ServerResponse.
     */
    public ServerResponse handleSystemErrorResponse(Exception e) {
        ResponseEntity<? extends ApiResponse<?>> responseEntity = handleSystemError(e);
        return ServerResponse.status(responseEntity.getStatusCode())
            .contentType(MediaType.APPLICATION_JSON)
            .body(responseEntity.getBody());
    }
}