-- V53__Add_calculation_results_uri_to_batches.sql
-- Adds calculation_results_uri column to batches table for file-first storage architecture
-- This column stores the S3 URI or filesystem path to the JSON file containing complete calculation results

-- Add calculation_results_uri column to batches table
ALTER TABLE riskcalculation.batches 
ADD COLUMN IF NOT EXISTS calculation_results_uri VARCHAR(500);

-- Add index on calculation_results_uri for URI lookups
CREATE INDEX IF NOT EXISTS idx_batches_results_uri 
ON riskcalculation.batches(calculation_results_uri);

-- Add comment to document the column purpose
COMMENT ON COLUMN riskcalculation.batches.calculation_results_uri IS 
'S3 URI or filesystem path to the JSON file containing complete calculation results (exposures, mitigations, portfolio analysis). This serves as the single source of truth for detailed calculation data.';
