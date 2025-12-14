-- Convert user_roles.user_id to UUID (safe/no-op if already UUID)
DO $$
BEGIN
	IF EXISTS (
		SELECT 1
		FROM information_schema.columns
		WHERE table_schema = 'iam'
			AND table_name = 'user_roles'
			AND column_name = 'user_id'
			AND data_type <> 'uuid'
	) THEN
		ALTER TABLE iam.user_roles ALTER COLUMN user_id TYPE UUID USING user_id::uuid;
	END IF;
END $$;