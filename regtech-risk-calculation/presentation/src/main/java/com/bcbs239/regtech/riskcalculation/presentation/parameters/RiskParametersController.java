package com.bcbs239.regtech.riskcalculation.presentation.parameters;

import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.riskcalculation.application.parameters.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/risk-parameters")
@CrossOrigin
@RequiredArgsConstructor
public class RiskParametersController {

    private final GetRiskParametersQueryHandler getHandler;
    private final UpdateRiskParametersCommandHandler updateHandler;
    private final ResetRiskParametersCommandHandler resetHandler;

    @GetMapping
    public ResponseEntity<RiskParametersDto> getRiskParameters(@RequestParam(defaultValue = "DEFAULT_BANK") String bankId) {
        Result<RiskParametersDto> result = getHandler.handle(new GetRiskParametersQuery(bankId));
        return ResponseEntity.ok(result.value());
    }

    @PutMapping
    public ResponseEntity<RiskParametersDto> updateRiskParameters(
            @RequestParam(defaultValue = "DEFAULT_BANK") String bankId,
            @RequestBody UpdateRiskParametersRequest request) {
        
        UpdateRiskParametersCommand command = new UpdateRiskParametersCommand(
            bankId,
            new UpdateRiskParametersCommand.LargeExposuresDto(
                request.largeExposures().limitPercent(),
                request.largeExposures().classificationThresholdPercent(),
                new UpdateRiskParametersCommand.MoneyDto(
                    java.math.BigDecimal.valueOf(request.largeExposures().eligibleCapital()),
                    "EUR"
                ),
                request.largeExposures().regulatoryReference()
            ),
            new UpdateRiskParametersCommand.CapitalBaseDto(
                new UpdateRiskParametersCommand.MoneyDto(
                    java.math.BigDecimal.valueOf(request.capitalBase().eligibleCapital()),
                    "EUR"
                ),
                new UpdateRiskParametersCommand.MoneyDto(
                    java.math.BigDecimal.valueOf(request.capitalBase().tier1Capital()),
                    "EUR"
                ),
                new UpdateRiskParametersCommand.MoneyDto(
                    java.math.BigDecimal.valueOf(request.capitalBase().tier2Capital()),
                    "EUR"
                ),
                request.capitalBase().calculationMethod(),
                request.capitalBase().capitalReferenceDate(),
                request.capitalBase().updateFrequency(),
                request.capitalBase().nextUpdateDate()
            ),
            new UpdateRiskParametersCommand.ConcentrationRiskDto(
                request.concentrationRisk().alertThresholdPercent(),
                request.concentrationRisk().attentionThresholdPercent(),
                request.concentrationRisk().maxLargeExposures()
            ),
            "Marco Rossi" // In a real app, this would come from SecurityContext
        );

        Result<RiskParametersDto> result = updateHandler.handle(command);
        return ResponseEntity.ok(result.value());
    }

    @PostMapping("/reset")
    public ResponseEntity<RiskParametersDto> resetToDefault(@RequestParam(defaultValue = "DEFAULT_BANK") String bankId) {
        Result<RiskParametersDto> result = resetHandler.handle(new ResetRiskParametersCommand(bankId, "Marco Rossi"));
        return ResponseEntity.ok(result.value());
    }

    public record UpdateRiskParametersRequest(
        LargeExposuresRequest largeExposures,
        CapitalBaseRequest capitalBase,
        ConcentrationRiskRequest concentrationRisk
    ) {}

    public record LargeExposuresRequest(
        double limitPercent,
        double classificationThresholdPercent,
        long eligibleCapital,
        String regulatoryReference
    ) {}

    public record CapitalBaseRequest(
        long eligibleCapital,
        long tier1Capital,
        long tier2Capital,
        String calculationMethod,
        java.time.LocalDate capitalReferenceDate,
        String updateFrequency,
        java.time.LocalDate nextUpdateDate
    ) {}

    public record ConcentrationRiskRequest(
        double alertThresholdPercent,
        double attentionThresholdPercent,
        int maxLargeExposures
    ) {}
}
