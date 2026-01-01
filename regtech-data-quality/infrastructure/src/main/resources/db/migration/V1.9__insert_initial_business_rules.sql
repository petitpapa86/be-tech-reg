-- =====================================================
-- Initial Business Rules Data Migration
-- =====================================================
-- This migration populates the rules engine with default
-- data quality rules for BCBS 239 compliance.
-- =====================================================

-- =====================================================
-- COMPLETENESS RULES (8 rules)
-- =====================================================

-- Rule 1: Exposure ID Required
INSERT INTO dataquality.business_rules (
    rule_id, regulation_id, rule_name, rule_code, description,
    rule_type, rule_category, severity, business_logic,
    execution_order, effective_date, enabled, created_at, updated_at
) VALUES (
    'DQ_COMPLETENESS_EXPOSURE_ID',
    'BCBS_239_DATA_QUALITY',
    'Exposure ID Required',
    'COMPLETENESS_EXPOSURE_ID_REQUIRED',
    'Validates that exposure ID is present and non-empty',
    'COMPLETENESS',
    'DATA_QUALITY',
    'CRITICAL',
    '#exposureId != null && !#exposureId.trim().isEmpty()',
    1,
    '2024-01-01',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (rule_id) DO NOTHING;

-- Rule 2: Amount Required
INSERT INTO dataquality.business_rules (
    rule_id, regulation_id, rule_name, rule_code, description,
    rule_type, rule_category, severity, business_logic,
    execution_order, effective_date, enabled, created_at, updated_at
) VALUES (
    'DQ_COMPLETENESS_AMOUNT',
    'BCBS_239_DATA_QUALITY',
    'Amount Required',
    'COMPLETENESS_AMOUNT_REQUIRED',
    'Validates that amount is present',
    'COMPLETENESS',
    'DATA_QUALITY',
    'CRITICAL',
    '#amount != null',
    2,
    '2024-01-01',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (rule_id) DO NOTHING;

-- Rule 3: Currency Required
INSERT INTO dataquality.business_rules (
    rule_id, regulation_id, rule_name, rule_code, description,
    rule_type, rule_category, severity, business_logic,
    execution_order, effective_date, enabled, created_at, updated_at
) VALUES (
    'DQ_COMPLETENESS_CURRENCY',
    'BCBS_239_DATA_QUALITY',
    'Currency Required',
    'COMPLETENESS_CURRENCY_REQUIRED',
    'Validates that currency is present and non-empty',
    'COMPLETENESS',
    'DATA_QUALITY',
    'CRITICAL',
    '#currency != null && !#currency.trim().isEmpty()',
    3,
    '2024-01-01',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (rule_id) DO NOTHING;

-- Rule 4: Country Required
INSERT INTO dataquality.business_rules (
    rule_id, regulation_id, rule_name, rule_code, description,
    rule_type, rule_category, severity, business_logic,
    execution_order, effective_date, enabled, created_at, updated_at
) VALUES (
    'DQ_COMPLETENESS_COUNTRY',
    'BCBS_239_DATA_QUALITY',
    'Country Required',
    'COMPLETENESS_COUNTRY_REQUIRED',
    'Validates that country is present and non-empty',
    'COMPLETENESS',
    'DATA_QUALITY',
    'CRITICAL',
    '#country != null && !#country.trim().isEmpty()',
    4,
    '2024-01-01',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (rule_id) DO NOTHING;

-- Rule 5: Sector Required
INSERT INTO dataquality.business_rules (
    rule_id, regulation_id, rule_name, rule_code, description,
    rule_type, rule_category, severity, business_logic,
    execution_order, effective_date, enabled, created_at, updated_at
) VALUES (
    'DQ_COMPLETENESS_SECTOR',
    'BCBS_239_DATA_QUALITY',
    'Sector Required',
    'COMPLETENESS_SECTOR_REQUIRED',
    'Validates that sector is present and non-empty',
    'COMPLETENESS',
    'DATA_QUALITY',
    'CRITICAL',
    '#sector != null && !#sector.trim().isEmpty()',
    5,
    '2024-01-01',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (rule_id) DO NOTHING;

-- Rule 6: LEI for Corporates
INSERT INTO dataquality.business_rules (
    rule_id, regulation_id, rule_name, rule_code, description,
    rule_type, rule_category, severity, business_logic,
    execution_order, effective_date, enabled, created_at, updated_at
) VALUES (
    'DQ_COMPLETENESS_LEI_CORPORATES',
    'BCBS_239_DATA_QUALITY',
    'LEI Required for Corporate Exposures',
    'COMPLETENESS_LEI_FOR_CORPORATES',
    'Validates that corporate exposures have LEI codes',
    'COMPLETENESS',
    'REGULATORY_COMPLIANCE',
    'HIGH',
    '#sector != ''CORPORATE'' || (#leiCode != null && !#leiCode.trim().isEmpty())',
    6,
    '2024-01-01',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (rule_id) DO NOTHING;

-- Rule 7: Maturity for Term Exposures
INSERT INTO dataquality.business_rules (
    rule_id, regulation_id, rule_name, rule_code, description,
    rule_type, rule_category, severity, business_logic,
    execution_order, effective_date, enabled, created_at, updated_at
) VALUES (
    'DQ_COMPLETENESS_MATURITY_TERM',
    'BCBS_239_DATA_QUALITY',
    'Maturity Date Required for Term Exposures',
    'COMPLETENESS_MATURITY_FOR_TERM',
    'Validates that term exposures (non-equity) have maturity dates',
    'COMPLETENESS',
    'BUSINESS_LOGIC',
    'HIGH',
    '#productType == ''EQUITY'' || #maturityDate != null',
    7,
    '2024-01-01',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (rule_id) DO NOTHING;

-- Rule 8: Internal Rating Required
INSERT INTO dataquality.business_rules (
    rule_id, regulation_id, rule_name, rule_code, description,
    rule_type, rule_category, severity, business_logic,
    execution_order, effective_date, enabled, created_at, updated_at
) VALUES (
    'DQ_COMPLETENESS_INTERNAL_RATING',
    'BCBS_239_DATA_QUALITY',
    'Internal Rating Required',
    'COMPLETENESS_INTERNAL_RATING',
    'Validates that internal rating is present for risk assessment',
    'COMPLETENESS',
    'REGULATORY_COMPLIANCE',
    'HIGH',
    '#internalRating != null && !#internalRating.trim().isEmpty()',
    8,
    '2024-01-01',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (rule_id) DO NOTHING;

-- =====================================================
-- ACCURACY RULES (5 rules)
-- =====================================================

-- Rule 1: Positive Amount
INSERT INTO dataquality.business_rules (
    rule_id, regulation_id, rule_name, rule_code, description,
    rule_type, rule_category, severity, business_logic,
    execution_order, effective_date, enabled, created_at, updated_at
) VALUES (
    'DQ_ACCURACY_POSITIVE_AMOUNT',
    'BCBS_239_DATA_QUALITY',
    'Positive Amount',
    'ACCURACY_POSITIVE_AMOUNT',
    'Validates that exposure amount is positive',
    'ACCURACY',
    'DATA_QUALITY',
    'CRITICAL',
    '#amount != null && #amount > 0',
    10,
    '2024-01-01',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (rule_id) DO NOTHING;

-- Rule 2: Valid Currency Codes
INSERT INTO dataquality.business_rules (
    rule_id, regulation_id, rule_name, rule_code, description,
    rule_type, rule_category, severity, business_logic,
    execution_order, effective_date, enabled, created_at, updated_at
) VALUES (
    'DQ_ACCURACY_VALID_CURRENCY',
    'BCBS_239_DATA_QUALITY',
    'Valid Currency Codes',
    'ACCURACY_VALID_CURRENCY',
    'Validates currency against list of valid ISO 4217 codes',
    'ACCURACY',
    'DATA_QUALITY',
    'HIGH',
    '#currency == null || #validCurrencies.contains(#currency.toUpperCase())',
    11,
    '2024-01-01',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (rule_id) DO NOTHING;

-- Rule 2 Parameter: Valid Currencies List
INSERT INTO dataquality.rule_parameters (
    rule_id, parameter_name, parameter_value, parameter_type, data_type,
    description, is_configurable, created_at, updated_at
) VALUES (
    'DQ_ACCURACY_VALID_CURRENCY',
    'validCurrencies',
    'USD,EUR,GBP,JPY,CHF,CAD,AUD,SEK,NOK,DKK,PLN,CZK,HUF,BGN,RON,HRK,RSD,BAM,MKD,ALL,CNY,HKD,SGD,KRW,INR,THB,MYR,IDR,PHP,VND,BRL,MXN,ARS,CLP,COP,PEN,UYU,ZAR,EGP,MAD,TND,NGN,GHS,KES,UGX,TZS,ZMW,BWP,MUR,SCR',
    'LIST',
    'STRING',
    'Comma-separated list of valid ISO 4217 currency codes',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (rule_id, parameter_name) DO NOTHING;

-- Rule 3: Valid Country Codes
INSERT INTO dataquality.business_rules (
    rule_id, regulation_id, rule_name, rule_code, description,
    rule_type, rule_category, severity, business_logic,
    execution_order, effective_date, enabled, created_at, updated_at
) VALUES (
    'DQ_ACCURACY_VALID_COUNTRY',
    'BCBS_239_DATA_QUALITY',
    'Valid Country Codes',
    'ACCURACY_VALID_COUNTRY',
    'Validates country against list of valid ISO 3166 codes',
    'ACCURACY',
    'DATA_QUALITY',
    'HIGH',
    '#country == null || #validCountries.contains(#country.toUpperCase())',
    12,
    '2024-01-01',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (rule_id) DO NOTHING;

-- Rule 3 Parameter: Valid Countries List
INSERT INTO dataquality.rule_parameters (
    rule_id, parameter_name, parameter_value, parameter_type, data_type,
    description, is_configurable, created_at, updated_at
) VALUES (
    'DQ_ACCURACY_VALID_COUNTRY',
    'validCountries',
    'US,GB,DE,FR,IT,ES,NL,BE,AT,CH,SE,NO,DK,FI,IE,PT,GR,PL,CZ,HU,SK,SI,EE,LV,LT,BG,RO,HR,CY,MT,LU,JP,CN,HK,SG,KR,IN,TH,MY,ID,PH,VN,AU,NZ,CA,MX,BR,AR,CL,CO,PE,UY,ZA,EG,MA,TN,NG,GH,KE,UG,TZ,ZM,BW,MU,SC,RU,UA,BY,MD,GE,AM,AZ,KZ,UZ,KG,TJ,TM,MN,TR,IL,SA,AE,QA,KW,BH,OM,JO,LB,SY,IQ,IR,AF,PK,BD,LK,NP,BT,MM,LA,KH,BN,TL,FJ,PG,SB,VU,NC,PF',
    'LIST',
    'STRING',
    'Comma-separated list of valid ISO 3166 country codes',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (rule_id, parameter_name) DO NOTHING;

-- Rule 4: Valid LEI Format
INSERT INTO dataquality.business_rules (
    rule_id, regulation_id, rule_name, rule_code, description,
    rule_type, rule_category, severity, business_logic,
    execution_order, effective_date, enabled, created_at, updated_at
) VALUES (
    'DQ_ACCURACY_VALID_LEI_FORMAT',
    'BCBS_239_DATA_QUALITY',
    'Valid LEI Format',
    'ACCURACY_VALID_LEI_FORMAT',
    'Validates that LEI code has correct format (20 alphanumeric characters)',
    'ACCURACY',
    'REGULATORY_COMPLIANCE',
    'HIGH',
    '#leiCode == null || #leiCode.matches(''[A-Z0-9]{20}'')',
    13,
    '2024-01-01',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (rule_id) DO NOTHING;

-- Rule 5: Reasonable Amount
INSERT INTO dataquality.business_rules (
    rule_id, regulation_id, rule_name, rule_code, description,
    rule_type, rule_category, severity, business_logic,
    execution_order, effective_date, enabled, created_at, updated_at
) VALUES (
    'DQ_ACCURACY_REASONABLE_AMOUNT',
    'BCBS_239_DATA_QUALITY',
    'Reasonable Amount',
    'ACCURACY_REASONABLE_AMOUNT',
    'Validates that exposure amount is within reasonable bounds (10B EUR)',
    'ACCURACY',
    'BUSINESS_LOGIC',
    'MEDIUM',
    '#amount == null || #amount < #maxReasonableAmount',
    14,
    '2024-01-01',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (rule_id) DO NOTHING;

-- Rule 5 Parameter: Max Reasonable Amount
INSERT INTO dataquality.rule_parameters (
    rule_id, parameter_name, parameter_value, parameter_type, data_type,
    unit, min_value, max_value, description, is_configurable, created_at, updated_at
) VALUES (
    'DQ_ACCURACY_REASONABLE_AMOUNT',
    'maxReasonableAmount',
    '10000000000',
    'NUMERIC',
    'DECIMAL',
    'EUR',
    NULL,
    NULL,
    'Maximum reasonable exposure amount (10 billion EUR)',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (rule_id, parameter_name) DO NOTHING;

-- =====================================================
-- TIMELINESS RULES (3 rules)
-- =====================================================

-- Rule 1: Reporting Period
INSERT INTO dataquality.business_rules (
    rule_id, regulation_id, rule_name, rule_code, description,
    rule_type, rule_category, severity, business_logic,
    execution_order, effective_date, enabled, created_at, updated_at
) VALUES (
    'DQ_TIMELINESS_REPORTING_PERIOD',
    'BCBS_239_DATA_QUALITY',
    'Reporting Period',
    'TIMELINESS_REPORTING_PERIOD',
    'Validates that reporting date is within acceptable reporting period (not older than max age)',
    'TIMELINESS',
    'REGULATORY_COMPLIANCE',
    'HIGH',
    '#reportingDate == null || T(java.time.temporal.ChronoUnit).DAYS.between(#reportingDate, T(java.time.LocalDate).now()) <= #maxReportingAgeDays',
    30,
    '2024-01-01',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (rule_id) DO NOTHING;

-- Rule 1 Parameter: Max Reporting Age
INSERT INTO dataquality.rule_parameters (
    rule_id, parameter_name, parameter_value, parameter_type, data_type,
    unit, min_value, max_value, description, is_configurable, created_at, updated_at
) VALUES (
    'DQ_TIMELINESS_REPORTING_PERIOD',
    'maxReportingAgeDays',
    '90',
    'NUMERIC',
    'INTEGER',
    'days',
    1,
    365,
    'Maximum age for reporting data (90 days)',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (rule_id, parameter_name) DO NOTHING;

-- Rule 2: No Future Date
INSERT INTO dataquality.business_rules (
    rule_id, regulation_id, rule_name, rule_code, description,
    rule_type, rule_category, severity, business_logic,
    execution_order, effective_date, enabled, created_at, updated_at
) VALUES (
    'DQ_TIMELINESS_NO_FUTURE_DATE',
    'BCBS_239_DATA_QUALITY',
    'No Future Date',
    'TIMELINESS_NO_FUTURE_DATE',
    'Validates that reporting date is not in the future',
    'TIMELINESS',
    'DATA_QUALITY',
    'CRITICAL',
    '#reportingDate == null || !#reportingDate.isAfter(T(java.time.LocalDate).now())',
    31,
    '2024-01-01',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (rule_id) DO NOTHING;

-- Rule 3: Recent Valuation
INSERT INTO dataquality.business_rules (
    rule_id, regulation_id, rule_name, rule_code, description,
    rule_type, rule_category, severity, business_logic,
    execution_order, effective_date, enabled, created_at, updated_at
) VALUES (
    'DQ_TIMELINESS_RECENT_VALUATION',
    'BCBS_239_DATA_QUALITY',
    'Recent Valuation',
    'TIMELINESS_RECENT_VALUATION',
    'Validates that valuation date is recent enough (not older than 30 days)',
    'TIMELINESS',
    'BUSINESS_LOGIC',
    'MEDIUM',
    '#valuationDate == null || T(java.time.temporal.ChronoUnit).DAYS.between(#valuationDate, T(java.time.LocalDate).now()) <= 30',
    32,
    '2024-01-01',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (rule_id) DO NOTHING;

-- =====================================================
-- CONSISTENCY RULES (3 rules - disabled, require custom implementation)
-- =====================================================

-- Rule 1: Currency-Country Consistency
INSERT INTO dataquality.business_rules (
    rule_id, regulation_id, rule_name, rule_code, description,
    rule_type, rule_category, severity, business_logic,
    execution_order, effective_date, enabled, created_at, updated_at
) VALUES (
    'DQ_CONSISTENCY_CURRENCY_COUNTRY',
    'BCBS_239_DATA_QUALITY',
    'Currency-Country Consistency',
    'CONSISTENCY_CURRENCY_COUNTRY',
    'Validates that currency matches country (e.g., EUR for Eurozone countries)',
    'CONSISTENCY',
    'BUSINESS_LOGIC',
    'MEDIUM',
    'true',
    20,
    '2024-01-01',
    false,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (rule_id) DO NOTHING;

-- Rule 2: Sector-Counterparty Type Consistency
INSERT INTO dataquality.business_rules (
    rule_id, regulation_id, rule_name, rule_code, description,
    rule_type, rule_category, severity, business_logic,
    execution_order, effective_date, enabled, created_at, updated_at
) VALUES (
    'DQ_CONSISTENCY_SECTOR_COUNTERPARTY',
    'BCBS_239_DATA_QUALITY',
    'Sector-Counterparty Type Consistency',
    'CONSISTENCY_SECTOR_COUNTERPARTY',
    'Validates that sector classification is consistent with counterparty type',
    'CONSISTENCY',
    'BUSINESS_LOGIC',
    'MEDIUM',
    'true',
    21,
    '2024-01-01',
    false,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (rule_id) DO NOTHING;

-- Rule 3: Rating-Risk Category Consistency
INSERT INTO dataquality.business_rules (
    rule_id, regulation_id, rule_name, rule_code, description,
    rule_type, rule_category, severity, business_logic,
    execution_order, effective_date, enabled, created_at, updated_at
) VALUES (
    'DQ_CONSISTENCY_RATING_RISK',
    'BCBS_239_DATA_QUALITY',
    'Rating-Risk Category Consistency',
    'CONSISTENCY_RATING_RISK',
    'Validates that internal rating is consistent with risk category',
    'CONSISTENCY',
    'BUSINESS_LOGIC',
    'MEDIUM',
    'true',
    22,
    '2024-01-01',
    false,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (rule_id) DO NOTHING;

-- =====================================================
-- UNIQUENESS RULES (2 rules - disabled, require batch-level implementation)
-- =====================================================

-- Rule 1: Unique Exposure IDs
INSERT INTO dataquality.business_rules (
    rule_id, regulation_id, rule_name, rule_code, description,
    rule_type, rule_category, severity, business_logic,
    execution_order, effective_date, enabled, created_at, updated_at
) VALUES (
    'DQ_UNIQUENESS_EXPOSURE_IDS',
    'BCBS_239_DATA_QUALITY',
    'Unique Exposure IDs',
    'UNIQUENESS_EXPOSURE_IDS',
    'Validates that all exposure IDs within a batch are unique (batch-level check)',
    'UNIQUENESS',
    'DATA_QUALITY',
    'CRITICAL',
    'true',
    40,
    '2024-01-01',
    false,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (rule_id) DO NOTHING;

-- Rule 2: Unique Counterparty-Exposure Pairs
INSERT INTO dataquality.business_rules (
    rule_id, regulation_id, rule_name, rule_code, description,
    rule_type, rule_category, severity, business_logic,
    execution_order, effective_date, enabled, created_at, updated_at
) VALUES (
    'DQ_UNIQUENESS_COUNTERPARTY_EXPOSURE',
    'BCBS_239_DATA_QUALITY',
    'Unique Counterparty-Exposure Pairs',
    'UNIQUENESS_COUNTERPARTY_EXPOSURE',
    'Validates that counterparty-exposure pairs are unique within a batch (batch-level check)',
    'UNIQUENESS',
    'DATA_QUALITY',
    'HIGH',
    'true',
    41,
    '2024-01-01',
    false,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (rule_id) DO NOTHING;

-- =====================================================
-- VALIDITY RULES (3 rules)
-- =====================================================

-- Rule 1: Valid Sector
INSERT INTO dataquality.business_rules (
    rule_id, regulation_id, rule_name, rule_code, description,
    rule_type, rule_category, severity, business_logic,
    execution_order, effective_date, enabled, created_at, updated_at
) VALUES (
    'DQ_VALIDITY_VALID_SECTOR',
    'BCBS_239_DATA_QUALITY',
    'Valid Sector',
    'VALIDITY_VALID_SECTOR',
    'Validates that sector is from the list of valid sectors',
    'VALIDITY',
    'DATA_QUALITY',
    'HIGH',
    '#sector == null || #validSectors.contains(#sector.toUpperCase())',
    50,
    '2024-01-01',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (rule_id) DO NOTHING;

-- Rule 1 Parameter: Valid Sectors List
INSERT INTO dataquality.rule_parameters (
    rule_id, parameter_name, parameter_value, parameter_type, data_type,
    description, is_configurable, created_at, updated_at
) VALUES (
    'DQ_VALIDITY_VALID_SECTOR',
    'validSectors',
    'BANKING,CORPORATE_MANUFACTURING,CORPORATE_SERVICES,CORPORATE_RETAIL,CORPORATE_TECHNOLOGY,SOVEREIGN,RETAIL,SME,REAL_ESTATE,INSURANCE,CORPORATE',
    'LIST',
    'STRING',
    'Comma-separated list of valid sector codes',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (rule_id, parameter_name) DO NOTHING;

-- Rule 2: Risk Weight Range
INSERT INTO dataquality.business_rules (
    rule_id, regulation_id, rule_name, rule_code, description,
    rule_type, rule_category, severity, business_logic,
    execution_order, effective_date, enabled, created_at, updated_at
) VALUES (
    'DQ_VALIDITY_RISK_WEIGHT_RANGE',
    'BCBS_239_DATA_QUALITY',
    'Risk Weight Range',
    'VALIDITY_RISK_WEIGHT_RANGE',
    'Validates that risk weight is within valid range (0 to 1.5)',
    'VALIDITY',
    'REGULATORY_COMPLIANCE',
    'HIGH',
    '#riskWeight == null || (#riskWeight >= 0 && #riskWeight <= 1.5)',
    51,
    '2024-01-01',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (rule_id) DO NOTHING;

-- Rule 3: Maturity After Reporting
INSERT INTO dataquality.business_rules (
    rule_id, regulation_id, rule_name, rule_code, description,
    rule_type, rule_category, severity, business_logic,
    execution_order, effective_date, enabled, created_at, updated_at
) VALUES (
    'DQ_VALIDITY_MATURITY_AFTER_REPORTING',
    'BCBS_239_DATA_QUALITY',
    'Maturity After Reporting',
    'VALIDITY_MATURITY_AFTER_REPORTING',
    'Validates that maturity date is after or equal to reporting date',
    'VALIDITY',
    'BUSINESS_LOGIC',
    'MEDIUM',
    '#maturityDate == null || #reportingDate == null || !#maturityDate.isBefore(#reportingDate)',
    52,
    '2024-01-01',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (rule_id) DO NOTHING;

-- =====================================================
-- Migration Summary
-- =====================================================
-- Total Rules Created: 24
--   - Completeness: 8 rules (all enabled)
--   - Accuracy: 5 rules (all enabled)
--   - Timeliness: 3 rules (all enabled)
--   - Consistency: 3 rules (disabled - require custom implementation)
--   - Uniqueness: 2 rules (disabled - require batch-level implementation)
--   - Validity: 3 rules (all enabled)
-- Total Parameters: 6
-- =====================================================
