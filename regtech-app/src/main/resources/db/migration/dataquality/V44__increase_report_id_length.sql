-- V44__increase_report_id_length.sql
-- Increase report_id column length to accommodate prefixed UUIDs

-- The report_id is generated with format 'qr_<uuid>' which is 41 characters
-- Current VARCHAR(36) is too short, increase to VARCHAR(50) to be safe
ALTER TABLE dataquality.quality_reports 
    ALTER COLUMN report_id TYPE VARCHAR(50);

COMMENT ON COLUMN dataquality.quality_reports.report_id IS 'Quality report identifier with format qr_<uuid>';
