CREATE SCHEMA IF NOT EXISTS metrics;

CREATE TABLE IF NOT EXISTS metrics.dashboard_metrics (
    id BIGINT PRIMARY KEY,
    overall_score DOUBLE PRECISION,
    data_quality_score DOUBLE PRECISION,
    bcbs_rules_score DOUBLE PRECISION,
    completeness_score DOUBLE PRECISION,
    total_files_processed INTEGER,
    total_violations INTEGER,
    total_reports_generated INTEGER,
    version BIGINT
);
