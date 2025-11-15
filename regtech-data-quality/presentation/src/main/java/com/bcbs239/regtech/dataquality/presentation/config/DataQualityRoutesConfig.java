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
        return endpoints.stream()
            .map(IEndpoint::mapEndpoint)
            .reduce(RouterFunction::and)
            .orElseGet(() -> RouterFunctions.route().build());
    }
}
