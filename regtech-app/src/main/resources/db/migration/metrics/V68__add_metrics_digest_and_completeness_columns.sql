-- V68__add_metrics_digest_and_completeness_columns.sql
-- Adds persisted TDigest sketches for dashboard medians and file completeness
-- NOTE: Uses PostgreSQL `bytea` for serialized sketches; completeness_score is a numeric snapshot.

-- Add digest columns to dashboard_metrics
ALTER TABLE IF EXISTS metrics.dashboard_metrics
    ADD COLUMN IF NOT EXISTS data_quality_digest bytea;

ALTER TABLE IF EXISTS metrics.dashboard_metrics
    ADD COLUMN IF NOT EXISTS completeness_digest bytea;

COMMENT ON COLUMN metrics.dashboard_metrics.data_quality_digest IS
    'Serialized TDigest (MergingDigest) storing data-quality score samples (nullable)';

COMMENT ON COLUMN metrics.dashboard_metrics.completeness_digest IS
    'Serialized TDigest (MergingDigest) storing completeness score samples (nullable)';

-- Add completeness_score to file table (per-file completeness snapshot)
ALTER TABLE IF EXISTS metrics.metrics_file
    ADD COLUMN IF NOT EXISTS completeness_score double precision;

COMMENT ON COLUMN metrics.metrics_file.completeness_score IS
    'Per-file completeness score (0.0 - 1.0) stored as a double precision snapshot';

-- Optional: leave existing scalar median columns unchanged. The application will
-- maintain the scalar `data_quality_score` and `completeness_score` snapshots
-- alongside the digest columns for fast reads.
