-- Manual Database Migration Script
-- Run this script to fix the VARCHAR(36) length issue for batch_id and bank_id columns
-- Database: PostgreSQL

-- Connect to your database first, then run:

\c mydatabase;

-- Alter quality_reports table
ALTER TABLE dataquality.quality_reports 
    ALTER COLUMN batch_id TYPE VARCHAR(255),
    ALTER COLUMN bank_id TYPE VARCHAR(255);

-- Alter quality_error_summaries table  
ALTER TABLE dataquality.quality_error_summaries
    ALTER COLUMN batch_id TYPE VARCHAR(255),
    ALTER COLUMN bank_id TYPE VARCHAR(255);

-- Verify the changes
SELECT 
    table_name, 
    column_name, 
    data_type, 
    character_maximum_length 
FROM information_schema.columns 
WHERE table_schema = 'dataquality' 
    AND table_name IN ('quality_reports', 'quality_error_summaries')
    AND column_name IN ('batch_id', 'bank_id')
ORDER BY table_name, column_name;
