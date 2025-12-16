-- =====================================================================
-- Rules Engine Sequences Alignment (Batch Inserts)
-- Version: 1.10
-- Description:
--   Ensure BIGSERIAL-backed sequences exist and are correctly owned/defaulted.
--   This supports Hibernate SEQUENCE ID generation, which enables JDBC batching.
-- =====================================================================

-- Ensure schema exists
CREATE SCHEMA IF NOT EXISTS dataquality;

-- ---------------------------------------------------------------------
-- rule_execution_log.execution_id
-- ---------------------------------------------------------------------
DO $$
BEGIN
  -- Create sequence if missing (BIGSERIAL would normally create it)
  IF NOT EXISTS (
    SELECT 1
    FROM pg_class c
    JOIN pg_namespace n ON n.oid = c.relnamespace
    WHERE c.relkind = 'S'
      AND n.nspname = 'dataquality'
      AND c.relname = 'rule_execution_log_execution_id_seq'
  ) THEN
    CREATE SEQUENCE dataquality.rule_execution_log_execution_id_seq;
  END IF;

  -- Ensure the column uses the expected sequence
  ALTER TABLE dataquality.rule_execution_log
    ALTER COLUMN execution_id SET DEFAULT nextval('dataquality.rule_execution_log_execution_id_seq'::regclass);

  -- Ensure ownership links sequence lifecycle to the column
  ALTER SEQUENCE dataquality.rule_execution_log_execution_id_seq
    OWNED BY dataquality.rule_execution_log.execution_id;
END $$;

-- ---------------------------------------------------------------------
-- rule_violations.violation_id
-- ---------------------------------------------------------------------
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM pg_class c
    JOIN pg_namespace n ON n.oid = c.relnamespace
    WHERE c.relkind = 'S'
      AND n.nspname = 'dataquality'
      AND c.relname = 'rule_violations_violation_id_seq'
  ) THEN
    CREATE SEQUENCE dataquality.rule_violations_violation_id_seq;
  END IF;

  ALTER TABLE dataquality.rule_violations
    ALTER COLUMN violation_id SET DEFAULT nextval('dataquality.rule_violations_violation_id_seq'::regclass);

  ALTER SEQUENCE dataquality.rule_violations_violation_id_seq
    OWNED BY dataquality.rule_violations.violation_id;
END $$;
