-- Create chunk_metadata table for tracking individual chunk processing
-- This enables detailed performance monitoring and debugging of batch processing

CREATE TABLE chunk_metadata (
    id BIGSERIAL PRIMARY KEY,
    portfolio_analysis_id BIGINT NOT NULL,
    chunk_index INTEGER NOT NULL,
    chunk_size INTEGER NOT NULL,
    processed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processing_time_ms BIGINT NOT NULL,
    
    -- Foreign key to portfolio_analysis
    CONSTRAINT fk_chunk_metadata_portfolio_analysis 
        FOREIGN KEY (portfolio_analysis_id) 
        REFERENCES portfolio_analysis(id) 
        ON DELETE CASCADE,
    
    -- Ensure unique chunk index per portfolio analysis
    CONSTRAINT uk_chunk_metadata_portfolio_chunk 
        UNIQUE (portfolio_analysis_id, chunk_index)
);

-- Index for efficient querying by portfolio analysis
CREATE INDEX idx_chunk_metadata_portfolio_analysis ON chunk_metadata(portfolio_analysis_id);

-- Index for querying by processing time (for performance analysis)
CREATE INDEX idx_chunk_metadata_processing_time ON chunk_metadata(processing_time_ms);
