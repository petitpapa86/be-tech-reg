-- V33__Increase_bank_id_length.sql
-- Increase bank_id column length to accommodate longer bank identifiers

DO $$
BEGIN
    -- Check if the column is already VARCHAR(50) or larger
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'ingestion'
        AND table_name = 'bank_info'
        AND column_name = 'bank_id'
        AND character_maximum_length >= 50
    ) THEN
        ALTER TABLE ingestion.bank_info
        ALTER COLUMN bank_id TYPE VARCHAR(50);
    END IF;
END $$;
