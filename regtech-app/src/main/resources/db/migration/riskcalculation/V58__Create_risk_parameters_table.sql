CREATE TABLE riskcalculation.risk_parameters (
    id VARCHAR(36) PRIMARY KEY,
    bank_id VARCHAR(50) NOT NULL UNIQUE,
    
    -- Large Exposures
    le_limit_percent DOUBLE PRECISION,
    le_classification_threshold_percent DOUBLE PRECISION,
    le_eligible_capital_amount DECIMAL(19, 4),
    le_eligible_capital_currency VARCHAR(3),
    le_absolute_limit_value_amount DECIMAL(19, 4),
    le_absolute_limit_value_currency VARCHAR(3),
    le_absolute_classification_value_amount DECIMAL(19, 4),
    le_absolute_classification_value_currency VARCHAR(3),
    le_regulatory_reference VARCHAR(255),
    
    -- Capital Base
    cb_eligible_capital_amount DECIMAL(19, 4),
    cb_eligible_capital_currency VARCHAR(3),
    cb_tier1_capital_amount DECIMAL(19, 4),
    cb_tier1_capital_currency VARCHAR(3),
    cb_tier2_capital_amount DECIMAL(19, 4),
    cb_tier2_capital_currency VARCHAR(3),
    cb_calculation_method VARCHAR(50),
    cb_capital_reference_date DATE,
    cb_update_frequency VARCHAR(20),
    cb_next_update_date DATE,
    
    -- Concentration Risk
    cr_alert_threshold_percent DOUBLE PRECISION,
    cr_attention_threshold_percent DOUBLE PRECISION,
    cr_max_large_exposures INTEGER,
    
    -- Validation Status
    vs_bcbs239_compliant BOOLEAN,
    vs_capital_up_to_date BOOLEAN,
    
    -- Audit
    created_at TIMESTAMP NOT NULL,
    last_modified_at TIMESTAMP,
    last_modified_by VARCHAR(100),
    version BIGINT
);

CREATE INDEX idx_risk_parameters_bank_id ON riskcalculation.risk_parameters(bank_id);
