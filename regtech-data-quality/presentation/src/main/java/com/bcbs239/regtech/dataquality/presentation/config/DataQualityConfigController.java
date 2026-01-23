package com.bcbs239.regtech.dataquality.presentation.config;

import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.core.presentation.controllers.BaseController;
import com.bcbs239.regtech.dataquality.application.config.*;
import com.bcbs239.regtech.dataquality.domain.config.BankId;
import com.bcbs239.regtech.dataquality.presentation.common.IEndpoint;
import jakarta.servlet.ServletException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.io.IOException;

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
 * <p><b>Architecture:</b> Extends BaseController for consistent response handling
 * across both traditional REST and functional endpoints.
 *
 * <p><b>Module:</b> regtech-data-quality/presentation
 * <p><b>Bounded Context:</b> Data Quality Configuration
 * <p><b>Architecture Layer:</b> Presentation (REST API boundary)
 */
@Component
public class DataQualityConfigController extends BaseController implements IEndpoint {

    private static final Logger logger = LoggerFactory.getLogger(DataQualityConfigController.class);

    private final GetConfigurationQueryHandler getConfigurationQueryHandler;
    private final UpdateConfigurationCommandHandler updateConfigurationCommandHandler;
    private final ResetConfigurationCommandHandler resetConfigurationCommandHandler;

    public DataQualityConfigController(
            GetConfigurationQueryHandler getConfigurationQueryHandler,
            UpdateConfigurationCommandHandler updateConfigurationCommandHandler,
            ResetConfigurationCommandHandler resetConfigurationCommandHandler
    ) {
        this.getConfigurationQueryHandler = getConfigurationQueryHandler;
        this.updateConfigurationCommandHandler = updateConfigurationCommandHandler;
        this.resetConfigurationCommandHandler = resetConfigurationCommandHandler;
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
    public ServerResponse getConfiguration(ServerRequest request) {
        logger.info("Fetching data quality configuration");

        String bankIdStr = request.param("bankId")
                .orElse("default-bank");

        Result<BankId> bankIdResult = BankId.of(bankIdStr);
        if (bankIdResult.isFailure()) {
            return handleErrorResponse(bankIdResult.getError().orElseThrow());
        }

        GetConfigurationQuery query = new GetConfigurationQuery(bankIdResult.getValueOrThrow());
        Result<ConfigurationDto> result = getConfigurationQueryHandler.handle(query);

        return handleSuccessResult(result, "Configuration retrieved successfully", "config.retrieved");
    }

    /**
     * PUT /api/v1/data-quality/config
     *
     * <p>Updates data quality configuration with new thresholds and rules.
     * <p><b>Security:</b> BankId is included in request body to avoid exposure in URL parameters.
     *
     * <p><b>CQRS Pattern:</b> Uses UpdateConfigurationCommandHandler for WRITE operation.
     *
     * @param request ServerRequest containing configuration payload with bankId
     * @return ServerResponse with updated configuration
     */
    public ServerResponse updateConfiguration(ServerRequest request) throws ServletException, IOException {
        logger.info("Updating data quality configuration");

        ConfigurationDto config = request.body(ConfigurationDto.class);

        // Validate bankId from request body using value object
        Result<BankId> bankIdResult = BankId.of(config.bankId());
        if (bankIdResult.isFailure()) {
            return handleErrorResponse(bankIdResult.getError().orElseThrow());
        }

        UpdateConfigurationCommand command = new UpdateConfigurationCommand(bankIdResult.getValueOrThrow(), config);
        Result<ConfigurationDto> result = updateConfigurationCommandHandler.handle(command);

        return handleSuccessResult(result, "Configuration updated successfully", "config.updated");

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
    public ServerResponse resetToDefault(ServerRequest request) {
        logger.info("Resetting data quality configuration to defaults");

        String bankIdStr = request.param("bankId")
                .orElse("default-bank");

        Result<BankId> bankIdResult = BankId.of(bankIdStr);
        if (bankIdResult.isFailure()) {
            return handleErrorResponse(bankIdResult.getError().orElseThrow());
        }

        ResetConfigurationCommand command = new ResetConfigurationCommand(bankIdResult.getValueOrThrow());
        Result<ConfigurationDto> result = resetConfigurationCommandHandler.handle(command);

        return handleSuccessResult(result, "Configuration reset to defaults", "config.reset");
    }
}

