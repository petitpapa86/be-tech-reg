-- V202512031002__Convert_refresh_tokens_id_to_uuid.sql
-- Convert refresh_tokens.id from VARCHAR to UUID

ALTER TABLE iam.refresh_tokens ALTER COLUMN id TYPE UUID USING id::uuid;