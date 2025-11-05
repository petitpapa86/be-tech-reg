package com.bcbs239.regtech.ingestion.presentation.common;

import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * Interface for functional endpoints using Spring MVC RouterFunction.
 */
public interface IEndpoint {
    RouterFunction<ServerResponse> mapEndpoint();
}

