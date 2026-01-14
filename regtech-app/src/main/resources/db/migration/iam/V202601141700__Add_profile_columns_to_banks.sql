-- Add missing profile columns to iam.banks table
-- Columns are nullable - domain validation handles required fields

ALTER TABLE iam.banks ADD COLUMN IF NOT EXISTS legal_name VARCHAR(255);
ALTER TABLE iam.banks ADD COLUMN IF NOT EXISTS abi_code VARCHAR(5);
ALTER TABLE iam.banks ADD COLUMN IF NOT EXISTS lei_code VARCHAR(20);
ALTER TABLE iam.banks ADD COLUMN IF NOT EXISTS group_type VARCHAR(50);
ALTER TABLE iam.banks ADD COLUMN IF NOT EXISTS bank_type VARCHAR(50);
ALTER TABLE iam.banks ADD COLUMN IF NOT EXISTS supervision_category VARCHAR(50);
ALTER TABLE iam.banks ADD COLUMN IF NOT EXISTS legal_address TEXT;
ALTER TABLE iam.banks ADD COLUMN IF NOT EXISTS vat_number VARCHAR(13);
ALTER TABLE iam.banks ADD COLUMN IF NOT EXISTS tax_code VARCHAR(11);
ALTER TABLE iam.banks ADD COLUMN IF NOT EXISTS company_registry VARCHAR(100);
ALTER TABLE iam.banks ADD COLUMN IF NOT EXISTS institutional_email VARCHAR(255);
ALTER TABLE iam.banks ADD COLUMN IF NOT EXISTS pec VARCHAR(255);
ALTER TABLE iam.banks ADD COLUMN IF NOT EXISTS phone VARCHAR(50);
ALTER TABLE iam.banks ADD COLUMN IF NOT EXISTS website VARCHAR(255);

-- Step 4: Create unique constraints for business identifiers
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'banks_abi_code_key' AND conrelid = 'iam.banks'::regclass
    ) THEN
        ALTER TABLE iam.banks ADD CONSTRAINT banks_abi_code_key UNIQUE (abi_code);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'banks_lei_code_key' AND conrelid = 'iam.banks'::regclass
    ) THEN
        ALTER TABLE iam.banks ADD CONSTRAINT banks_lei_code_key UNIQUE (lei_code);
    END IF;
END $$;

-- Add comment
COMMENT ON TABLE iam.banks IS 'Banks table including institutional profile for BCBS 239 compliance';
