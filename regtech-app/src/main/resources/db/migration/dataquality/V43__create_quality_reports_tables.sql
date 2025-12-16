-- V43__create_quality_reports_tables.sql
-- Create tables for quality reports and error summaries

-- Table: quality_reports
-- Stores quality validation reports for batches
CREATE TABLE IF NOT EXISTS dataquality.quality_reports (
    report_id VARCHAR(36) NOT NULL PRIMARY KEY,
    batch_id VARCHAR(255) NOT NULL UNIQUE,
    bank_id VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL,
    completeness_score DECIMAL(5,2),
    accuracy_score DECIMAL(5,2),
    consistency_score DECIMAL(5,2),
    timeliness_score DECIMAL(5,2),
    uniqueness_score DECIMAL(5,2),
    validity_score DECIMAL(5,2),
    overall_score DECIMAL(5,2),
    quality_grade VARCHAR(2),
    total_exposures INTEGER,
    valid_exposures INTEGER,
    total_errors INTEGER,
    completeness_errors INTEGER,
    accuracy_errors INTEGER,
    consistency_errors INTEGER,
    timeliness_errors INTEGER,
    uniqueness_errors INTEGER,
    validity_errors INTEGER,
    s3_bucket VARCHAR(255),
    s3_key VARCHAR(1024),
    s3_uri VARCHAR(1024),
    compliance_status BOOLEAN NOT NULL DEFAULT FALSE,
    error_message VARCHAR(2000),
    processing_start_time TIMESTAMP,
    processing_end_time TIMESTAMP,
    processing_duration_ms BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

-- Table: quality_error_summaries
-- Stores aggregated error summaries by dimension
CREATE TABLE IF NOT EXISTS dataquality.quality_error_summaries (
    id BIGSERIAL PRIMARY KEY,
    batch_id VARCHAR(255) NOT NULL,
    bank_id VARCHAR(255) NOT NULL,
    rule_code VARCHAR(100) NOT NULL,
    dimension VARCHAR(20) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    error_message VARCHAR(1000) NOT NULL,
    field_name VARCHAR(100),
    error_count INTEGER NOT NULL,
    affected_exposure_ids JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

-- Indexes for quality_reports
CREATE INDEX IF NOT EXISTS idx_quality_reports_batch_id ON dataquality.quality_reports (batch_id);
CREATE INDEX IF NOT EXISTS idx_quality_reports_bank_id ON dataquality.quality_reports (bank_id);
CREATE INDEX IF NOT EXISTS idx_quality_reports_status ON dataquality.quality_reports (status);
CREATE INDEX IF NOT EXISTS idx_quality_reports_bank_status ON dataquality.quality_reports (bank_id, status);
CREATE INDEX IF NOT EXISTS idx_quality_reports_created_at ON dataquality.quality_reports (created_at);
CREATE INDEX IF NOT EXISTS idx_quality_reports_overall_score ON dataquality.quality_reports (overall_score);
CREATE INDEX IF NOT EXISTS idx_quality_reports_compliance_status ON dataquality.quality_reports (compliance_status);

-- Indexes for quality_error_summaries
CREATE INDEX IF NOT EXISTS idx_error_summaries_batch_id ON dataquality.quality_error_summaries (batch_id);
CREATE INDEX IF NOT EXISTS idx_error_summaries_batch_dimension ON dataquality.quality_error_summaries (batch_id, dimension);
CREATE INDEX IF NOT EXISTS idx_error_summaries_batch_severity ON dataquality.quality_error_summaries (batch_id, severity);
CREATE INDEX IF NOT EXISTS idx_error_summaries_rule_code ON dataquality.quality_error_summaries (rule_code);
CREATE INDEX IF NOT EXISTS idx_error_summaries_dimension ON dataquality.quality_error_summaries (dimension);
CREATE INDEX IF NOT EXISTS idx_error_summaries_severity ON dataquality.quality_error_summaries (severity);
CREATE INDEX IF NOT EXISTS idx_error_summaries_bank_id ON dataquality.quality_error_summaries (bank_id);
CREATE INDEX IF NOT EXISTS idx_error_summaries_bank_dimension ON dataquality.quality_error_summaries (bank_id, dimension);
CREATE INDEX IF NOT EXISTS idx_error_summaries_created_at ON dataquality.quality_error_summaries (created_at);
CREATE INDEX IF NOT EXISTS idx_error_summaries_error_count ON dataquality.quality_error_summaries (error_count);

-- Comments for documentation
COMMENT ON TABLE dataquality.quality_reports IS 'Quality validation reports for ingested batches';
COMMENT ON TABLE dataquality.quality_error_summaries IS 'Aggregated error summaries by dimension and severity';
