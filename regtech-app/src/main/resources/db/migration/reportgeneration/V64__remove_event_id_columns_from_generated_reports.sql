-- V64__remove_event_id_columns_from_generated_reports.sql
-- Remove dataQualityEventId and riskCalculationEventId columns from generated_reports table

-- Drop the event ID columns from the generated_reports table
ALTER TABLE reportgeneration.generated_reports
DROP COLUMN data_quality_event_id,
DROP COLUMN risk_calculation_event_id;