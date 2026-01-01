-- V63.1__increase_event_id_columns_length.sql
-- Increase length of event ID columns to 300 characters

ALTER TABLE reportgeneration.generated_reports
ALTER COLUMN data_quality_event_id TYPE VARCHAR(300),
ALTER COLUMN risk_calculation_event_id TYPE VARCHAR(300);