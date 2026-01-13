package com.bcbs239.regtech.riskcalculation.application.parameters;

import com.bcbs239.regtech.riskcalculation.domain.parameters.*;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.time.LocalDate;

/**
 * DTO: Risk Parameters Response
 */
public record RiskParametersDto(
    @NonNull String id,
    @NonNull String bankId,
    @NonNull LargeExposuresDto largeExposures,
    @NonNull CapitalBaseDto capitalBase,
    @NonNull ConcentrationRiskDto concentrationRisk,
    @NonNull ValidationStatusDto validationStatus,
    @NonNull StatusDto status
) {
    
    @NonNull
    public static RiskParametersDto from(@NonNull RiskParameters parameters) {
        return new RiskParametersDto(
            parameters.getId().value(),
            parameters.getBankId(),
            LargeExposuresDto.from(parameters.getLargeExposures()),
            CapitalBaseDto.from(parameters.getCapitalBase()),
            ConcentrationRiskDto.from(parameters.getConcentrationRisk()),
            ValidationStatusDto.from(parameters.getValidationStatus()),
            StatusDto.from(parameters)
        );
    }
    
    public record LargeExposuresDto(
        double limitPercent,
        double classificationThresholdPercent,
        long eligibleCapital,
        long absoluteLimitValue,
        long absoluteClassificationValue,
        @NonNull String regulatoryReference
    ) {
        @NonNull
        public static LargeExposuresDto from(@NonNull LargeExposuresParameters params) {
            return new LargeExposuresDto(
                params.limitPercent(),
                params.classificationThresholdPercent(),
                params.eligibleCapital().amount().longValue(),
                params.absoluteLimitValue().amount().longValue(),
                params.absoluteClassificationValue().amount().longValue(),
                params.regulatoryReference()
            );
        }
    }
    
    public record CapitalBaseDto(
        long eligibleCapital,
        long tier1Capital,
        long tier2Capital,
        @NonNull String calculationMethod,
        @NonNull LocalDate capitalReferenceDate,
        @NonNull String updateFrequency,
        @NonNull LocalDate nextUpdateDate
    ) {
        @NonNull
        public static CapitalBaseDto from(@NonNull CapitalBaseParameters params) {
            return new CapitalBaseDto(
                params.eligibleCapital().amount().longValue(),
                params.tier1Capital().amount().longValue(),
                params.tier2Capital().amount().longValue(),
                params.calculationMethod().name(),
                params.capitalReferenceDate(),
                params.updateFrequency().name(),
                params.nextUpdateDate()
            );
        }
    }
    
    public record ConcentrationRiskDto(
        double alertThresholdPercent,
        double attentionThresholdPercent,
        int maxLargeExposures
    ) {
        @NonNull
        public static ConcentrationRiskDto from(@NonNull ConcentrationRiskParameters params) {
            return new ConcentrationRiskDto(
                params.alertThresholdPercent(),
                params.attentionThresholdPercent(),
                params.maxLargeExposures()
            );
        }
    }
    
    public record ValidationStatusDto(
        boolean bcbs239Compliant,
        boolean capitalUpToDate
    ) {
        @NonNull
        public static ValidationStatusDto from(@NonNull ValidationStatus status) {
            return new ValidationStatusDto(
                status.bcbs239Compliant(),
                status.capitalUpToDate()
            );
        }
    }
    
    public record StatusDto(
        boolean valid,
        @NonNull Instant lastModifiedAt,
        @Nullable String lastModifiedBy
    ) {
        @NonNull
        public static StatusDto from(@NonNull RiskParameters parameters) {
            return new StatusDto(
                parameters.getValidationStatus().isValid(),
                parameters.getLastModifiedAt() != null ? 
                    parameters.getLastModifiedAt() : parameters.getCreatedAt(),
                parameters.getLastModifiedBy()
            );
        }
    }
}
