-- Create Metrics schema + metrics_file table (safe for existing DBs)
-- This migration intentionally uses a timestamped version so it runs even when the database
-- already has later Vxx migrations applied.

CREATE SCHEMA IF NOT EXISTS metrics;

CREATE TABLE IF NOT EXISTS metrics.metrics_file (
    filename VARCHAR(255) PRIMARY KEY,
    date VARCHAR(255),
    score DOUBLE PRECISION,
    status VARCHAR(255),
    batch_id VARCHAR(255),
    bank_id VARCHAR(255)
);
