package com.bcbs239.regtech.riskcalculation.infrastructure.persistence.parameters;

import com.bcbs239.regtech.riskcalculation.domain.parameters.*;
import com.bcbs239.regtech.riskcalculation.domain.shared.Money;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class RiskParametersRepositoryAdapter implements RiskParametersRepository {

    private final RiskParametersJpaRepository jpaRepository;

    public RiskParametersRepositoryAdapter(RiskParametersJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @NonNull
    public Optional<RiskParameters> findById(@NonNull RiskParametersId id) {
        return jpaRepository.findById(id.value()).map(this::toDomain);
    }

    @Override
    @NonNull
    public Optional<RiskParameters> findByBankId(@NonNull String bankId) {
        return jpaRepository.findByBankId(bankId).map(this::toDomain);
    }

    @Override
    public boolean existsByBankId(@NonNull String bankId) {
        return jpaRepository.findByBankId(bankId).isPresent();
    }

    @Override
    @NonNull
    public RiskParameters save(@NonNull RiskParameters parameters) {
        RiskParametersJpaEntity entity = toEntity(parameters);
        RiskParametersJpaEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    private RiskParameters toDomain(RiskParametersJpaEntity entity) {
        return RiskParameters.reconstitute(
            new RiskParametersId(entity.getId()),
            entity.getBankId(),
            new LargeExposuresParameters(
                entity.getLeLimitPercent(),
                entity.getLeClassificationThresholdPercent(),
                new Money(entity.getLeEligibleCapitalAmount(), entity.getLeEligibleCapitalCurrency()),
                new Money(entity.getLeAbsoluteLimitValueAmount(), entity.getLeAbsoluteLimitValueCurrency()),
                new Money(entity.getLeAbsoluteClassificationValueAmount(), entity.getLeAbsoluteClassificationValueCurrency()),
                entity.getLeRegulatoryReference()
            ),
            new CapitalBaseParameters(
                new Money(entity.getCbEligibleCapitalAmount(), entity.getCbEligibleCapitalCurrency()),
                new Money(entity.getCbTier1CapitalAmount(), entity.getCbTier1CapitalCurrency()),
                new Money(entity.getCbTier2CapitalAmount(), entity.getCbTier2CapitalCurrency()),
                CapitalBaseParameters.CalculationMethod.valueOf(entity.getCbCalculationMethod()),
                entity.getCbCapitalReferenceDate(),
                CapitalBaseParameters.UpdateFrequency.valueOf(entity.getCbUpdateFrequency()),
                entity.getCbNextUpdateDate()
            ),
            new ConcentrationRiskParameters(
                entity.getCrAlertThresholdPercent(),
                entity.getCrAttentionThresholdPercent(),
                entity.getCrMaxLargeExposures()
            ),
            new ValidationStatus(
                entity.isVsBcbs239Compliant(),
                entity.isVsCapitalUpToDate()
            ),
            entity.getCreatedAt(),
            entity.getLastModifiedAt(),
            entity.getLastModifiedBy(),
            entity.getVersion()
        );
    }

    private RiskParametersJpaEntity toEntity(RiskParameters parameters) {
        RiskParametersJpaEntity entity = new RiskParametersJpaEntity();
        entity.setId(parameters.getId().value());
        entity.setBankId(parameters.getBankId());
        
        // Large Exposures
        LargeExposuresParameters le = parameters.getLargeExposures();
        entity.setLeLimitPercent(le.limitPercent());
        entity.setLeClassificationThresholdPercent(le.classificationThresholdPercent());
        entity.setLeEligibleCapitalAmount(le.eligibleCapital().amount());
        entity.setLeEligibleCapitalCurrency(le.eligibleCapital().currency());
        entity.setLeAbsoluteLimitValueAmount(le.absoluteLimitValue().amount());
        entity.setLeAbsoluteLimitValueCurrency(le.absoluteLimitValue().currency());
        entity.setLeAbsoluteClassificationValueAmount(le.absoluteClassificationValue().amount());
        entity.setLeAbsoluteClassificationValueCurrency(le.absoluteClassificationValue().currency());
        entity.setLeRegulatoryReference(le.regulatoryReference());
        
        // Capital Base
        CapitalBaseParameters cb = parameters.getCapitalBase();
        entity.setCbEligibleCapitalAmount(cb.eligibleCapital().amount());
        entity.setCbEligibleCapitalCurrency(cb.eligibleCapital().currency());
        entity.setCbTier1CapitalAmount(cb.tier1Capital().amount());
        entity.setCbTier1CapitalCurrency(cb.tier1Capital().currency());
        entity.setCbTier2CapitalAmount(cb.tier2Capital().amount());
        entity.setCbTier2CapitalCurrency(cb.tier2Capital().currency());
        entity.setCbCalculationMethod(cb.calculationMethod().name());
        entity.setCbCapitalReferenceDate(cb.capitalReferenceDate());
        entity.setCbUpdateFrequency(cb.updateFrequency().name());
        entity.setCbNextUpdateDate(cb.nextUpdateDate());
        
        // Concentration Risk
        ConcentrationRiskParameters cr = parameters.getConcentrationRisk();
        entity.setCrAlertThresholdPercent(cr.alertThresholdPercent());
        entity.setCrAttentionThresholdPercent(cr.attentionThresholdPercent());
        entity.setCrMaxLargeExposures(cr.maxLargeExposures());
        
        // Validation Status
        ValidationStatus vs = parameters.getValidationStatus();
        entity.setVsBcbs239Compliant(vs.bcbs239Compliant());
        entity.setVsCapitalUpToDate(vs.capitalUpToDate());
        
        // Audit
        entity.setCreatedAt(parameters.getCreatedAt());
        entity.setLastModifiedAt(parameters.getLastModifiedAt());
        entity.setLastModifiedBy(parameters.getLastModifiedBy());
        entity.setVersion(parameters.getVersion());
        
        return entity;
    }
}
