package com.bcbs239.regtech.ingestion.web.config;

import com.bcbs239.regtech.ingestion.web.controller.BatchStatusController;
import com.bcbs239.regtech.ingestion.web.controller.ProcessBatchController;
import com.bcbs239.regtech.ingestion.web.controller.UploadFileController;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * Web configuration for ingestion module endpoints.
 * Registers all functional RouterFunction endpoints.
 */
@Configuration
public class IngestionWebConfig {
    
    @Bean
    public RouterFunction<ServerResponse> ingestionRoutes(
            UploadFileController uploadFileController,
            ProcessBatchController processBatchController,
            BatchStatusController batchStatusController) {
        
        return uploadFileController.mapEndpoint()
            .and(processBatchController.mapEndpoint())
            .and(batchStatusController.mapEndpoint());
    }
}