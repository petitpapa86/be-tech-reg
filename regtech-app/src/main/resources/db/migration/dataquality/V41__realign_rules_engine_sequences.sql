-- =====================================================================
-- Rules Engine Sequence Realignment (App migrations)
-- Version: 41
--
-- Fixes duplicate key violations on rule execution/violation IDs when using
-- Hibernate SEQUENCE pooled allocation (allocationSize=20).
--
-- Ensures:
--  - sequence INCREMENT matches allocationSize
--  - sequence value is set to at least MAX(id) so new IDs don't overlap
--  - first pooled block does not produce negative IDs on empty tables
-- =====================================================================

CREATE SCHEMA IF NOT EXISTS dataquality;

DO $$
DECLARE
  max_execution_id BIGINT;
  max_violation_id BIGINT;
BEGIN
  -- execution_id sequence
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
        -- Make nextval() return 20 so the first pooled block is 1..20.
        PERFORM setval('dataquality.rule_execution_log_execution_id_seq', 20, false);
      ELSE
        -- Make nextval() return a value > current max.
        PERFORM setval('dataquality.rule_execution_log_execution_id_seq', max_execution_id, true);
      END IF;
    END IF;
  END IF;

  -- violation_id sequence
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
        PERFORM setval('dataquality.rule_violations_violation_id_seq', 20, false);
      ELSE
        PERFORM setval('dataquality.rule_violations_violation_id_seq', max_violation_id, true);
      END IF;
    END IF;
  END IF;
END $$;
