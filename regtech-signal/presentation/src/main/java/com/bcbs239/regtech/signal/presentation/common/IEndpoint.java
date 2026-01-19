package com.bcbs239.regtech.signal.presentation.common;

import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * Simple interface for functional endpoints in the signal module.
 */
public interface IEndpoint {
    RouterFunction<ServerResponse> mapEndpoints();
}
