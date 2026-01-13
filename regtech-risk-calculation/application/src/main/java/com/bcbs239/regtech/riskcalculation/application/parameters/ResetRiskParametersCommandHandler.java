package com.bcbs239.regtech.riskcalculation.application.parameters;

import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.riskcalculation.domain.parameters.RiskParameters;
import com.bcbs239.regtech.riskcalculation.domain.parameters.RiskParametersRepository;
import io.micrometer.observation.annotation.Observed;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Handler for ResetRiskParametersCommand
 */
@Component
public class ResetRiskParametersCommandHandler {

    private final RiskParametersRepository repository;

    public ResetRiskParametersCommandHandler(RiskParametersRepository repository) {
        this.repository = repository;
    }

    @NonNull
    @Observed(name = "reset-risk-parameters", contextualName = "Reset Risk Parameters")
    public Result<RiskParametersDto> handle(@NonNull ResetRiskParametersCommand command) {
        Optional<RiskParameters> existingParams = repository.findByBankId(command.bankId());
        
        RiskParameters parameters;
        if (existingParams.isPresent()) {
            parameters = existingParams.get();
            parameters.resetToDefault(command.modifiedBy());
        } else {
            parameters = RiskParameters.createDefault(command.bankId(), command.modifiedBy());
        }
        
        repository.save(parameters);
        
        return Result.success(RiskParametersDto.from(parameters));
    }
}
