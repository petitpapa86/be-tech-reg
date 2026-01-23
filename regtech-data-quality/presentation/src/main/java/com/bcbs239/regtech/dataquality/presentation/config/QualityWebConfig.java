package com.bcbs239.regtech.dataquality.presentation.config;

import com.bcbs239.regtech.dataquality.presentation.reports.QualityReportRoutes;
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
        DataQualityConfigRoutes dataQualityConfigRoutes,
        QualityReportRoutes qualityReportRoutes
    ) {
        return dataQualityConfigRoutes.createRoutes()
            .and(qualityReportRoutes.createRoutes());
    }
}

