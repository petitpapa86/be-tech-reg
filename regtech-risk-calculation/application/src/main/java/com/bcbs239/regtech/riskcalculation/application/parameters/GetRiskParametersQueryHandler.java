package com.bcbs239.regtech.riskcalculation.application.parameters;

import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.riskcalculation.domain.parameters.RiskParameters;
import com.bcbs239.regtech.riskcalculation.domain.parameters.RiskParametersRepository;
import io.micrometer.observation.annotation.Observed;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Handler for GetRiskParametersQuery
 */
@Component
public class GetRiskParametersQueryHandler {

    private final RiskParametersRepository repository;

    public GetRiskParametersQueryHandler(RiskParametersRepository repository) {
        this.repository = repository;
    }

    @NonNull
    @Observed(name = "get-risk-parameters", contextualName = "Get Risk Parameters")
    public Result<RiskParametersDto> handle(@NonNull GetRiskParametersQuery query) {
        Optional<RiskParameters> parameters = repository.findByBankId(query.bankId());
        
        return parameters
            .map(p -> Result.success(RiskParametersDto.from(p)))
            .orElseGet(() -> Result.success(createDefault(query.bankId())));
    }

    private RiskParametersDto createDefault(String bankId) {
        RiskParameters defaultParams = RiskParameters.createDefault(bankId, "SYSTEM");
        return RiskParametersDto.from(defaultParams);
    }
}
