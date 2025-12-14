-- Add missing OAuth ID columns expected by JPA mappings
-- Fixes runtime failure: column iam.users.facebook_id does not exist

ALTER TABLE iam.users
    ADD COLUMN IF NOT EXISTS google_id VARCHAR(255),
    ADD COLUMN IF NOT EXISTS facebook_id VARCHAR(255);

-- Current JPA mapping expects a 'status' column (Enum string). Existing schema uses 'is_active'.
-- Add 'status' for forward compatibility; keep existing 'is_active' unchanged.
ALTER TABLE iam.users
    ADD COLUMN IF NOT EXISTS status VARCHAR(50);

CREATE INDEX IF NOT EXISTS idx_users_google_id ON iam.users (google_id);
CREATE INDEX IF NOT EXISTS idx_users_facebook_id ON iam.users (facebook_id);
