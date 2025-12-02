-- V50__Create_risk_calculation_tables.sql
-- Creates tables for batches, exposures, mitigations, and portfolio analysis
-- Originally: V2__Create_risk_calculation_tables.sql

-- Create schema if it doesn't exist
CREATE SCHEMA IF NOT EXISTS riskcalculation;

-- Batches table - stores batch metadata
CREATE TABLE IF NOT EXISTS riskcalculation.batches (
    batch_id VARCHAR(100) PRIMARY KEY,
    bank_name VARCHAR(255) NOT NULL,
    abi_code VARCHAR(10) NOT NULL,
    lei_code VARCHAR(20) NOT NULL,
    report_date DATE NOT NULL,
    total_exposures INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL,
    ingested_at TIMESTAMP NOT NULL,
    processed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE INDEX idx_batches_report_date ON riskcalculation.batches(report_date);
CREATE INDEX idx_batches_status ON riskcalculation.batches(status);
CREATE INDEX idx_batches_bank_name ON riskcalculation.batches(bank_name);

-- Exposures table - stores individual exposure records
CREATE TABLE IF NOT EXISTS riskcalculation.exposures (
    exposure_id VARCHAR(100) PRIMARY KEY,
    batch_id VARCHAR(100) NOT NULL,
    instrument_id VARCHAR(100) NOT NULL,
    
    -- Counterparty information
    counterparty_id VARCHAR(100) NOT NULL,
    counterparty_name VARCHAR(255) NOT NULL,
    counterparty_lei VARCHAR(20),
    
    -- Monetary amounts
    exposure_amount DECIMAL(20, 2) NOT NULL,
    currency_code VARCHAR(3) NOT NULL,
    
    -- Classification
    product_type VARCHAR(100) NOT NULL,
    instrument_type VARCHAR(20) NOT NULL,
    balance_sheet_type VARCHAR(20) NOT NULL,
    country_code VARCHAR(2) NOT NULL,
    
    -- Metadata
    recorded_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (batch_id) REFERENCES riskcalculation.batches(batch_id) ON DELETE CASCADE
);

CREATE INDEX idx_exposures_batch_id ON riskcalculation.exposures(batch_id);
CREATE INDEX idx_exposures_country_code ON riskcalculation.exposures(country_code);
CREATE INDEX idx_exposures_instrument_type ON riskcalculation.exposures(instrument_type);
CREATE INDEX idx_exposures_product_type ON riskcalculation.exposures(product_type);

-- Mitigations table - stores credit risk mitigation data
CREATE TABLE IF NOT EXISTS riskcalculation.mitigations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    exposure_id VARCHAR(100) NOT NULL,
    batch_id VARCHAR(100) NOT NULL,
    mitigation_type VARCHAR(50) NOT NULL,
    value DECIMAL(20, 2) NOT NULL,
    currency_code VARCHAR(3) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (exposure_id) REFERENCES riskcalculation.exposures(exposure_id) ON DELETE CASCADE,
    FOREIGN KEY (batch_id) REFERENCES riskcalculation.batches(batch_id) ON DELETE CASCADE
);

CREATE INDEX idx_mitigations_exposure_id ON riskcalculation.mitigations(exposure_id);
CREATE INDEX idx_mitigations_batch_id ON riskcalculation.mitigations(batch_id);

-- Portfolio Analysis table - stores aggregated portfolio analysis results
CREATE TABLE IF NOT EXISTS riskcalculation.portfolio_analysis (
    batch_id VARCHAR(100) PRIMARY KEY,
    
    -- Totals
    total_portfolio_eur DECIMAL(20, 2) NOT NULL,
    
    -- Geographic breakdown
    italy_amount DECIMAL(20, 2),
    italy_percentage DECIMAL(5, 2),
    eu_other_amount DECIMAL(20, 2),
    eu_other_percentage DECIMAL(5, 2),
    non_european_amount DECIMAL(20, 2),
    non_european_percentage DECIMAL(5, 2),
    
    -- Sector breakdown
    retail_mortgage_amount DECIMAL(20, 2),
    retail_mortgage_percentage DECIMAL(5, 2),
    sovereign_amount DECIMAL(20, 2),
    sovereign_percentage DECIMAL(5, 2),
    corporate_amount DECIMAL(20, 2),
    corporate_percentage DECIMAL(5, 2),
    banking_amount DECIMAL(20, 2),
    banking_percentage DECIMAL(5, 2),
    other_amount DECIMAL(20, 2),
    other_percentage DECIMAL(5, 2),
    
    -- Concentration indices
    geographic_hhi DECIMAL(6, 4) NOT NULL,
    geographic_concentration_level VARCHAR(20) NOT NULL,
    sector_hhi DECIMAL(6, 4) NOT NULL,
    sector_concentration_level VARCHAR(20) NOT NULL,
    
    -- Metadata
    analyzed_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (batch_id) REFERENCES riskcalculation.batches(batch_id) ON DELETE CASCADE
);

CREATE INDEX idx_portfolio_analysis_analyzed_at ON riskcalculation.portfolio_analysis(analyzed_at);
