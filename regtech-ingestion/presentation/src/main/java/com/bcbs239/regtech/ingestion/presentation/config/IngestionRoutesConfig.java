package com.bcbs239.regtech.ingestion.presentation.config;

import com.bcbs239.regtech.ingestion.presentation.common.IEndpoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerResponse;

import java.util.List;

/**
 * Configuration that registers all ingestion functional endpoints.
 * Collects all IEndpoint implementations and exposes their routes as a single RouterFunction bean.
 */
@Configuration
public class IngestionRoutesConfig {

    @Bean
    public RouterFunction<ServerResponse> ingestionRoutes(List<IEndpoint> endpoints) {
        // If no endpoints, return a router with at least one dummy route to avoid build() error
        if (endpoints == null || endpoints.isEmpty()) {
            return RouterFunctions.route()
                .GET("/api/ingestion/health", request -> 
                    ServerResponse.ok().body("Ingestion module - no endpoints configured"))
                .build();
        }
        
        return endpoints.stream()
            .map(IEndpoint::mapEndpoint)
            .reduce(RouterFunction::and)
            .orElseGet(() -> RouterFunctions.route()
                .GET("/api/ingestion/health", request -> 
                    ServerResponse.ok().body("Ingestion module ready"))
                .build());
    }
}
