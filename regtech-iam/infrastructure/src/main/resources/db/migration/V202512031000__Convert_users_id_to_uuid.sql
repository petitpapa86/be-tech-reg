-- V202512031000__Convert_users_id_to_uuid.sql
-- Convert users.id from VARCHAR to UUID

-- First, ensure all existing id values are valid UUIDs (they should be since they're generated as such)
-- If any are invalid, this will fail - but in our case, they should be valid

ALTER TABLE iam.users ALTER COLUMN id TYPE UUID USING id::uuid;

-- Update any references if needed (but there shouldn't be direct string comparisons)