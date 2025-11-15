-- Increase batch_id column length from 50 to 100 characters
-- Required because batch IDs like "batch_20251115_213143_7a626ad6-6a61-47d5-862d-b8f0d25fcae0" are 63 chars
ALTER TABLE ingestion.ingestion_batches ALTER COLUMN batch_id TYPE VARCHAR(100);
