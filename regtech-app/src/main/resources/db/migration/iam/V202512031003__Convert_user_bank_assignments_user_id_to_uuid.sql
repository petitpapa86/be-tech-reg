-- Convert user_bank_assignments.user_id to UUID
ALTER TABLE iam.user_bank_assignments ALTER COLUMN user_id TYPE UUID USING user_id::uuid;