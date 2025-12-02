-- Add state tracking columns to portfolio_analysis table for performance optimization
-- This enables progress tracking, resumability, and monitoring of batch processing

ALTER TABLE portfolio_analysis
    ADD COLUMN processing_state VARCHAR(20),
    ADD COLUMN total_exposures INTEGER,
    ADD COLUMN processed_exposures INTEGER DEFAULT 0,
    ADD COLUMN started_at TIMESTAMP,
    ADD COLUMN last_updated_at TIMESTAMP;

-- Add index for querying by processing state
CREATE INDEX idx_portfolio_analysis_state ON portfolio_analysis(processing_state);

-- Add index for querying by timestamps
CREATE INDEX idx_portfolio_analysis_started_at ON portfolio_analysis(started_at);
