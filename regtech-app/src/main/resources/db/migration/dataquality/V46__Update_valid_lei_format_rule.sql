-- V46__Update_valid_lei_format_rule.sql
-- Keep "Valid LEI Format" validation strict (20 uppercase alphanumeric characters).

UPDATE dataquality.business_rules
SET
    business_logic = '#leiCode == null || #leiCode.matches(''[A-Z0-9]{20}'')',
    description = 'Validates that LEI code has correct format (20 alphanumeric characters)'
WHERE rule_id = 'DQ_ACCURACY_VALID_LEI_FORMAT';
