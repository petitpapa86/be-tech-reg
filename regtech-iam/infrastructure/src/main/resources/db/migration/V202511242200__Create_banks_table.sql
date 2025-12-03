-- Create banks table in iam schema (idempotent)
CREATE TABLE IF NOT EXISTS iam.banks (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    country_code VARCHAR(2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create index on country_code for faster lookups (idempotent)
CREATE INDEX IF NOT EXISTS idx_banks_country_code ON iam.banks(country_code);

-- Create index on name for faster lookups (idempotent)
CREATE INDEX IF NOT EXISTS idx_banks_name ON iam.banks(name);
