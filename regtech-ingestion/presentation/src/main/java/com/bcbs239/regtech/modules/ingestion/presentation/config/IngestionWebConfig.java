package com.bcbs239.regtech.modules.ingestion.presentation.config;

import com.bcbs239.regtech.modules.ingestion.presentation.batch.status.BatchStatusController;
import com.bcbs239.regtech.modules.ingestion.presentation.batch.process.ProcessBatchController;
import com.bcbs239.regtech.modules.ingestion.presentation.batch.upload.UploadFileController;
import com.bcbs239.regtech.modules.ingestion.presentation.compliance.reports.ComplianceReportsController;
import com.bcbs239.regtech.modules.ingestion.presentation.compliance.policies.RetentionPoliciesController;
import com.bcbs239.regtech.modules.ingestion.presentation.compliance.lifecycle.LifecyclePoliciesController;
import com.bcbs239.regtech.modules.ingestion.presentation.health.IngestionHealthController;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * Web configuration for ingestion module endpoints.
 * Registers all functional RouterFunction endpoints organized by entity and operation.
 */
@Configuration
public class IngestionWebConfig {
    
    @Bean
    public RouterFunction<ServerResponse> ingestionRoutes(
            UploadFileController uploadFileController,
            ProcessBatchController processBatchController,
            BatchStatusController batchStatusController,
            ComplianceReportsController complianceReportsController,
            RetentionPoliciesController retentionPoliciesController,
            LifecyclePoliciesController lifecyclePoliciesController,
            IngestionHealthController healthController) {
        
        return uploadFileController.mapEndpoint()
            .and(processBatchController.mapEndpoint())
            .and(batchStatusController.mapEndpoint())
            .and(complianceReportsController.mapEndpoint())
            .and(retentionPoliciesController.mapEndpoint())
            .and(lifecyclePoliciesController.mapEndpoint())
            .and(healthController.mapEndpoint());
    }
}