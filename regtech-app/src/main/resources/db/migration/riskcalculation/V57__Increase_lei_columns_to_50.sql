-- V57__Increase_lei_columns_to_50.sql
-- Widen LEI-related columns so invalid/dirty values can be stored and later flagged by data-quality rules.

ALTER TABLE riskcalculation.batches
    ALTER COLUMN lei_code TYPE VARCHAR(50);

ALTER TABLE riskcalculation.exposures
    ALTER COLUMN counterparty_lei TYPE VARCHAR(50);
