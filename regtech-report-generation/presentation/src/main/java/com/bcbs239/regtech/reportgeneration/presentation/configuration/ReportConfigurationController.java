package com.bcbs239.regtech.reportgeneration.presentation.configuration;

import com.bcbs239.regtech.reportgeneration.application.configuration.*;
import com.bcbs239.regtech.reportgeneration.domain.configuration.ReportConfiguration;
import com.bcbs239.regtech.core.domain.shared.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springframework.web.servlet.function.RouterFunctions.route;
import static org.springframework.web.servlet.function.RequestPredicates.*;

/**
 * Report Configuration Routes (Route Functions)
 * 
 * Capability: Report Configuration
 */
@Configuration
@RequiredArgsConstructor
public class ReportConfigurationController {
    
    private final GetReportConfigurationHandler getReportConfigurationHandler;
    private final UpdateReportConfigurationHandler updateReportConfigurationHandler;
    
    @Bean
    public RouterFunction<ServerResponse> reportConfigurationRoutes() {
        return route()
                .GET("/api/v1/banks/{bankId}/configuration/report", accept(MediaType.APPLICATION_JSON), 
                    this::getReportConfiguration)
                .PUT("/api/v1/banks/{bankId}/configuration/report", accept(MediaType.APPLICATION_JSON), 
                    this::updateReportConfiguration)
                .build();
    }
    
    /**
     * GET /api/v1/banks/{bankId}/configuration/report
     */
    private ServerResponse getReportConfiguration(
            org.springframework.web.servlet.function.ServerRequest request) {
        
        Long bankId = Long.valueOf(request.pathVariable("bankId"));
        
        return getReportConfigurationHandler.handle(bankId)
                .map(ReportConfigurationResponse::from)
                .map(response -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(response))
                .orElseGet(() -> ServerResponse.notFound().build());
    }
    
    /**
     * PUT /api/v1/banks/{bankId}/configuration/report
     */
    private ServerResponse updateReportConfiguration(
            org.springframework.web.servlet.function.ServerRequest request) {
        
        try {
            Long bankId = Long.valueOf(request.pathVariable("bankId"));
            ReportConfigurationRequest requestDto = request.body(ReportConfigurationRequest.class);
            
            // Validate bankId matches path parameter
            if (!bankId.equals(requestDto.bankId())) {
                return ServerResponse.badRequest()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new ErrorResponse("Bank ID in path does not match request body"));
            }
            
            // TODO: Get actual user from security context
            String modifiedBy = "admin";
            
            UpdateReportConfigurationHandler.UpdateCommand command = requestDto.toCommand(modifiedBy);
            
            Result<ReportConfiguration> result = updateReportConfigurationHandler.handle(command);
            
            if (result.isSuccess()) {
                ReportConfigurationResponse response = ReportConfigurationResponse.from(result.getValueOrThrow());
                return ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(response);
            } else {
                String errorMessage = result.getError()
                        .map(error -> error.getMessage())
                        .orElse("Unknown error occurred");
                return ServerResponse.badRequest()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new ErrorResponse(errorMessage));
            }
            
        } catch (Exception e) {
            return ServerResponse.badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new ErrorResponse("Invalid request: " + e.getMessage()));
        }
    }
    
    private record ErrorResponse(String error) {}
}
