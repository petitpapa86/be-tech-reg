package com.bcbs239.regtech.app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerResponse;

import java.util.List;

/**
 * Configuration for registering all functional router endpoints.
 * Automatically collects all RouterFunction beans and combines them into a single router.
 * This approach is module-agnostic and doesn't require dependencies on specific modules.
 */
@Configuration
public class RouterConfig {

    /**
     * Combines all RouterFunction beans into a single router.
     * Spring will automatically inject all RouterFunction<ServerResponse> beans
     * defined across all modules, including those returned by IEndpoint.mapEndpoint().
     */
    @Bean
    public RouterFunction<ServerResponse> combinedRoutes(
            List<RouterFunction<ServerResponse>> allRoutes) {
        if (allRoutes.isEmpty()) {
            return RouterFunctions.route().build();
        }
        
        return allRoutes.stream()
            .reduce(RouterFunction::and)
            .orElseGet(() -> RouterFunctions.route().build());
    }
}
