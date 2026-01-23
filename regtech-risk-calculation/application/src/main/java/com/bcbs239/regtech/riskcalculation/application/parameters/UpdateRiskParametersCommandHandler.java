package com.bcbs239.regtech.riskcalculation.application.parameters;

import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.riskcalculation.domain.parameters.*;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Handler for UpdateRiskParametersCommand
 */
@Component
public class UpdateRiskParametersCommandHandler {

    private final RiskParametersRepository repository;

    public UpdateRiskParametersCommandHandler(RiskParametersRepository repository) {
        this.repository = repository;
    }

    @NonNull
    public Result<RiskParametersDto> handle(@NonNull UpdateRiskParametersCommand command) {
        Optional<RiskParameters> existingParams = repository.findByBankId(command.bankId());
        
        RiskParameters parameters;
        if (existingParams.isPresent()) {
            parameters = existingParams.get();
            parameters.updateLargeExposuresParameters(command.largeExposures().toDomain(), command.modifiedBy());
            parameters.updateCapitalBase(command.capitalBase().toDomain(), command.modifiedBy());
            parameters.updateConcentrationRisk(command.concentrationRisk().toDomain(), command.modifiedBy());
        } else {
            parameters = RiskParameters.reconstitute(
                RiskParametersId.generate(),
                command.bankId(),
                command.largeExposures().toDomain(),
                command.capitalBase().toDomain(),
                command.concentrationRisk().toDomain(),
                ValidationStatus.createValid(),
                java.time.Instant.now(),
                java.time.Instant.now(),
                command.modifiedBy(),
                0L
            );
        }
        
        parameters.validate();
        repository.save(parameters);
        
        return Result.success(RiskParametersDto.from(parameters));
    }
}
