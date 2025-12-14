-- DEPRECATED MIGRATION
--
-- This file was created with an incorrect Flyway version (a timestamp-like number).
-- The real migration is `V62__Convert_iam_user_ids_to_uuid.sql`.
--
-- We intentionally keep this file as a NO-OP to avoid accidentally applying
-- schema-changing statements in the future.

DO $$
BEGIN
    RAISE NOTICE 'DEPRECATED: V202512141510__Convert_iam_user_ids_to_uuid.sql is a no-op. Use V62__Convert_iam_user_ids_to_uuid.sql';
END $$;
