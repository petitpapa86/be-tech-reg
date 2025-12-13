-- V30__create_ingestion_tables.sql
-- Create ingestion schema tables for batch tracking and bank information

-- Create schema if it doesn't exist
CREATE SCHEMA IF NOT EXISTS ingestion;

-- Ingestion Batches table - stores batch processing metadata
CREATE TABLE IF NOT EXISTS ingestion.ingestion_batches (
    batch_id VARCHAR(255) PRIMARY KEY,
    bank_id VARCHAR(255) NOT NULL,
    bank_name VARCHAR(100),
    bank_country VARCHAR(3),
    status VARCHAR(20) NOT NULL,
    total_exposures INTEGER,
    file_size_bytes BIGINT,
    file_name VARCHAR(255),
    content_type VARCHAR(100),
    s3_uri VARCHAR(500),
    s3_bucket VARCHAR(100),
    s3_key VARCHAR(300),
    s3_version_id VARCHAR(100),
    md5_checksum VARCHAR(32),
    sha256_checksum VARCHAR(64),
    uploaded_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP,
    error_message TEXT,
    processing_duration_ms BIGINT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_ingestion_batches_bank_id ON ingestion.ingestion_batches(bank_id);
CREATE INDEX idx_ingestion_batches_status ON ingestion.ingestion_batches(status);
CREATE INDEX idx_ingestion_batches_uploaded_at ON ingestion.ingestion_batches(uploaded_at);
CREATE INDEX idx_ingestion_batches_completed_at ON ingestion.ingestion_batches(completed_at);

-- Bank Info table - stores bank metadata
CREATE TABLE IF NOT EXISTS ingestion.bank_info (
    bank_id VARCHAR(50) PRIMARY KEY,
    bank_name VARCHAR(100) NOT NULL,
    bank_country VARCHAR(3) NOT NULL,
    bank_status VARCHAR(20) NOT NULL,
    last_updated TIMESTAMP NOT NULL
);

CREATE INDEX idx_bank_info_country ON ingestion.bank_info(bank_country);
CREATE INDEX idx_bank_info_status ON ingestion.bank_info(bank_status);

-- Comments for documentation
COMMENT ON TABLE ingestion.ingestion_batches IS 'Stores batch processing metadata for data ingestion';
COMMENT ON TABLE ingestion.bank_info IS 'Stores bank metadata and configuration';
