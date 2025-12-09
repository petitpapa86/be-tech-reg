-- V33__Increase_bank_id_length.sql
-- Increase bank_id column length to accommodate longer bank identifiers

ALTER TABLE ingestion.bank_info 
ALTER COLUMN bank_id TYPE VARCHAR(50);
