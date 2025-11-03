package com.bcbs239.regtech.modules.dataquality.presentation.config;

import com.bcbs239.regtech.modules.dataquality.presentation.controllers.QualityReportController;
import com.bcbs239.regtech.modules.dataquality.presentation.controllers.QualityHealthController;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * Web configuration for the data quality module.
 * Registers all functional endpoints and configures routing.
 */
@Configuration
public class QualityWebConfig {
    
    /**
     * Registers all data quality module endpoints.
     */
    @Bean
    public RouterFunction<ServerResponse> qualityRoutes(
        QualityReportController qualityReportController,
        QualityHealthController qualityHealthController
    ) {
        return qualityReportController.mapEndpoints()
            .and(qualityHealthController.mapEndpoints());
    }
}