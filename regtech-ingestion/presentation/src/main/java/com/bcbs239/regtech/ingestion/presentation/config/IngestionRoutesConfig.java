package com.bcbs239.regtech.ingestion.presentation.config;

import com.bcbs239.regtech.ingestion.presentation.batch.upload.UploadAndProcessFileRoutes;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * Web configuration for the ingestion module.
 * Registers all functional endpoints and configures routing.
 */
@Configuration
public class IngestionRoutesConfig {

    /**
     * Registers all ingestion module endpoints.
     */
    @Bean
    public RouterFunction<ServerResponse> ingestionRoutes(
        UploadAndProcessFileRoutes uploadAndProcessFileRoutes
    ) {
        return uploadAndProcessFileRoutes.createRoutes();
    }
}
