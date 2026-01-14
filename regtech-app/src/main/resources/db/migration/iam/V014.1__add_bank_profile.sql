-- Update banks table with initial profile data
-- (Table was updated in V12)

-- Insert initial bank profile data into existing bank
-- First ensure the bank exists
INSERT INTO iam.banks (id, name, country_code, status)
SELECT 'BANK-001', 'Banca Italiana SpA', 'IT', 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM iam.banks WHERE id = 'BANK-001');

-- Update the bank with profile information
UPDATE iam.banks SET
    legal_name = 'Banca Italiana SpA',
    abi_code = '12345',
    lei_code = '1234567890ABCDEFGHIJ',
    group_type = 'NATIONAL_GROUP',
    bank_type = 'COMMERCIAL',
    supervision_category = 'SIGNIFICANT_SSM',
    legal_address = 'Via Roma 1, 00100 Roma, Italia',
    vat_number = 'IT12345678901',
    tax_code = '12345678901',
    company_registry = 'REA RM-1234567',
    institutional_email = 'info@bancaitaliana.it',
    pec = 'pec@bancaitaliana.it',
    phone = '+39 06 12345678',
    website = 'https://www.bancaitaliana.it',
    updated_at = CURRENT_TIMESTAMP
WHERE id = 'BANK-001';

-- Add comment
COMMENT ON TABLE iam.banks IS 'Banks table including institutional profile for BCBS 239 compliance';
