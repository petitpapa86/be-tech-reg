package com.bcbs239.regtech.riskcalculation.infrastructure.persistence.parameters;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "risk_parameters", schema = "riskcalculation")
@Getter
@Setter
public class RiskParametersJpaEntity {

    @Id
    private String id;

    @Column(name = "bank_id", nullable = false, unique = true)
    private String bankId;

    // Large Exposures
    @Column(name = "le_limit_percent")
    private double leLimitPercent;

    @Column(name = "le_classification_threshold_percent")
    private double leClassificationThresholdPercent;

    @Column(name = "le_eligible_capital_amount")
    private BigDecimal leEligibleCapitalAmount;

    @Column(name = "le_eligible_capital_currency")
    private String leEligibleCapitalCurrency;

    @Column(name = "le_absolute_limit_value_amount")
    private BigDecimal leAbsoluteLimitValueAmount;

    @Column(name = "le_absolute_limit_value_currency")
    private String leAbsoluteLimitValueCurrency;

    @Column(name = "le_absolute_classification_value_amount")
    private BigDecimal leAbsoluteClassificationValueAmount;

    @Column(name = "le_absolute_classification_value_currency")
    private String leAbsoluteClassificationValueCurrency;

    @Column(name = "le_regulatory_reference")
    private String leRegulatoryReference;

    // Capital Base
    @Column(name = "cb_eligible_capital_amount")
    private BigDecimal cbEligibleCapitalAmount;

    @Column(name = "cb_eligible_capital_currency")
    private String cbEligibleCapitalCurrency;

    @Column(name = "cb_tier1_capital_amount")
    private BigDecimal cbTier1CapitalAmount;

    @Column(name = "cb_tier1_capital_currency")
    private String cbTier1CapitalCurrency;

    @Column(name = "cb_tier2_capital_amount")
    private BigDecimal cbTier2CapitalAmount;

    @Column(name = "cb_tier2_capital_currency")
    private String cbTier2CapitalCurrency;

    @Column(name = "cb_calculation_method")
    private String cbCalculationMethod;

    @Column(name = "cb_capital_reference_date")
    private LocalDate cbCapitalReferenceDate;

    @Column(name = "cb_update_frequency")
    private String cbUpdateFrequency;

    @Column(name = "cb_next_update_date")
    private LocalDate cbNextUpdateDate;

    // Concentration Risk
    @Column(name = "cr_alert_threshold_percent")
    private double crAlertThresholdPercent;

    @Column(name = "cr_attention_threshold_percent")
    private double crAttentionThresholdPercent;

    @Column(name = "cr_max_large_exposures")
    private int crMaxLargeExposures;

    // Validation Status
    @Column(name = "vs_bcbs239_compliant")
    private boolean vsBcbs239Compliant;

    @Column(name = "vs_capital_up_to_date")
    private boolean vsCapitalUpToDate;

    // Audit
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "last_modified_at")
    private Instant lastModifiedAt;

    @Column(name = "last_modified_by")
    private String lastModifiedBy;

    @Version
    private Long version;
}
