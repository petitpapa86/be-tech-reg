-- Evolve dashboard metrics from global singleton to per-bank/per-month rows

CREATE SCHEMA IF NOT EXISTS metrics;

-- Add new key columns if missing
ALTER TABLE IF EXISTS metrics.dashboard_metrics
    ADD COLUMN IF NOT EXISTS bank_id VARCHAR(255);

ALTER TABLE IF EXISTS metrics.dashboard_metrics
    ADD COLUMN IF NOT EXISTS period_start DATE;

-- Backfill existing rows (if any) so we can apply NOT NULL and primary key
UPDATE metrics.dashboard_metrics
SET bank_id = COALESCE(bank_id, 'UNKNOWN')
WHERE bank_id IS NULL;

UPDATE metrics.dashboard_metrics
SET period_start = COALESCE(period_start, date_trunc('month', CURRENT_DATE)::date)
WHERE period_start IS NULL;

-- Drop old primary key on id (if present), then drop the id column (if present)
ALTER TABLE IF EXISTS metrics.dashboard_metrics
    DROP CONSTRAINT IF EXISTS dashboard_metrics_pkey;

ALTER TABLE IF EXISTS metrics.dashboard_metrics
    DROP COLUMN IF EXISTS id;

-- Ensure key columns are non-null and use composite primary key
ALTER TABLE IF EXISTS metrics.dashboard_metrics
    ALTER COLUMN bank_id SET NOT NULL;

ALTER TABLE IF EXISTS metrics.dashboard_metrics
    ALTER COLUMN period_start SET NOT NULL;

ALTER TABLE IF EXISTS metrics.dashboard_metrics
    ADD CONSTRAINT dashboard_metrics_pkey PRIMARY KEY (bank_id, period_start);
