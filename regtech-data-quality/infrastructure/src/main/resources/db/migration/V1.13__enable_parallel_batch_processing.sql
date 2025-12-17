-- ============================================================================
-- Schema Migration: Enable Parallel Batch Processing
--
-- Based on requested changes:
-- - Remove blocking FK from rule_violations.execution_id -> rule_execution_log.execution_id
-- - Make execution_id nullable
-- - Add batch_id columns to violations + execution logs
-- - Rename rule_execution_log.context_data -> context_json
-- - Add supporting indexes
-- - Increase sequence cache for batch operations
-- ============================================================================

BEGIN;

-- 1) Remove the blocking foreign key constraint
ALTER TABLE dataquality.rule_violations
    DROP CONSTRAINT IF EXISTS fk_violation_execution;

-- 2) Make execution_id optional (nullable)
ALTER TABLE dataquality.rule_violations
    ALTER COLUMN execution_id DROP NOT NULL;

-- 3) Add batch_id for better tracking
ALTER TABLE dataquality.rule_violations
    ADD COLUMN IF NOT EXISTS batch_id VARCHAR(100);

ALTER TABLE dataquality.rule_execution_log
    ADD COLUMN IF NOT EXISTS batch_id VARCHAR(100);

-- 4) Rename context_data to context_json for consistency (safe/conditional)
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'dataquality'
          AND table_name = 'rule_execution_log'
          AND column_name = 'context_data'
    ) AND NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'dataquality'
          AND table_name = 'rule_execution_log'
          AND column_name = 'context_json'
    ) THEN
        ALTER TABLE dataquality.rule_execution_log
            RENAME COLUMN context_data TO context_json;
    END IF;
END $$;

-- 5) Add indexes for batch queries
CREATE INDEX IF NOT EXISTS idx_violations_batch
    ON dataquality.rule_violations(batch_id)
    WHERE batch_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_execution_batch
    ON dataquality.rule_execution_log(batch_id)
    WHERE batch_id IS NOT NULL;

-- 6) Optional index for joining violations to logs (when execution_id exists)
CREATE INDEX IF NOT EXISTS idx_violations_execution_optional
    ON dataquality.rule_violations(execution_id)
    WHERE execution_id IS NOT NULL;

-- 7) Optimize sequences for batch operations (increase cache)
ALTER SEQUENCE IF EXISTS dataquality.rule_execution_log_execution_id_seq
    CACHE 1000;

ALTER SEQUENCE IF EXISTS dataquality.rule_violations_violation_id_seq
    CACHE 1000;

-- 8) Composite index for common query patterns
CREATE INDEX IF NOT EXISTS idx_violations_entity_batch
    ON dataquality.rule_violations(entity_type, entity_id, batch_id);

-- 9) Update column comments
COMMENT ON COLUMN dataquality.rule_violations.execution_id IS
    'Optional reference to rule_execution_log. NULL for batch validations where individual execution tracking is not required.';

COMMENT ON COLUMN dataquality.rule_violations.batch_id IS
    'Batch identifier for tracking which validation run created this violation.';

COMMENT ON COLUMN dataquality.rule_execution_log.batch_id IS
    'Batch identifier for tracking which validation run created this execution log.';

COMMENT ON COLUMN dataquality.rule_execution_log.context_json IS
    'Execution context captured as JSON.';

-- 10) Update planner stats
ANALYZE dataquality.rule_violations;
ANALYZE dataquality.rule_execution_log;

COMMIT;
