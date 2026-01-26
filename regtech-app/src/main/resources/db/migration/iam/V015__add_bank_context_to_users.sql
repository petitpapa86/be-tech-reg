-- Add multi-tenancy context to existing iam.users table
-- DO NOT create new users table - extend existing one

-- 1. Add bank_id for multi-tenancy
ALTER TABLE iam.users 
ADD COLUMN IF NOT EXISTS bank_id VARCHAR(50);

-- Add foreign key to iam.banks (not bank_profile)
ALTER TABLE iam.users
ADD CONSTRAINT fk_users_bank_id 
    FOREIGN KEY (bank_id) 
    REFERENCES iam.banks(id) 
    ON DELETE RESTRICT;

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_users_bank_id ON iam.users(bank_id);

-- 2. Update email uniqueness: per bank, not globally
-- Drop constraint first (which automatically drops the associated index)
ALTER TABLE iam.users DROP CONSTRAINT IF EXISTS users_email_key;
CREATE UNIQUE INDEX IF NOT EXISTS idx_users_email_bank_id ON iam.users(email, bank_id);

-- 3. Backfill existing users with default bank_id
UPDATE iam.users SET bank_id = 'BANK-001' WHERE bank_id IS NULL;
ALTER TABLE iam.users ALTER COLUMN bank_id SET NOT NULL;

-- 4. Add invitation workflow columns
ALTER TABLE iam.users 
ADD COLUMN IF NOT EXISTS invitation_token VARCHAR(64),
ADD COLUMN IF NOT EXISTS invited_at TIMESTAMP,
ADD COLUMN IF NOT EXISTS invited_by VARCHAR(100),
ADD COLUMN IF NOT EXISTS last_access TIMESTAMP,
ADD COLUMN IF NOT EXISTS status VARCHAR(50);

-- Backfill status for existing users
UPDATE iam.users SET status = 'ACTIVE' WHERE status IS NULL;

-- 5. Add constraint: PENDING_PAYMENT status means invitation pending
ALTER TABLE iam.users
ADD CONSTRAINT chk_pending_invitation 
    CHECK (
        (status = 'PENDING_PAYMENT' AND invitation_token IS NOT NULL) OR
        (status != 'PENDING_PAYMENT')
    );

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_users_invitation_token ON iam.users(invitation_token) WHERE invitation_token IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_users_bank_status ON iam.users(bank_id, status);

-- Comments
COMMENT ON COLUMN iam.users.bank_id IS 'Bank context - user belongs to one bank (multi-tenancy)';
COMMENT ON COLUMN iam.users.invitation_token IS 'Secure token for pending user invitation (status = PENDING_PAYMENT)';
COMMENT ON COLUMN iam.users.last_access IS 'Last successful login timestamp';
