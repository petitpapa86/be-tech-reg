-- V41__insert_initial_business_rules.sql
-- Populate the rules engine with default data quality rules for BCBS 239 compliance
-- Originally: V1.9__insert_initial_business_rules.sql

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
    'Valida che l''ID esposizione sia presente e non vuoto',
    'COMPLETENESS',
    'DATA_QUALITY',
    'CRITICAL',
    '#exposureId != null && !#exposureId.trim().isEmpty()',
    1,
    '2024-01-01',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

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
    'Valida che l''importo sia presente',
    'COMPLETENESS',
    'DATA_QUALITY',
    'CRITICAL',
    '#amount != null',
    2,
    '2024-01-01',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

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
    'Valida che la valuta sia presente e non vuota',
    'COMPLETENESS',
    'DATA_QUALITY',
    'CRITICAL',
    '#currency != null && !#currency.trim().isEmpty()',
    3,
    '2024-01-01',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

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
    'Valida che il paese sia presente e non vuoto',
    'COMPLETENESS',
    'DATA_QUALITY',
    'CRITICAL',
    '#country != null && !#country.trim().isEmpty()',
    4,
    '2024-01-01',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

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
    'Valida che il settore sia presente e non vuoto',
    'COMPLETENESS',
    'DATA_QUALITY',
    'CRITICAL',
    '#sector != null && !#sector.trim().isEmpty()',
    5,
    '2024-01-01',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

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
    'Valida che le esposizioni aziendali abbiano codici LEI',
    'COMPLETENESS',
    'REGULATORY_COMPLIANCE',
    'HIGH',
    '#sector != ''CORPORATE'' || (#leiCode != null && !#leiCode.trim().isEmpty())',
    6,
    '2024-01-01',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

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
    'Valida che le esposizioni a termine (non azionarie) abbiano date di scadenza',
    'COMPLETENESS',
    'BUSINESS_LOGIC',
    'HIGH',
    '#productType == ''EQUITY'' || #maturityDate != null',
    7,
    '2024-01-01',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

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
    'Valida che il rating interno sia presente per la valutazione del rischio',
    'COMPLETENESS',
    'REGULATORY_COMPLIANCE',
    'HIGH',
    '#internalRating != null && !#internalRating.trim().isEmpty()',
    8,
    '2024-01-01',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- Rule 9: Counterparty ID Required
INSERT INTO dataquality.business_rules (
    rule_id, regulation_id, rule_name, rule_code, description,
    rule_type, rule_category, severity, business_logic,
    execution_order, effective_date, enabled, created_at, updated_at
) VALUES (
    'DQ_COMPLETENESS_COUNTERPARTY_ID',
    'BCBS_239_DATA_QUALITY',
    'Counterparty ID Required',
    'COMPLETENESS_COUNTERPARTY_ID_REQUIRED',
    'Valida che l''ID controparte sia presente e non vuoto',
    'COMPLETENESS',
    'DATA_QUALITY',
    'CRITICAL',
    '#counterpartyId != null && !#counterpartyId.trim().isEmpty()',
    9,
    '2024-01-01',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

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
    'Valida che l''importo dell''esposizione sia positivo',
    'ACCURACY',
    'DATA_QUALITY',
    'CRITICAL',
    '#amount != null && #amount > 0',
    10,
    '2024-01-01',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

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
    'Valida la valuta rispetto all''elenco dei codici ISO 4217 validi',
    'ACCURACY',
    'DATA_QUALITY',
    'HIGH',
    '#currency == null || #validCurrencies.contains(#currency.toUpperCase())',
    11,
    '2024-01-01',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

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
    'Elenco separato da virgole dei codici valuta ISO 4217 validi',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

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
    'Valida il paese rispetto all''elenco dei codici ISO 3166 validi',
    'ACCURACY',
    'DATA_QUALITY',
    'HIGH',
    '#country == null || #validCountries.contains(#country.toUpperCase())',
    12,
    '2024-01-01',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

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
    'Elenco separato da virgole dei codici paese ISO 3166 validi',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

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
    'Valida che il codice LEI abbia il formato corretto (20 caratteri alfanumerici)',
    'ACCURACY',
    'REGULATORY_COMPLIANCE',
    'HIGH',
    '#leiCode == null || #leiCode.matches(''[A-Z0-9]{20}'')',
    13,
    '2024-01-01',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

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
    'Valida che l''importo dell''esposizione sia entro limiti ragionevoli (10 miliardi EUR)',
    'ACCURACY',
    'BUSINESS_LOGIC',
    'MEDIUM',
    '#amount == null || #amount < #maxReasonableAmount',
    14,
    '2024-01-01',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

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
    'Importo massimo ragionevole dell''esposizione (10 miliardi EUR)',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

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
    'Valida che la data di segnalazione sia entro il periodo accettabile (non più vecchia dell''età massima)',
    'TIMELINESS',
    'REGULATORY_COMPLIANCE',
    'HIGH',
    '#reportingDate == null || T(java.time.temporal.ChronoUnit).DAYS.between(#reportingDate, T(java.time.LocalDate).now()) <= #maxReportingAgeDays',
    30,
    '2024-01-01',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

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
    'Età massima per i dati di segnalazione (90 giorni)',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

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
    'Valida che la data di segnalazione non sia nel futuro',
    'TIMELINESS',
    'DATA_QUALITY',
    'CRITICAL',
    '#reportingDate == null || !#reportingDate.isAfter(T(java.time.LocalDate).now())',
    31,
    '2024-01-01',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

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
    'Valida che la data di valutazione sia abbastanza recente (non più vecchia di 30 giorni)',
    'TIMELINESS',
    'BUSINESS_LOGIC',
    'MEDIUM',
    '#valuationDate == null || T(java.time.temporal.ChronoUnit).DAYS.between(#valuationDate, T(java.time.LocalDate).now()) <= 30',
    32,
    '2024-01-01',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

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
    'Valida che la valuta corrisponda al paese (es. EUR per i paesi dell''Eurozona)',
    'CONSISTENCY',
    'BUSINESS_LOGIC',
    'MEDIUM',
    'true',
    20,
    '2024-01-01',
    false,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

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
    'Valida che la classificazione del settore sia coerente con il tipo di controparte',
    'CONSISTENCY',
    'BUSINESS_LOGIC',
    'MEDIUM',
    'true',
    21,
    '2024-01-01',
    false,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

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
    'Valida che il rating interno sia coerente con la categoria di rischio',
    'CONSISTENCY',
    'BUSINESS_LOGIC',
    'MEDIUM',
    'true',
    22,
    '2024-01-01',
    false,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

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
    'Valida che tutti gli ID esposizione all''interno di un batch siano univoci (controllo a livello batch)',
    'UNIQUENESS',
    'DATA_QUALITY',
    'CRITICAL',
    'true',
    40,
    '2024-01-01',
    false,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

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
    'Valida che le coppie controparte-esposizione siano univoche all''interno di un batch (controllo a livello batch)',
    'UNIQUENESS',
    'DATA_QUALITY',
    'HIGH',
    'true',
    41,
    '2024-01-01',
    false,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

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
    'Valida che il settore sia nell''elenco dei settori validi',
    'VALIDITY',
    'DATA_QUALITY',
    'HIGH',
    '#sector == null || #validSectors.contains(#sector.toUpperCase())',
    50,
    '2024-01-01',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

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
    'Elenco separato da virgole dei codici settore validi',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

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
    'Valida che il peso del rischio sia entro l''intervallo valido (da 0 a 1,5)',
    'VALIDITY',
    'REGULATORY_COMPLIANCE',
    'HIGH',
    '#riskWeight == null || (#riskWeight >= 0 && #riskWeight <= 1.5)',
    51,
    '2024-01-01',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

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
    'Valida che la data di scadenza sia successiva o uguale alla data di segnalazione',
    'VALIDITY',
    'BUSINESS_LOGIC',
    'MEDIUM',
    '#maturityDate == null || #reportingDate == null || !#maturityDate.isBefore(#reportingDate)',
    52,
    '2024-01-01',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

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
