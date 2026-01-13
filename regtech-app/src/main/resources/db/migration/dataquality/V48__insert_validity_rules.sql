-- V48__insert_validity_rules.sql
-- Insert DIMENSIONE: VALIDITÀ (Validity) business rules for BCBS 239 compliance
-- Validates that data respects business rules and expected formats

-- =====================================================
-- VALIDITY RULES (6 rules)
-- =====================================================

-- Rule 1: ExposureAmount > 0
INSERT INTO dataquality.business_rules (
    rule_id, regulation_id, rule_name, rule_code, description,
    rule_type, rule_category, severity, business_logic,
    execution_order, effective_date, enabled, created_at, updated_at
) VALUES (
    'DQ_VALIDITY_POSITIVE_EXPOSURE_AMOUNT',
    'BCBS_239_DATA_QUALITY',
    'Exposure Amount Must Be Positive',
    'VALIDITY_POSITIVE_EXPOSURE_AMOUNT',
    'Valida che l''importo dell''esposizione sia maggiore di zero (ExposureAmount > 0)',
    'VALIDITY',
    'BUSINESS_LOGIC',
    'CRITICAL',
    '#exposureAmount != null && #exposureAmount.compareTo(new java.math.BigDecimal(''0'')) > 0',
    50,
    '2024-01-01',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (rule_id) DO NOTHING;

-- Rule 2: MaturityDate > Today
INSERT INTO dataquality.business_rules (
    rule_id, regulation_id, rule_name, rule_code, description,
    rule_type, rule_category, severity, business_logic,
    execution_order, effective_date, enabled, created_at, updated_at
) VALUES (
    'DQ_VALIDITY_FUTURE_MATURITY_DATE',
    'BCBS_239_DATA_QUALITY',
    'Maturity Date Must Be In The Future',
    'VALIDITY_FUTURE_MATURITY_DATE',
    'Valida che la data di scadenza sia futura (MaturityDate > Today)',
    'VALIDITY',
    'BUSINESS_LOGIC',
    'HIGH',
    '#maturityDate == null || #maturityDate.isAfter(T(java.time.LocalDate).now())',
    51,
    '2024-01-01',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (rule_id) DO NOTHING;

-- Rule 3: Sector in Valid Catalog
INSERT INTO dataquality.business_rules (
    rule_id, regulation_id, rule_name, rule_code, description,
    rule_type, rule_category, severity, business_logic,
    execution_order, effective_date, enabled, created_at, updated_at
) VALUES (
    'DQ_VALIDITY_VALID_SECTOR',
    'BCBS_239_DATA_QUALITY',
    'Sector Must Be In Valid Catalog',
    'VALIDITY_VALID_SECTOR',
    'Valida che il settore sia nel catalogo valido (Sector in Catalogo Valido)',
    'VALIDITY',
    'DATA_QUALITY',
    'HIGH',
    '#sector == null || #validSectors.contains(#sector.toUpperCase())',
    52,
    '2024-01-01',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (rule_id) DO NOTHING;

-- Rule 3 Parameter: Valid Sectors Catalog
INSERT INTO dataquality.rule_parameters (
    rule_id, parameter_name, parameter_value, parameter_type, data_type,
    description, is_configurable, created_at, updated_at
) VALUES (
    'DQ_VALIDITY_VALID_SECTOR',
    'validSectors',
    'BANKING,CORPORATE,FINANCIAL_INSTITUTIONS,GOVERNMENT,INSURANCE,REAL_ESTATE,RETAIL,MANUFACTURING,ENERGY,UTILITIES,TELECOMMUNICATIONS,HEALTHCARE,CONSUMER_GOODS,TECHNOLOGY,TRANSPORTATION,AGRICULTURE,MINING,CONSTRUCTION,HOSPITALITY,EDUCATION,NON_PROFIT,OTHER',
    'LIST',
    'STRING',
    'Elenco separato da virgole dei settori validi secondo il catalogo BCBS 239',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (rule_id, parameter_name) DO NOTHING;

-- Rule 4: Currency ISO 4217
INSERT INTO dataquality.business_rules (
    rule_id, regulation_id, rule_name, rule_code, description,
    rule_type, rule_category, severity, business_logic,
    execution_order, effective_date, enabled, created_at, updated_at
) VALUES (
    'DQ_VALIDITY_ISO_CURRENCY',
    'BCBS_239_DATA_QUALITY',
    'Currency Must Follow ISO 4217',
    'VALIDITY_ISO_CURRENCY',
    'Valida che la valuta rispetti lo standard ISO 4217 (Currency ISO 4217)',
    'VALIDITY',
    'DATA_QUALITY',
    'CRITICAL',
    '#currency == null || #validCurrencies.contains(#currency.toUpperCase())',
    53,
    '2024-01-01',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (rule_id) DO NOTHING;

-- Rule 4 Parameter: Valid ISO 4217 Currencies
INSERT INTO dataquality.rule_parameters (
    rule_id, parameter_name, parameter_value, parameter_type, data_type,
    description, is_configurable, created_at, updated_at
) VALUES (
    'DQ_VALIDITY_ISO_CURRENCY',
    'validCurrencies',
    'USD,EUR,GBP,JPY,CHF,CAD,AUD,SEK,NOK,DKK,PLN,CZK,HUF,BGN,RON,HRK,RSD,BAM,MKD,ALL,CNY,HKD,SGD,KRW,INR,THB,MYR,IDR,PHP,VND,BRL,MXN,ARS,CLP,COP,PEN,UYU,ZAR,EGP,MAD,TND,NGN,GHS,KES,UGX,TZS,ZMW,BWP,MUR,SCR',
    'LIST',
    'STRING',
    'Elenco separato da virgole dei codici valuta ISO 4217 validi',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (rule_id, parameter_name) DO NOTHING;

-- Rule 5: InternalRating Format
INSERT INTO dataquality.business_rules (
    rule_id, regulation_id, rule_name, rule_code, description,
    rule_type, rule_category, severity, business_logic,
    execution_order, effective_date, enabled, created_at, updated_at
) VALUES (
    'DQ_VALIDITY_RATING_FORMAT',
    'BCBS_239_DATA_QUALITY',
    'Internal Rating Must Follow Valid Format',
    'VALIDITY_RATING_FORMAT',
    'Valida che il rating interno rispetti il formato valido (AAA, AA+, AA, AA-, A+, A, A-, BBB+, BBB, BBB-, BB+, BB, BB-, B+, B, B-, CCC+, CCC, CCC-, CC, C, D)',
    'VALIDITY',
    'REGULATORY_COMPLIANCE',
    'HIGH',
    '#internalRating == null || #validRatings.contains(#internalRating.toUpperCase())',
    54,
    '2024-01-01',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (rule_id) DO NOTHING;

-- Rule 5 Parameter: Valid Rating Formats
INSERT INTO dataquality.rule_parameters (
    rule_id, parameter_name, parameter_value, parameter_type, data_type,
    description, is_configurable, created_at, updated_at
) VALUES (
    'DQ_VALIDITY_RATING_FORMAT',
    'validRatings',
    'AAA,AA+,AA,AA-,A+,A,A-,BBB+,BBB,BBB-,BB+,BB,BB-,B+,B,B-,CCC+,CCC,CCC-,CC,C,D',
    'LIST',
    'STRING',
    'Elenco separato da virgole dei formati rating interni validi',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (rule_id, parameter_name) DO NOTHING;

-- Rule 6: Collateral_value ≤ 3 × Exposure_amount
INSERT INTO dataquality.business_rules (
    rule_id, regulation_id, rule_name, rule_code, description,
    rule_type, rule_category, severity, business_logic,
    execution_order, effective_date, enabled, created_at, updated_at
) VALUES (
    'DQ_VALIDITY_COLLATERAL_THRESHOLD',
    'BCBS_239_DATA_QUALITY',
    'Collateral Value Must Not Exceed 3x Exposure Amount',
    'VALIDITY_COLLATERAL_THRESHOLD',
    'Valida che il valore della garanzia non superi 3 volte l''importo dell''esposizione (Collateral_value ≤ 3 × Exposure_amount)',
    'VALIDITY',
    'BUSINESS_LOGIC',
    'HIGH',
    '#collateralValue == null || #exposureAmount == null || #collateralValue.compareTo(#exposureAmount.multiply(#maxCollateralMultiplier)) <= 0',
    55,
    '2024-01-01',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (rule_id) DO NOTHING;

-- Rule 6 Parameter: Max Collateral Multiplier
INSERT INTO dataquality.rule_parameters (
    rule_id, parameter_name, parameter_value, parameter_type, data_type,
    description, is_configurable, created_at, updated_at
) VALUES (
    'DQ_VALIDITY_COLLATERAL_THRESHOLD',
    'maxCollateralMultiplier',
    '3',
    'NUMERIC',
    'DECIMAL',
    'Moltiplicatore massimo per il valore della garanzia rispetto all''esposizione (default: 3x)',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (rule_id, parameter_name) DO NOTHING;

-- Add comment explaining validity dimension
COMMENT ON TABLE dataquality.business_rules IS 'Business rules for BCBS 239 data quality validation across 6 dimensions: COMPLETENESS, ACCURACY, CONSISTENCY, TIMELINESS, UNIQUENESS, VALIDITY';
