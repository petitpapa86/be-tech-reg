package com.bcbs239.regtech.dataquality.presentation.common;

import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * Interface for functional endpoints in the data quality module.
 * Provides a consistent contract for endpoint mapping.
 */
public interface IEndpoint {
    
    /**
     * Maps the endpoint to a RouterFunction.
     * 
     * @return RouterFunction that defines the endpoint routing and handling
     */
    RouterFunction<ServerResponse> mapEndpoints();
}