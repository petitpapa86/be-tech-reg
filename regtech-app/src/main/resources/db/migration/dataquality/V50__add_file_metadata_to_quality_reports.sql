-- V48__add_file_metadata_to_quality_reports.sql
-- Add filename, file_format, and file_size columns to quality_reports table

ALTER TABLE quality_reports ADD COLUMN IF NOT EXISTS filename VARCHAR(255);
ALTER TABLE quality_reports ADD COLUMN IF NOT EXISTS file_format VARCHAR(50);
ALTER TABLE quality_reports ADD COLUMN IF NOT EXISTS file_size BIGINT;

-- Backfill data for existing records
-- Assuming default filename and format for existing records
UPDATE quality_reports SET filename = 'esposizioni.xlsx' WHERE filename IS NULL;
UPDATE quality_reports SET file_format = 'XLSX' WHERE file_format IS NULL;
UPDATE quality_reports SET file_size = 0 WHERE file_size IS NULL;
