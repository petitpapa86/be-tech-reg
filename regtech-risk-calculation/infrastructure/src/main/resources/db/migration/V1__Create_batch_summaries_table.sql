-- Create batch_summaries table for risk calculation module
-- This table stores aggregated risk metrics and concentration data for each batch

CREATE TABLE batch_summaries (
    id SERIAL PRIMARY KEY,
    batch_summary_id VARCHAR(100) UNIQUE NOT NULL,
    batch_id VARCHAR(100) UNIQUE NOT NULL,
    bank_id VARCHAR(50) NOT NULL,
    
    -- Status and timestamps
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    failed_at TIMESTAMP,
    
    -- Totals
    total_exposures INTEGER,
    total_amount_eur DECIMAL(20,2),
    
    -- Geographic breakdown
    italy_amount DECIMAL(20,2),
    italy_pct DECIMAL(5,2),
    italy_count INTEGER,
    eu_amount DECIMAL(20,2),
    eu_pct DECIMAL(5,2),
    eu_count INTEGER,
    non_eu_amount DECIMAL(20,2),
    non_eu_pct DECIMAL(5,2),
    non_eu_count INTEGER,
    
    -- Sector breakdown
    retail_amount DECIMAL(20,2),
    retail_pct DECIMAL(5,2),
    retail_count INTEGER,
    sovereign_amount DECIMAL(20,2),
    sovereign_pct DECIMAL(5,2),
    sovereign_count INTEGER,
    corporate_amount DECIMAL(20,2),
    corporate_pct DECIMAL(5,2),
    corporate_count INTEGER,
    banking_amount DECIMAL(20,2),
    banking_pct DECIMAL(5,2),
    banking_count INTEGER,
    other_amount DECIMAL(20,2),
    other_pct DECIMAL(5,2),
    other_count INTEGER,
    
    -- Concentration metrics
    herfindahl_geographic DECIMAL(6,4),
    herfindahl_sector DECIMAL(6,4),
    
    -- File reference
    result_file_uri TEXT,
    
    -- Error handling
    error_message TEXT,
    
    -- Optimistic locking
    version BIGINT DEFAULT 0
);

-- Create indexes for performance
CREATE INDEX idx_batch_summaries_batch_id ON batch_summaries(batch_id);
CREATE INDEX idx_batch_summaries_bank_id ON batch_summaries(bank_id);
CREATE INDEX idx_batch_summaries_status ON batch_summaries(status);
CREATE INDEX idx_batch_summaries_created_at ON batch_summaries(created_at);

-- Add comments for documentation
COMMENT ON TABLE batch_summaries IS 'Stores aggregated risk calculation results for each batch of exposures';
COMMENT ON COLUMN batch_summaries.batch_summary_id IS 'Unique identifier for the batch summary record';
COMMENT ON COLUMN batch_summaries.batch_id IS 'Reference to the original ingestion batch';
COMMENT ON COLUMN batch_summaries.bank_id IS 'Identifier of the bank that owns the exposures';
COMMENT ON COLUMN batch_summaries.status IS 'Current status of the risk calculation (PENDING, DOWNLOADING, CALCULATING, COMPLETED, FAILED)';
COMMENT ON COLUMN batch_summaries.herfindahl_geographic IS 'Herfindahl-Hirschman Index for geographic concentration (0.0000 to 1.0000)';
COMMENT ON COLUMN batch_summaries.herfindahl_sector IS 'Herfindahl-Hirschman Index for sector concentration (0.0000 to 1.0000)';
COMMENT ON COLUMN batch_summaries.result_file_uri IS 'URI reference to detailed calculation results file (S3 or filesystem)';