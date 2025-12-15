-- V61__expand_generated_reports_status_columns.sql
-- Expand enum-backed status columns to avoid truncation for longer values

ALTER TABLE reportgeneration.generated_reports
    ALTER COLUMN compliance_status TYPE VARCHAR(50);

ALTER TABLE reportgeneration.generated_reports
    ALTER COLUMN xbrl_validation_status TYPE VARCHAR(50);
