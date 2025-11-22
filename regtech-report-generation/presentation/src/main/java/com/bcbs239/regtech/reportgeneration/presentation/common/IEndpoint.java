package com.bcbs239.regtech.reportgeneration.presentation.common;

import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * Interface for functional endpoints using Spring MVC RouterFunction.
 * All presentation layer controllers implement this interface to expose their routes.
 */
public interface IEndpoint {
    RouterFunction<ServerResponse> mapEndpoint();
}
