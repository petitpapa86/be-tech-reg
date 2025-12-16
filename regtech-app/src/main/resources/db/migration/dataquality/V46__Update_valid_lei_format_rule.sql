-- V46__Update_valid_lei_format_rule.sql
-- Make "Valid LEI Format" validation case-insensitive without transforming stored data.

UPDATE dataquality.business_rules
SET
    business_logic = '#leiCode == null || #leiCode.trim().matches(''[A-Za-z0-9]{20}'')',
    description = 'Validates that LEI code has correct format (20 alphanumeric characters)'
WHERE rule_id = 'DQ_ACCURACY_VALID_LEI_FORMAT';
