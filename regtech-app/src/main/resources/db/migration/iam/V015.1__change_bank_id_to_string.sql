-- Change bank_id column from BIGINT to VARCHAR to support non-numeric IDs
-- Migration for IAM schema

-- First, drop the foreign key in iam.users to allow type change
ALTER TABLE iam.users DROP CONSTRAINT IF EXISTS fk_users_bank_id;

-- Change type of bank_id column in iam.users
-- Note: iam.banks.id is already VARCHAR(36) from V12, so no change needed there
ALTER TABLE iam.users ALTER COLUMN bank_id TYPE VARCHAR(50);

-- Update seed data to use a proper string ID if it was still using numeric
-- Note: This updates iam.banks, not bank_profile (which doesn't exist)
UPDATE iam.banks SET id = 'BANK-001' WHERE id = '1';
UPDATE iam.users SET bank_id = 'BANK-001' WHERE bank_id = '1';

-- Re-add the foreign key constraint
ALTER TABLE iam.users
ADD CONSTRAINT fk_users_bank_id 
    FOREIGN KEY (bank_id) 
    REFERENCES iam.banks(id) 
    ON DELETE RESTRICT;

-- Update comments
COMMENT ON COLUMN iam.banks.id IS 'Unique identifier for the bank (proper string, can contain non-digits)';
