package com.bcbs239.regtech.reportgeneration.presentation.config;

import com.bcbs239.regtech.reportgeneration.presentation.configuration.ReportConfigurationRoutes;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * Web configuration for the report generation module.
 * Registers all functional endpoints and configures routing.
 * 
 * Requirements: 24.3, 24.4
 */
@Configuration
public class ReportGenerationWebConfig {
    
    /**
     * Registers all report generation module endpoints.
     */
    @Bean
    public RouterFunction<ServerResponse> reportGenerationRoutes(
        ReportConfigurationRoutes configurationRoutes
    ) {
        return configurationRoutes.createRoutes();
    }
}
