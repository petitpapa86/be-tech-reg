-- Ensure sequences used by Hibernate pooled optimizer advance by the allocation size.
--
-- With @SequenceGenerator(allocationSize = 20), Hibernate uses a pooled optimizer that
-- assumes the database sequence INCREMENT matches the allocation size.
-- If the DB sequence increment is 1, ID ranges overlap and can produce duplicate IDs,
-- leading to NonUniqueObjectException / persistence-context conflicts.

DO $$
BEGIN
    -- Execution log IDs
    IF EXISTS (
        SELECT 1
        FROM pg_class c
        JOIN pg_namespace n ON n.oid = c.relnamespace
        WHERE c.relkind = 'S'
          AND n.nspname = 'dataquality'
          AND c.relname = 'rule_execution_log_execution_id_seq'
    ) THEN
        EXECUTE 'ALTER SEQUENCE dataquality.rule_execution_log_execution_id_seq INCREMENT BY 20';
    END IF;

    -- Rule violation IDs
    IF EXISTS (
        SELECT 1
        FROM pg_class c
        JOIN pg_namespace n ON n.oid = c.relnamespace
        WHERE c.relkind = 'S'
          AND n.nspname = 'dataquality'
          AND c.relname = 'rule_violations_violation_id_seq'
    ) THEN
        EXECUTE 'ALTER SEQUENCE dataquality.rule_violations_violation_id_seq INCREMENT BY 20';
    END IF;
END $$;
