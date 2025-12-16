-- Convert user_bank_assignments.id to UUID (safe/no-op if table missing or already UUID)
--
-- Fixes runtime error:
--   java.lang.ClassCastException: Cannot cast java.lang.String to java.util.UUID
-- when Hibernate reads a UUID-mapped column that is still VARCHAR/TEXT in PostgreSQL.

DO $$
BEGIN
	-- Convert primary key column
	IF to_regclass('iam.user_bank_assignments') IS NOT NULL AND EXISTS (
		SELECT 1
		FROM information_schema.columns
		WHERE table_schema = 'iam'
			AND table_name = 'user_bank_assignments'
			AND column_name = 'id'
			AND data_type <> 'uuid'
	) THEN
		ALTER TABLE iam.user_bank_assignments
			ALTER COLUMN id TYPE UUID USING id::uuid;
	END IF;

	-- Convert foreign key column (covers older databases)
	IF to_regclass('iam.user_bank_assignments') IS NOT NULL AND EXISTS (
		SELECT 1
		FROM information_schema.columns
		WHERE table_schema = 'iam'
			AND table_name = 'user_bank_assignments'
			AND column_name = 'user_id'
			AND data_type <> 'uuid'
	) THEN
		ALTER TABLE iam.user_bank_assignments
			ALTER COLUMN user_id TYPE UUID USING user_id::uuid;
	END IF;
END $$;
