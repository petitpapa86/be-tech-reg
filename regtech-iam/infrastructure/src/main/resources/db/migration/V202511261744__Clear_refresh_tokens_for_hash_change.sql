-- Migration to clear existing refresh tokens due to hash algorithm change
-- Previous tokens used BCrypt (salted, non-deterministic)
-- New tokens use SHA-256 (deterministic, allows lookup)
-- Existing tokens cannot be migrated, so we clear them
-- Users will need to log in again to get new refresh tokens

DELETE FROM refresh_tokens;

-- Add comment to table documenting the hash algorithm
COMMENT ON COLUMN refresh_tokens.token_hash IS 'SHA-256 hash of the refresh token value (Base64 encoded)';
