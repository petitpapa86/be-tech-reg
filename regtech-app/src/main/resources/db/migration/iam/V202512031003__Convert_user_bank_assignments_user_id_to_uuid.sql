-- Convert user_bank_assignments.user_id to UUID (safe/no-op if table missing or already UUID)
DO $$
BEGIN
	IF to_regclass('iam.user_bank_assignments') IS NOT NULL AND EXISTS (
		SELECT 1
		FROM information_schema.columns
		WHERE table_schema = 'iam'
			AND table_name = 'user_bank_assignments'
			AND column_name = 'user_id'
			AND data_type <> 'uuid'
	) THEN
		ALTER TABLE iam.user_bank_assignments ALTER COLUMN user_id TYPE UUID USING user_id::uuid;
	END IF;
END $$;