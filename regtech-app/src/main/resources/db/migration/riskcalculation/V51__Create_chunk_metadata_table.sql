-- V51__Create_chunk_metadata_table.sql
-- Create chunk_metadata table for tracking individual chunk processing
-- Originally: V4__Create_chunk_metadata_table.sql
-- This enables detailed performance monitoring and debugging of batch processing

CREATE TABLE riskcalculation.chunk_metadata (
    id BIGSERIAL PRIMARY KEY,
    batch_id VARCHAR(100) NOT NULL,
    chunk_index INTEGER NOT NULL,
    chunk_size INTEGER NOT NULL,
    processed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processing_time_ms BIGINT NOT NULL,
    
    -- Foreign key to portfolio_analysis
    CONSTRAINT fk_chunk_metadata_portfolio_analysis 
        FOREIGN KEY (batch_id) 
        REFERENCES riskcalculation.portfolio_analysis(batch_id) 
        ON DELETE CASCADE,
    
    -- Ensure unique chunk index per portfolio analysis
    CONSTRAINT uk_chunk_metadata_portfolio_chunk 
        UNIQUE (batch_id, chunk_index)
);

-- Index for efficient querying by portfolio analysis
CREATE INDEX idx_chunk_metadata_portfolio_analysis ON riskcalculation.chunk_metadata(batch_id);

-- Index for querying by processing time (for performance analysis)
CREATE INDEX idx_chunk_metadata_processing_time ON riskcalculation.chunk_metadata(processing_time_ms);
