-- V202512031001__Convert_refresh_tokens_user_id_to_uuid.sql
-- Convert refresh_tokens.user_id from VARCHAR to UUID (after users.id is converted)

ALTER TABLE iam.refresh_tokens ALTER COLUMN user_id TYPE UUID USING user_id::uuid;