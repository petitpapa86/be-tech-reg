-- V62__add_event_ids_to_generated_reports.sql
-- Add dataQualityEventId and riskCalculationEventId columns to generated_reports table

-- Add new columns to the generated_reports table
ALTER TABLE reportgeneration.generated_reports
ADD COLUMN data_quality_event_id VARCHAR(255),
ADD COLUMN risk_calculation_event_id VARCHAR(255);

-- Update existing rows to populate the new columns based on batch_id
-- Assuming the event IDs are batch_id + some suffix
-- Note: Adjust the suffixes based on actual requirements
UPDATE reportgeneration.generated_reports
SET data_quality_event_id = batch_id || '-data-quality',
    risk_calculation_event_id = batch_id || '-risk-calculation'
WHERE data_quality_event_id IS NULL OR risk_calculation_event_id IS NULL;

-- Add comments for documentation
COMMENT ON COLUMN reportgeneration.generated_reports.data_quality_event_id IS 'Event ID for data quality processing, derived from batch_id';
COMMENT ON COLUMN reportgeneration.generated_reports.risk_calculation_event_id IS 'Event ID for risk calculation processing, derived from batch_id';