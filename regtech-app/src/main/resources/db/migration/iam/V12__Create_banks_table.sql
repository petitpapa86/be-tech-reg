-- V13__Create_banks_table.sql
-- Create banks table in iam schema
-- Originally: V202511242200__Create_banks_table.sql

CREATE TABLE iam.banks (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    country_code VARCHAR(2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    
    -- Profile fields
    legal_name VARCHAR(255),
    abi_code VARCHAR(5) UNIQUE,
    lei_code VARCHAR(20) UNIQUE,
    group_type VARCHAR(50),
    bank_type VARCHAR(50),
    supervision_category VARCHAR(50),
    legal_address TEXT,
    vat_number VARCHAR(13),
    tax_code VARCHAR(11),
    company_registry VARCHAR(100),
    institutional_email VARCHAR(255),
    pec VARCHAR(255),
    phone VARCHAR(50),
    website VARCHAR(255),
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create index on country_code for faster lookups
CREATE INDEX idx_banks_country_code ON iam.banks(country_code);

-- Create index on name for faster lookups
CREATE INDEX idx_banks_name ON iam.banks(name);
