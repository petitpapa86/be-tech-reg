-- Migration: Increase batch_id and bank_id column lengths in ingestion_batches table
-- Date: 2025-11-18
-- Issue: VARCHAR(100) for batch_id and VARCHAR(20) for bank_id are too small
--        Example batch_id: batch_20251115_233742_batch_20251115_233722_74546acb-a0df-44c1-a09b-a1fd884a8af4

-- Alter ingestion_batches table
ALTER TABLE ingestion.ingestion_batches 
    ALTER COLUMN batch_id TYPE VARCHAR(255),
    ALTER COLUMN bank_id TYPE VARCHAR(255);

-- Verify the changes
SELECT 
    table_name, 
    column_name, 
    data_type, 
    character_maximum_length 
FROM information_schema.columns 
WHERE table_schema = 'ingestion' 
    AND table_name = 'ingestion_batches'
    AND column_name IN ('batch_id', 'bank_id')
ORDER BY column_name;
