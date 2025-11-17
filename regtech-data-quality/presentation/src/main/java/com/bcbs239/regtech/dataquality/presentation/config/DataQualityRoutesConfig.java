package com.bcbs239.regtech.dataquality.presentation.config;

import com.bcbs239.regtech.dataquality.presentation.common.IEndpoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerResponse;

import java.util.List;

/**
 * Configuration that registers all data quality functional endpoints.
 * Collects all IEndpoint implementations and exposes their routes as a single RouterFunction bean.
 */
@Configuration
public class DataQualityRoutesConfig {

    @Bean
    public RouterFunction<ServerResponse> dataQualityRoutes(List<IEndpoint> endpoints) {
        // If no endpoints, return a router with at least one dummy route to avoid build() error
        if (endpoints == null || endpoints.isEmpty()) {
            return RouterFunctions.route()
                .GET("/api/data-quality/health", request -> 
                    ServerResponse.ok().body("Data Quality module - no endpoints configured"))
                .build();
        }
        
        return endpoints.stream()
            .map(IEndpoint::mapEndpoints)
            .reduce(RouterFunction::and)
            .orElseGet(() -> RouterFunctions.route()
                .GET("/api/data-quality/health", request -> 
                    ServerResponse.ok().body("Data Quality module ready"))
                .build());
    }
}
