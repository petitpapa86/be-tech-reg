-- Migration: Increase batch_id and bank_id column lengths to accommodate longer identifiers
-- Date: 2025-11-18
-- Issue: VARCHAR(36) is too small for batch IDs that include timestamps and UUIDs
--        Example: batch_20251115_233742_batch_20251115_233722_74546acb-a0df-44c1-a09b-a1fd884a8af4

-- Alter quality_reports table
ALTER TABLE dataquality.quality_reports 
    ALTER COLUMN batch_id TYPE VARCHAR(255),
    ALTER COLUMN bank_id TYPE VARCHAR(255);

-- Alter quality_error_summaries table
ALTER TABLE dataquality.quality_error_summaries
    ALTER COLUMN batch_id TYPE VARCHAR(255),
    ALTER COLUMN bank_id TYPE VARCHAR(255);
