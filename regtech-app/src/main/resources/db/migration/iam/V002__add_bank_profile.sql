-- Add bank profile table to existing IAM schema

CREATE TABLE IF NOT EXISTS bank_profile (
    bank_id BIGINT PRIMARY KEY,
    legal_name VARCHAR(255) NOT NULL,
    abi_code VARCHAR(5) NOT NULL UNIQUE,
    lei_code VARCHAR(20) NOT NULL UNIQUE,
    group_type VARCHAR(50) NOT NULL,
    bank_type VARCHAR(50) NOT NULL,
    supervision_category VARCHAR(50) NOT NULL,
    legal_address TEXT NOT NULL,
    vat_number VARCHAR(13),
    tax_code VARCHAR(11),
    company_registry VARCHAR(100),
    institutional_email VARCHAR(255),
    pec VARCHAR(255),
    phone VARCHAR(50),
    website VARCHAR(255),
    last_modified TIMESTAMP NOT NULL,
    last_modified_by VARCHAR(100) NOT NULL,
    CONSTRAINT chk_group_type CHECK (group_type IN ('INDEPENDENT', 'NATIONAL_GROUP', 'INTERNATIONAL_GROUP')),
    CONSTRAINT chk_bank_type CHECK (bank_type IN ('COMMERCIAL', 'INVESTMENT', 'COOPERATIVE', 'POPULAR', 'BCC')),
    CONSTRAINT chk_supervision CHECK (supervision_category IN ('SIGNIFICANT_SSM', 'LESS_SIGNIFICANT', 'SYSTEMICALLY_IMPORTANT', 'OTHER'))
);

-- Index for fast retrieval
CREATE INDEX idx_bank_profile_last_modified ON bank_profile(last_modified DESC);

-- Insert initial bank profile data
INSERT INTO bank_profile (
    bank_id,
    legal_name,
    abi_code,
    lei_code,
    group_type,
    bank_type,
    supervision_category,
    legal_address,
    vat_number,
    tax_code,
    company_registry,
    institutional_email,
    pec,
    phone,
    website,
    last_modified,
    last_modified_by
) VALUES (
    1,
    'Banca Italiana SpA',
    '12345',
    '1234567890ABCDEFGHIJ',
    'NATIONAL_GROUP',
    'COMMERCIAL',
    'SIGNIFICANT_SSM',
    'Via Roma 1, 00100 Roma, Italia',
    'IT12345678901',
    '12345678901',
    'REA RM-1234567',
    'info@bancaitaliana.it',
    'pec@bancaitaliana.it',
    '+39 06 12345678',
    'https://www.bancaitaliana.it',
    CURRENT_TIMESTAMP,
    'System'
);

-- Add comment
COMMENT ON TABLE bank_profile IS 'Bank institutional profile for BCBS 239 compliance - Singleton table (one row only)';
