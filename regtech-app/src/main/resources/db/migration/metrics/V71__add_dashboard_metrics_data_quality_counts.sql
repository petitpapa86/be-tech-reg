-- Add data-quality exposure/error counts to metrics.dashboard_metrics

ALTER TABLE IF EXISTS metrics.dashboard_metrics
    ADD COLUMN IF NOT EXISTS total_exposures INTEGER;

ALTER TABLE IF EXISTS metrics.dashboard_metrics
    ADD COLUMN IF NOT EXISTS valid_exposures INTEGER;

ALTER TABLE IF EXISTS metrics.dashboard_metrics
    ADD COLUMN IF NOT EXISTS total_errors INTEGER;

UPDATE metrics.dashboard_metrics
SET total_exposures = COALESCE(total_exposures, 0)
WHERE total_exposures IS NULL;

UPDATE metrics.dashboard_metrics
SET valid_exposures = COALESCE(valid_exposures, 0)
WHERE valid_exposures IS NULL;

UPDATE metrics.dashboard_metrics
SET total_errors = COALESCE(total_errors, 0)
WHERE total_errors IS NULL;

ALTER TABLE IF EXISTS metrics.dashboard_metrics
    ALTER COLUMN total_exposures SET DEFAULT 0;

ALTER TABLE IF EXISTS metrics.dashboard_metrics
    ALTER COLUMN valid_exposures SET DEFAULT 0;

ALTER TABLE IF EXISTS metrics.dashboard_metrics
    ALTER COLUMN total_errors SET DEFAULT 0;
