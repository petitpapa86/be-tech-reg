package com.bcbs239.regtech.riskcalculation.application.parameters;

import com.bcbs239.regtech.riskcalculation.domain.parameters.*;
import com.bcbs239.regtech.riskcalculation.domain.shared.Money;
import org.jspecify.annotations.NonNull;

import java.time.LocalDate;

/**
 * Command: Update Risk Parameters
 */
public record UpdateRiskParametersCommand(
    @NonNull String bankId,
    @NonNull LargeExposuresDto largeExposures,
    @NonNull CapitalBaseDto capitalBase,
    @NonNull ConcentrationRiskDto concentrationRisk,
    @NonNull String modifiedBy
) {
    
    public record LargeExposuresDto(
        double limitPercent,
        double classificationThresholdPercent,
        @NonNull MoneyDto eligibleCapital,
        @NonNull String regulatoryReference
    ) {
        @NonNull
        public LargeExposuresParameters toDomain() {
            Money eligibleCapitalMoney = new Money(
                eligibleCapital.amount(),
                eligibleCapital.currency()
            );
            
            Money absoluteLimitValue = Money.of(
                eligibleCapitalMoney.amount().multiply(java.math.BigDecimal.valueOf(limitPercent / 100.0)),
                eligibleCapitalMoney.currency()
            );
            
            Money absoluteClassificationValue = Money.of(
                eligibleCapitalMoney.amount().multiply(java.math.BigDecimal.valueOf(classificationThresholdPercent / 100.0)),
                eligibleCapitalMoney.currency()
            );
            
            return new LargeExposuresParameters(
                limitPercent,
                classificationThresholdPercent,
                eligibleCapitalMoney,
                absoluteLimitValue,
                absoluteClassificationValue,
                regulatoryReference
            );
        }
    }
    
    public record CapitalBaseDto(
        @NonNull MoneyDto eligibleCapital,
        @NonNull MoneyDto tier1Capital,
        @NonNull MoneyDto tier2Capital,
        @NonNull String calculationMethod,
        @NonNull LocalDate capitalReferenceDate,
        @NonNull String updateFrequency,
        @NonNull LocalDate nextUpdateDate
    ) {
        @NonNull
        public CapitalBaseParameters toDomain() {
            return new CapitalBaseParameters(
                new Money(eligibleCapital.amount(), eligibleCapital.currency()),
                new Money(tier1Capital.amount(), tier1Capital.currency()),
                new Money(tier2Capital.amount(), tier2Capital.currency()),
                CapitalBaseParameters.CalculationMethod.valueOf(calculationMethod),
                capitalReferenceDate,
                CapitalBaseParameters.UpdateFrequency.valueOf(updateFrequency),
                nextUpdateDate
            );
        }
    }
    
    public record ConcentrationRiskDto(
        double alertThresholdPercent,
        double attentionThresholdPercent,
        int maxLargeExposures
    ) {
        @NonNull
        public ConcentrationRiskParameters toDomain() {
            return new ConcentrationRiskParameters(
                alertThresholdPercent,
                attentionThresholdPercent,
                maxLargeExposures
            );
        }
    }
    
    public record MoneyDto(
        java.math.@NonNull BigDecimal amount,
        @NonNull String currency
    ) {}
}
