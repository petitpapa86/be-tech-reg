-- =====================================================================
-- Rules Engine Sequence Realignment
-- Version: 1.12
--
-- Fixes duplicate key violations on rule execution/violation IDs when the
-- underlying database sequences drift behind existing table data.
--
-- This can happen after manual data loads, restores, or ad-hoc inserts.
-- =====================================================================

CREATE SCHEMA IF NOT EXISTS dataquality;

DO $$
DECLARE
  max_execution_id BIGINT;
  max_violation_id BIGINT;
BEGIN
  -- Ensure increments match Hibernate's allocationSize (see V1.11)
  IF EXISTS (
      SELECT 1
      FROM pg_class c
      JOIN pg_namespace n ON n.oid = c.relnamespace
      WHERE c.relkind = 'S'
        AND n.nspname = 'dataquality'
        AND c.relname = 'rule_execution_log_execution_id_seq'
  ) THEN
    EXECUTE 'ALTER SEQUENCE dataquality.rule_execution_log_execution_id_seq INCREMENT BY 20';

    IF to_regclass('dataquality.rule_execution_log') IS NOT NULL THEN
      SELECT MAX(execution_id) INTO max_execution_id
      FROM dataquality.rule_execution_log;

      IF max_execution_id IS NULL THEN
        -- Empty table:
        -- With Hibernate pooled sequence optimizer (allocationSize=20), if the first sequence value is 1,
        -- Hibernate can generate a first block containing negative IDs.
        -- Make nextval() return 20 so the first generated block is 1..20.
        PERFORM setval('dataquality.rule_execution_log_execution_id_seq', 20, false);
      ELSE
        -- Non-empty table: make nextval() return a value > current max
        PERFORM setval('dataquality.rule_execution_log_execution_id_seq', max_execution_id, true);
      END IF;
    END IF;
  END IF;

  IF EXISTS (
      SELECT 1
      FROM pg_class c
      JOIN pg_namespace n ON n.oid = c.relnamespace
      WHERE c.relkind = 'S'
        AND n.nspname = 'dataquality'
        AND c.relname = 'rule_violations_violation_id_seq'
  ) THEN
    EXECUTE 'ALTER SEQUENCE dataquality.rule_violations_violation_id_seq INCREMENT BY 20';

    IF to_regclass('dataquality.rule_violations') IS NOT NULL THEN
      SELECT MAX(violation_id) INTO max_violation_id
      FROM dataquality.rule_violations;

      IF max_violation_id IS NULL THEN
        -- See note above: avoid negative IDs on first pooled block.
        PERFORM setval('dataquality.rule_violations_violation_id_seq', 20, false);
      ELSE
        PERFORM setval('dataquality.rule_violations_violation_id_seq', max_violation_id, true);
      END IF;
    END IF;
  END IF;
END $$;
