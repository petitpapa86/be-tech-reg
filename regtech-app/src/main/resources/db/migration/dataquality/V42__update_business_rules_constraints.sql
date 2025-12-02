-- V42__update_business_rules_constraints.sql
-- Update the check constraints on the business_rules table to support data quality rule types
-- Originally: V1.8.1__update_business_rules_constraints.sql

-- Drop the old constraint on rule_type
ALTER TABLE dataquality.business_rules 
DROP CONSTRAINT IF EXISTS business_rules_rule_type_check;

-- Add new constraint with data quality rule types
ALTER TABLE dataquality.business_rules 
ADD CONSTRAINT business_rules_rule_type_check 
CHECK (rule_type IN ('COMPLETENESS', 'ACCURACY', 'CONSISTENCY', 'TIMELINESS', 'VALIDITY', 'UNIQUENESS'));

-- Drop the old constraint on rule_category if it exists
ALTER TABLE dataquality.business_rules 
DROP CONSTRAINT IF EXISTS business_rules_rule_category_check;

-- Add new constraint for rule_category
ALTER TABLE dataquality.business_rules 
ADD CONSTRAINT business_rules_rule_category_check 
CHECK (rule_category IN ('DATA_QUALITY', 'BUSINESS_LOGIC', 'REGULATORY_COMPLIANCE'));
