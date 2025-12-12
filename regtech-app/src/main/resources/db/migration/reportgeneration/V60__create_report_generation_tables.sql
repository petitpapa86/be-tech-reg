-- V60__create_report_generation_tables.sql
-- Create report generation schema tables for generated reports

-- Create schema if it doesn't exist
CREATE SCHEMA IF NOT EXISTS reportgeneration;

-- Generated Reports table - stores report generation metadata and results
CREATE TABLE IF NOT EXISTS reportgeneration.generated_reports (
    report_id UUID PRIMARY KEY,
    batch_id VARCHAR(255) NOT NULL UNIQUE,
    bank_id VARCHAR(20) NOT NULL,
    reporting_date DATE NOT NULL,
    report_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    html_s3_uri TEXT,
    html_file_size BIGINT,
    html_presigned_url TEXT,
    xbrl_s3_uri TEXT,
    xbrl_file_size BIGINT,
    xbrl_presigned_url TEXT,
    xbrl_validation_status VARCHAR(20),
    overall_quality_score DECIMAL(5, 2),
    compliance_status VARCHAR(20),
    generated_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP,
    failure_reason TEXT,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_generated_reports_batch_id ON reportgeneration.generated_reports(batch_id);
CREATE INDEX idx_generated_reports_bank_id ON reportgeneration.generated_reports(bank_id);
CREATE INDEX idx_generated_reports_status ON reportgeneration.generated_reports(status);
CREATE INDEX idx_generated_reports_reporting_date ON reportgeneration.generated_reports(reporting_date);
CREATE INDEX idx_generated_reports_generated_at ON reportgeneration.generated_reports(generated_at);

-- Comments for documentation
COMMENT ON TABLE reportgeneration.generated_reports IS 'Stores generated report metadata including HTML and XBRL outputs';
COMMENT ON COLUMN reportgeneration.generated_reports.report_id IS 'Unique identifier for the generated report';
COMMENT ON COLUMN reportgeneration.generated_reports.batch_id IS 'Reference to the batch that triggered report generation';
COMMENT ON COLUMN reportgeneration.generated_reports.bank_id IS 'Bank identifier for the report';
COMMENT ON COLUMN reportgeneration.generated_reports.reporting_date IS 'Date for which the report was generated';
COMMENT ON COLUMN reportgeneration.generated_reports.report_type IS 'Type of report (COMPREHENSIVE, XBRL, etc.)';
COMMENT ON COLUMN reportgeneration.generated_reports.status IS 'Current status of report generation';
COMMENT ON COLUMN reportgeneration.generated_reports.html_s3_uri IS 'S3 URI for the HTML report';
COMMENT ON COLUMN reportgeneration.generated_reports.xbrl_s3_uri IS 'S3 URI for the XBRL report';
COMMENT ON COLUMN reportgeneration.generated_reports.overall_quality_score IS 'Overall data quality score (0-100)';
COMMENT ON COLUMN reportgeneration.generated_reports.compliance_status IS 'Compliance status (COMPLIANT, NON_COMPLIANT, etc.)';

-- Report Metadata Failures table - fallback table for failed database inserts
-- Used when database insert fails after S3 upload for later reconciliation
CREATE TABLE IF NOT EXISTS reportgeneration.report_metadata_failures (
    id UUID PRIMARY KEY,
    batch_id VARCHAR(255) NOT NULL,
    html_s3_uri TEXT,
    xbrl_s3_uri TEXT,
    failed_at TIMESTAMP NOT NULL,
    retry_count INT DEFAULT 0,
    last_retry_at TIMESTAMP
);

-- Create index on batch_id for faster lookups during reconciliation
CREATE INDEX IF NOT EXISTS idx_report_metadata_failures_batch_id ON reportgeneration.report_metadata_failures(batch_id);

-- Create index on failed_at for cleanup operations
CREATE INDEX IF NOT EXISTS idx_report_metadata_failures_failed_at ON reportgeneration.report_metadata_failures(failed_at);

-- Comments for documentation
COMMENT ON TABLE reportgeneration.report_metadata_failures IS 'Fallback table for storing metadata when database insert fails after S3 upload';
COMMENT ON COLUMN reportgeneration.report_metadata_failures.id IS 'Unique identifier for the failed metadata record';
COMMENT ON COLUMN reportgeneration.report_metadata_failures.batch_id IS 'Reference to the batch that failed';
COMMENT ON COLUMN reportgeneration.report_metadata_failures.html_s3_uri IS 'S3 URI for the HTML report that was successfully uploaded';
COMMENT ON COLUMN reportgeneration.report_metadata_failures.xbrl_s3_uri IS 'S3 URI for the XBRL report that was successfully uploaded';
COMMENT ON COLUMN reportgeneration.report_metadata_failures.failed_at IS 'Timestamp when the database insert failed';
COMMENT ON COLUMN reportgeneration.report_metadata_failures.retry_count IS 'Number of retry attempts made';
COMMENT ON COLUMN reportgeneration.report_metadata_failures.last_retry_at IS 'Timestamp of the last retry attempt';
