package com.bcbs239.regtech.reportgeneration.presentation.configuration;

import com.bcbs239.regtech.reportgeneration.application.configuration.GetReportConfigurationHandler;
import com.bcbs239.regtech.reportgeneration.application.configuration.UpdateReportConfigurationHandler;
import com.bcbs239.regtech.reportgeneration.domain.configuration.ReportConfiguration;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.reportgeneration.presentation.common.IEndpoint;
import com.bcbs239.regtech.reportgeneration.presentation.web.ReportResponseHandler;
import io.micrometer.observation.annotation.Observed;
import jakarta.servlet.ServletException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.io.IOException;

/**
 * Report Configuration Controller
 * 
 * Capability: Report Configuration
 */
@Component
@RequiredArgsConstructor
public class ReportConfigurationController extends ReportResponseHandler implements IEndpoint {

        private final GetReportConfigurationHandler getReportConfigurationHandler;
        private final UpdateReportConfigurationHandler updateReportConfigurationHandler;
    
    @Override
    public RouterFunction<ServerResponse> mapEndpoint() {
        // Mapping is handled by ReportConfigurationRoutes to avoid circular dependencies
        throw new UnsupportedOperationException("Endpoint mapping is handled by ReportConfigurationRoutes component");
    }

    /**
     * GET /api/reporting
     * Returns full read model (configuration + derived status)
     */
    @Observed(name = "report-generation.api.configuration.get", contextualName = "get-report-configuration")
    public ServerResponse getReportingConfiguration(ServerRequest request) {
        // TODO: Get actual bankId from security context
        Long bankId = 1L;
        
        return getReportConfigurationHandler.handle(bankId)
                .map(ReportConfigurationReadModelResponse::from)
                .map(data -> handleSuccessResult(Result.success(data), "Success", "report.configuration.retrieved"))
                .orElseGet(() -> handleErrorResponse(
                        com.bcbs239.regtech.core.domain.shared.ErrorDetail.of(
                                "CONFIGURATION_NOT_FOUND",
                                com.bcbs239.regtech.core.domain.shared.ErrorType.NOT_FOUND_ERROR,
                                "Report configuration not found for bank",
                                "report.configuration.not_found"
                        )
                ));
    }
    
    /**
     * PUT /api/reporting
     * Accepts only configuration input
     * Returns full read model (same shape as GET)
     */
    @Observed(name = "report-generation.api.configuration.update", contextualName = "update-report-configuration")
    public ServerResponse updateReportingConfiguration(ServerRequest request) throws ServletException, IOException {
        ReportConfigurationCommandRequest requestDto = request.body(ReportConfigurationCommandRequest.class);
        
        // TODO: Get actual bankId and user from security context
        Long bankId = 1L;
        String modifiedBy = "admin";
        
        UpdateReportConfigurationHandler.UpdateCommand command = requestDto.toCommand(bankId, modifiedBy);
        
        Result<ReportConfiguration> result = updateReportConfigurationHandler.handle(command);

        return handleSuccessResult(
                result.map(ReportConfigurationReadModelResponse::from),
                "Configuration updated successfully",
                "report.configuration.updated"
        );
    }

    /**
     * POST /api/reporting/reset
     * Resets to default configuration
     */
    @Observed(name = "report-generation.api.configuration.reset", contextualName = "reset-report-configuration")
    public ServerResponse resetToDefault(ServerRequest request) {
        // TODO: Implement actual reset logic in application layer
        // For now, returning current as per mock behavior
        Long bankId = 1L;
        return getReportConfigurationHandler.handle(bankId)
                .map(ReportConfigurationReadModelResponse::from)
                .map(data -> handleSuccessResult(Result.success(data), "Configuration reset successfully", "report.configuration.reset"))
                .orElseGet(() -> handleErrorResponse(
                        com.bcbs239.regtech.core.domain.shared.ErrorDetail.of(
                                "CONFIGURATION_NOT_FOUND",
                                com.bcbs239.regtech.core.domain.shared.ErrorType.NOT_FOUND_ERROR,
                                "Report configuration not found for bank",
                                "report.configuration.not_found"
                        )
                ));
    }
}
