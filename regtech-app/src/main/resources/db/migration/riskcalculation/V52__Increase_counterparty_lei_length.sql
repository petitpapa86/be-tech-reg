-- V52__Increase_counterparty_lei_length.sql
-- Increases counterparty_lei column length to accommodate edge cases
-- LEI codes are standardized at 20 characters, but we allow 30 for flexibility

ALTER TABLE riskcalculation.exposures 
    ALTER COLUMN counterparty_lei TYPE VARCHAR(30);
