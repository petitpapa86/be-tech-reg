-- V4__Add_Performance_Indexes.sql
-- Add additional indexes for performance optimization

-- Composite index for common query patterns (bank_id + status + uploaded_at)
CREATE INDEX idx_ingestion_batches_bank_status_uploaded 
ON regtech.ingestion_batches(bank_id, status, uploaded_at);

-- Composite index for time-based queries with status filtering
CREATE INDEX idx_ingestion_batches_status_uploaded 
ON regtech.ingestion_batches(status, uploaded_at);

-- Index for processing duration analysis
CREATE INDEX idx_ingestion_batches_processing_duration 
ON regtech.ingestion_batches(processing_duration_ms) 
WHERE processing_duration_ms IS NOT NULL;

-- Index for file size analysis and performance monitoring
CREATE INDEX idx_ingestion_batches_file_size 
ON regtech.ingestion_batches(file_size_bytes) 
WHERE file_size_bytes IS NOT NULL;

-- Partial index for active/processing batches (most frequently queried)
CREATE INDEX idx_ingestion_batches_active_status 
ON regtech.ingestion_batches(status, updated_at) 
WHERE status IN ('UPLOADED', 'PARSING', 'VALIDATED', 'STORING');

-- Index for error analysis and monitoring
CREATE INDEX idx_ingestion_batches_failed_status 
ON regtech.ingestion_batches(status, updated_at, bank_id) 
WHERE status = 'FAILED';

-- Composite index for bank-specific time range queries (common in reporting)
CREATE INDEX idx_ingestion_batches_bank_time_range 
ON regtech.ingestion_batches(bank_id, uploaded_at, status);

-- Index for completed batches with completion time (for SLA monitoring)
CREATE INDEX idx_ingestion_batches_completed 
ON regtech.ingestion_batches(completed_at, processing_duration_ms) 
WHERE status = 'COMPLETED' AND completed_at IS NOT NULL;

-- Add comments for index documentation
COMMENT ON INDEX regtech.idx_ingestion_batches_bank_status_uploaded IS 
'Composite index for bank-specific status queries with time ordering';

COMMENT ON INDEX regtech.idx_ingestion_batches_status_uploaded IS 
'Optimizes status-based queries with time range filtering';

COMMENT ON INDEX regtech.idx_ingestion_batches_processing_duration IS 
'Supports performance analysis queries on processing duration';

COMMENT ON INDEX regtech.idx_ingestion_batches_file_size IS 
'Enables efficient file size analysis and large file identification';

COMMENT ON INDEX regtech.idx_ingestion_batches_active_status IS 
'Partial index for frequently queried active/processing batches';

COMMENT ON INDEX regtech.idx_ingestion_batches_failed_status IS 
'Optimizes error analysis and failed batch monitoring queries';

COMMENT ON INDEX regtech.idx_ingestion_batches_bank_time_range IS 
'Supports bank-specific reporting queries with time ranges';

COMMENT ON INDEX regtech.idx_ingestion_batches_completed IS 
'Optimizes SLA monitoring and completion time analysis';