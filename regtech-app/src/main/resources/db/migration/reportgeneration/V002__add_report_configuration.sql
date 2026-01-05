-- Add report configuration table to report generation schema

CREATE TABLE IF NOT EXISTS report_configuration (
    id BIGSERIAL PRIMARY KEY,
    
    -- Bank Identifier
    bank_id BIGINT NOT NULL,
    
    -- Template & Format
    template VARCHAR(50) NOT NULL CHECK (template IN ('BANCA_ITALIA_STANDARD', 'ECB_EUROPEAN_STANDARD', 'CUSTOM')),
    language VARCHAR(50) NOT NULL CHECK (language IN ('ITALIAN', 'ENGLISH', 'BILINGUAL')),
    output_format VARCHAR(50) NOT NULL CHECK (output_format IN ('PDF', 'EXCEL', 'BOTH')),
    
    -- Scheduling
    frequency VARCHAR(50) NOT NULL CHECK (frequency IN ('MONTHLY', 'QUARTERLY', 'SEMI_ANNUAL', 'ANNUAL')),
    submission_deadline INTEGER NOT NULL CHECK (submission_deadline BETWEEN 1 AND 30),
    auto_generation_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    schedule_day VARCHAR(50) NOT NULL CHECK (schedule_day IN ('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY')),
    schedule_time TIME NOT NULL,
    
    -- Distribution
    primary_recipient VARCHAR(255) NOT NULL,
    cc_recipient VARCHAR(255),
    auto_send_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    swift_auto_submit_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    
    -- Metadata
    last_modified TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_by VARCHAR(100) NOT NULL,
    
    -- Constraints
    CONSTRAINT chk_schedule_time_working_hours CHECK (schedule_time BETWEEN '08:00:00' AND '18:00:00'),
    CONSTRAINT chk_primary_recipient_email CHECK (primary_recipient ~* '^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$'),
    CONSTRAINT chk_cc_recipient_email CHECK (cc_recipient IS NULL OR cc_recipient ~* '^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$')
);

-- Index for fast retrieval
CREATE UNIQUE INDEX idx_report_configuration_bank_id ON report_configuration(bank_id);
CREATE INDEX idx_report_configuration_last_modified ON report_configuration(last_modified DESC);

-- Insert initial configuration data
INSERT INTO report_configuration (
    bank_id,
    template,
    language,
    output_format,
    frequency,
    submission_deadline,
    auto_generation_enabled,
    schedule_day,
    schedule_time,
    primary_recipient,
    cc_recipient,
    auto_send_enabled,
    swift_auto_submit_enabled,
    last_modified,
    last_modified_by
) VALUES (
    1,
    'BANCA_ITALIA_STANDARD',
    'ITALIAN',
    'PDF',
    'MONTHLY',
    20,
    FALSE,
    'MONDAY',
    '09:00:00',
    'reports@bank.it',
    NULL,
    FALSE,
    FALSE,
    CURRENT_TIMESTAMP,
    'System'
);

-- Add comments
COMMENT ON TABLE report_configuration IS 'Report generation and distribution configuration for BCBS 239 - One configuration per bank';
COMMENT ON COLUMN report_configuration.swift_auto_submit_enabled IS 'Auto-submission to Banca d''Italia via SWIFT (currently in development)';
