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
    @Bean(name = "routerFunction")
    public RouterFunction<ServerResponse> combinedRoutes(
            List<RouterFunction<ServerResponse>> allRoutes) {
        // Filter out null routes and check if any remain
        List<RouterFunction<ServerResponse>> validRoutes = allRoutes == null ? 
            List.of() : 
            allRoutes.stream()
                .filter(route -> route != null)
                .toList();
        
        // Return a simple GET endpoint if no routes are available
        // This prevents the "No routes registered" error
        if (validRoutes.isEmpty()) {
            return RouterFunctions.route()
                .GET("/", request -> ServerResponse.ok().body("RegTech Application - No routes configured"))
                .build();
        }
        
        return validRoutes.stream()
            .reduce(RouterFunction::and)
            .orElseGet(() -> RouterFunctions.route()
                .GET("/", request -> ServerResponse.ok().body("RegTech Application"))
                .build());
    }
}
