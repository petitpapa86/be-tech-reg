-- Convert user_roles.user_id to UUID
ALTER TABLE iam.user_roles ALTER COLUMN user_id TYPE UUID USING user_id::uuid;