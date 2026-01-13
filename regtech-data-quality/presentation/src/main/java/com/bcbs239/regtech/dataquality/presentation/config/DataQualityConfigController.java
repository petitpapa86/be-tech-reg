package com.bcbs239.regtech.dataquality.presentation.config;

import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.dataquality.application.config.*;
import com.bcbs239.regtech.dataquality.domain.config.BankId;
import com.bcbs239.regtech.dataquality.presentation.common.IEndpoint;
import com.bcbs239.regtech.dataquality.presentation.web.QualityResponseHandler;
import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * Data Quality Configuration Controller using functional endpoints.
 * 
 * <p><b>CQRS Pattern - PHASE 3:</b> Controller now uses separate handlers for read/write:
 * <ul>
 *   <li>GetConfigurationQueryHandler - READ operations</li>
 *   <li>UpdateConfigurationCommandHandler - WRITE operations</li>
 *   <li>ResetConfigurationCommandHandler - WRITE operations</li>
 * </ul>
 * 
 * <p><b>Module:</b> regtech-data-quality/presentation
 * <p><b>Bounded Context:</b> Data Quality Configuration
 * <p><b>Architecture Layer:</b> Presentation (REST API boundary)
 */
@Component
public class DataQualityConfigController implements IEndpoint {

    private static final Logger logger = LoggerFactory.getLogger(DataQualityConfigController.class);

    private final GetConfigurationQueryHandler getConfigurationQueryHandler;
    private final UpdateConfigurationCommandHandler updateConfigurationCommandHandler;
    private final ResetConfigurationCommandHandler resetConfigurationCommandHandler;
    private final QualityResponseHandler responseHandler;

    public DataQualityConfigController(
        GetConfigurationQueryHandler getConfigurationQueryHandler,
        UpdateConfigurationCommandHandler updateConfigurationCommandHandler,
        ResetConfigurationCommandHandler resetConfigurationCommandHandler,
        QualityResponseHandler responseHandler
    ) {
        this.getConfigurationQueryHandler = getConfigurationQueryHandler;
        this.updateConfigurationCommandHandler = updateConfigurationCommandHandler;
        this.resetConfigurationCommandHandler = resetConfigurationCommandHandler;
        this.responseHandler = responseHandler;
    }

    /**
     * Maps the configuration endpoints.
     * Note: This method is implemented for the IEndpoint interface but routing is 
     * handled by DataQualityConfigRoutes to avoid circular dependencies.
     */
    @Override
    public RouterFunction<ServerResponse> mapEndpoints() {
        // This is handled by DataQualityConfigRoutes to avoid circular dependency
        throw new UnsupportedOperationException(
            "Endpoint mapping is handled by DataQualityConfigRoutes component"
        );
    }

    /**
     * GET /api/v1/data-quality/config?bankId=xxx
     * 
     * <p>Retrieves current data quality configuration.
     * 
     * <p><b>CQRS Pattern:</b> Uses GetConfigurationQueryHandler for READ operation.
     * 
     * @param request ServerRequest containing bankId query parameter
     * @return ServerResponse with configuration object
     */
    @Observed(name = "data-quality.config.get", contextualName = "Get Data Quality Configuration")
    public ServerResponse getConfiguration(ServerRequest request) {
        logger.info("Fetching data quality configuration");

        String bankIdStr = request.param("bankId")
            .orElse("default-bank");
        
        Result<BankId> bankIdResult = BankId.of(bankIdStr);
        if (bankIdResult.isFailure()) {
            return responseHandler.handleErrorResponse(bankIdResult.getError().orElseThrow());
        }
        
        GetConfigurationQuery query = new GetConfigurationQuery(bankIdResult.getValueOrThrow());
        Result<ConfigurationDto> result = getConfigurationQueryHandler.handle(query);
        
        return responseHandler.handleSuccessResult(result, "Configuration retrieved successfully", "config.retrieved");
    }

    /**
     * PUT /api/v1/data-quality/config?bankId=xxx
     * 
     * <p>Updates data quality configuration with new thresholds and rules.
     * 
     * <p><b>CQRS Pattern:</b> Uses UpdateConfigurationCommandHandler for WRITE operation.
     * 
     * @param request ServerRequest containing bankId and configuration payload
     * @return ServerResponse with updated configuration
     */
    @Observed(name = "data-quality.config.update", contextualName = "Update Data Quality Configuration")
    public ServerResponse updateConfiguration(ServerRequest request) {
        logger.info("Updating data quality configuration");

        try {
            String bankIdStr = request.param("bankId")
                .orElse("default-bank");
            
            Result<BankId> bankIdResult = BankId.of(bankIdStr);
            if (bankIdResult.isFailure()) {
                return responseHandler.handleErrorResponse(bankIdResult.getError().orElseThrow());
            }
            
            ConfigurationDto config = request.body(ConfigurationDto.class);
            
            UpdateConfigurationCommand command = new UpdateConfigurationCommand(bankIdResult.getValueOrThrow(), config);
            Result<ConfigurationDto> result = updateConfigurationCommandHandler.handle(command);
            
            return responseHandler.handleSuccessResult(result, "Configuration updated successfully", "config.updated");
                
        } catch (Exception e) {
            logger.error("Failed to update configuration", e);
            return responseHandler.handleSystemErrorResponse(e);
        }
    }

    /**
     * POST /api/v1/data-quality/config/reset?bankId=xxx
     * 
     * <p>Resets configuration to system defaults.
     * 
     * <p><b>CQRS Pattern:</b> Uses ResetConfigurationCommandHandler for WRITE operation.
     * 
     * @param request ServerRequest containing bankId query parameter
     * @return ServerResponse with default configuration
     */
    @Observed(name = "data-quality.config.reset", contextualName = "Reset Data Quality Configuration")
    public ServerResponse resetToDefault(ServerRequest request) {
        logger.info("Resetting data quality configuration to defaults");
        
        String bankIdStr = request.param("bankId")
            .orElse("default-bank");
        
        Result<BankId> bankIdResult = BankId.of(bankIdStr);
        if (bankIdResult.isFailure()) {
            return responseHandler.handleErrorResponse(bankIdResult.getError().orElseThrow());
        }
        
        ResetConfigurationCommand command = new ResetConfigurationCommand(bankIdResult.getValueOrThrow());
        Result<ConfigurationDto> result = resetConfigurationCommandHandler.handle(command);
        
        return responseHandler.handleSuccessResult(result, "Configuration reset to defaults", "config.reset");
    }
}
