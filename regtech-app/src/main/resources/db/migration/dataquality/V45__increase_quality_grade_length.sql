-- V45__increase_quality_grade_length.sql
-- Fix quality_grade column length to match entity definition

-- The quality_grade column was created as VARCHAR(2) but the entity uses EnumType.STRING
-- which stores the enum name (e.g., "ACCEPTABLE", "EXCELLENT") not the letter grade
-- This migration increases the column length to VARCHAR(20) to accommodate enum names

ALTER TABLE dataquality.quality_reports 
ALTER COLUMN quality_grade TYPE VARCHAR(20);

COMMENT ON COLUMN dataquality.quality_reports.quality_grade IS 'Quality grade enum name (EXCELLENT, VERY_GOOD, GOOD, ACCEPTABLE, POOR)';
