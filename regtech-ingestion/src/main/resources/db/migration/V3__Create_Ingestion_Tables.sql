-- V3__Create_Ingestion_Tables.sql
-- Create ingestion module tables for batch processing and bank information caching

-- Create ingestion_batches table
CREATE TABLE regtech.ingestion_batches (
    batch_id VARCHAR(50) PRIMARY KEY,
    bank_id VARCHAR(20) NOT NULL,
    bank_name VARCHAR(100),
    bank_country VARCHAR(3),
    status VARCHAR(20) NOT NULL CHECK (status IN ('UPLOADED', 'PARSING', 'VALIDATED', 'STORING', 'COMPLETED', 'FAILED')),
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
    uploaded_at TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE,
    error_message TEXT,
    processing_duration_ms BIGINT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for efficient querying
CREATE INDEX idx_ingestion_batches_bank_id ON regtech.ingestion_batches(bank_id);
CREATE INDEX idx_ingestion_batches_status ON regtech.ingestion_batches(status);
CREATE INDEX idx_ingestion_batches_uploaded_at ON regtech.ingestion_batches(uploaded_at);
CREATE INDEX idx_ingestion_batches_created_at ON regtech.ingestion_batches(created_at);

-- Create bank_info table for caching bank information
CREATE TABLE regtech.bank_info (
    bank_id VARCHAR(20) PRIMARY KEY,
    bank_name VARCHAR(100) NOT NULL,
    bank_country VARCHAR(3) NOT NULL,
    bank_status VARCHAR(20) NOT NULL CHECK (bank_status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED')),
    last_updated TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create index for timestamp-based queries (freshness validation)
CREATE INDEX idx_bank_info_last_updated ON regtech.bank_info(last_updated);

-- Add comments for documentation
COMMENT ON TABLE regtech.ingestion_batches IS 'Stores metadata for file ingestion batches without storing individual exposures';
COMMENT ON TABLE regtech.bank_info IS 'Caches bank information from Bank Registry service with freshness tracking';

COMMENT ON COLUMN regtech.ingestion_batches.batch_id IS 'Unique batch identifier with format batch_YYYYMMDD_HHMMSS_UUID';
COMMENT ON COLUMN regtech.ingestion_batches.status IS 'Current processing status of the batch';
COMMENT ON COLUMN regtech.ingestion_batches.total_exposures IS 'Count of exposures in the file (not stored individually)';
COMMENT ON COLUMN regtech.ingestion_batches.s3_uri IS 'Complete S3 URI for accessing the stored file';
COMMENT ON COLUMN regtech.ingestion_batches.processing_duration_ms IS 'Total processing time in milliseconds';

COMMENT ON COLUMN regtech.bank_info.last_updated IS 'Timestamp for cache freshness validation (24 hour TTL)';
COMMENT ON COLUMN regtech.bank_info.bank_status IS 'Bank registration status from Bank Registry service';