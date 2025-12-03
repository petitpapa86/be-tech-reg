-- V12__Create_refresh_tokens_table.sql
-- Create refresh_tokens table for OAuth 2.0 refresh token management
-- Originally: V202511242100__Create_refresh_tokens_table.sql

-- Create refresh_tokens table
CREATE TABLE IF NOT EXISTS iam.refresh_tokens (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    revoked_at TIMESTAMP,
    CONSTRAINT fk_refresh_tokens_user_id 
        FOREIGN KEY (user_id) 
        REFERENCES iam.users (id) 
        ON DELETE CASCADE
);

-- Create indexes for efficient queries
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_id 
    ON iam.refresh_tokens (user_id);

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_token_hash 
    ON iam.refresh_tokens (token_hash);

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_expires_at 
    ON iam.refresh_tokens (expires_at);

-- Create partial index for active (non-revoked) tokens
-- This improves query performance when looking up valid tokens
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_active 
    ON iam.refresh_tokens (user_id, revoked) 
    WHERE revoked = FALSE;

-- Add comment to table for documentation
COMMENT ON TABLE iam.refresh_tokens IS 'Stores OAuth 2.0 refresh tokens with token rotation support';
COMMENT ON COLUMN iam.refresh_tokens.id IS 'Unique identifier for the refresh token (UUID)';
COMMENT ON COLUMN iam.refresh_tokens.user_id IS 'Reference to the user who owns this token';
COMMENT ON COLUMN iam.refresh_tokens.token_hash IS 'BCrypt hash of the refresh token value';
COMMENT ON COLUMN iam.refresh_tokens.expires_at IS 'Timestamp when the token expires';
COMMENT ON COLUMN iam.refresh_tokens.created_at IS 'Timestamp when the token was created';
COMMENT ON COLUMN iam.refresh_tokens.revoked IS 'Flag indicating if the token has been revoked';
COMMENT ON COLUMN iam.refresh_tokens.revoked_at IS 'Timestamp when the token was revoked (null if not revoked)';
