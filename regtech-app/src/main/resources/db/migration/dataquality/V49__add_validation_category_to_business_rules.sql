-- V65__add_validation_category_to_business_rules.sql
-- Add validation_category column to business_rules table and populate with categories

-- Add validation_category column
ALTER TABLE dataquality.business_rules 
ADD COLUMN IF NOT EXISTS validation_category VARCHAR(50);

-- Update existing rules with appropriate categories based on rule_code patterns

-- DATA_QUALITY category - Completeness rules
UPDATE dataquality.business_rules 
SET validation_category = 'DATA_QUALITY'
WHERE rule_code LIKE 'COMPLETENESS_%'
  AND validation_category IS NULL;

-- NUMERIC_RANGES category - Amount validations
UPDATE dataquality.business_rules 
SET validation_category = 'NUMERIC_RANGES'
WHERE (
    rule_code LIKE '%POSITIVE_AMOUNT%' OR 
    rule_code LIKE '%REASONABLE_AMOUNT%' OR
    rule_code LIKE 'VALIDITY_POSITIVE_EXPOSURE_AMOUNT'
)
AND validation_category IS NULL;

-- CODE_VALIDATION category - Format validations
UPDATE dataquality.business_rules 
SET validation_category = 'CODE_VALIDATION'
WHERE (
    rule_code LIKE '%VALID_CURRENCY%' OR 
    rule_code LIKE '%VALID_COUNTRY%' OR 
    rule_code LIKE '%VALID_LEI%' OR
    rule_code LIKE '%LEI_FORMAT%' OR
    rule_code LIKE '%RATING_FORMAT%' OR
    rule_code LIKE '%VALID_SECTOR%' OR
    rule_code = 'ACCURACY_VALID_CURRENCY_CODE' OR
    rule_code = 'ACCURACY_VALID_COUNTRY_CODE' OR
    rule_code = 'ACCURACY_VALID_LEI_FORMAT' OR
    rule_code = 'VALIDITY_RATING_FORMAT' OR
    rule_code = 'VALIDITY_VALID_SECTOR'
)
AND validation_category IS NULL;

-- TEMPORAL_COHERENCE category - Date and time validations
UPDATE dataquality.business_rules 
SET validation_category = 'TEMPORAL_COHERENCE'
WHERE (
    rule_code LIKE 'TIMELINESS_%' OR 
    rule_code LIKE '%MATURITY%' OR 
    rule_code LIKE '%DATE%' OR
    rule_code LIKE '%FUTURE_%' OR
    rule_code = 'VALIDITY_FUTURE_MATURITY_DATE'
)
AND validation_category IS NULL;

-- DUPLICATE_DETECTION category
UPDATE dataquality.business_rules 
SET validation_category = 'DUPLICATE_DETECTION'
WHERE (
    rule_code LIKE '%DUPLICATE%' OR 
    rule_code LIKE '%UNIQUE%'
)
AND validation_category IS NULL;

-- CROSS_REFERENCE category
UPDATE dataquality.business_rules 
SET validation_category = 'CROSS_REFERENCE'
WHERE (
    rule_code LIKE '%CROSS%' OR 
    rule_code LIKE '%REFERENCE%'
)
AND validation_category IS NULL;

-- Default category for any remaining rules
UPDATE dataquality.business_rules 
SET validation_category = 'DATA_QUALITY'
WHERE validation_category IS NULL;

-- Add index for better query performance
CREATE INDEX IF NOT EXISTS idx_business_rules_validation_category 
ON dataquality.business_rules(validation_category);

-- Add comment to document the column
COMMENT ON COLUMN dataquality.business_rules.validation_category IS 
'Validation rule category for UI organization: DATA_QUALITY, NUMERIC_RANGES, CODE_VALIDATION, TEMPORAL_COHERENCE, DUPLICATE_DETECTION, CROSS_REFERENCE';
