-- =====================================================================
-- Rules Engine Database Schema
-- Version: 1.8
-- Description: Create tables for dynamic business rules management
-- =====================================================================

-- Table: regulations
-- Stores regulatory frameworks and their metadata
CREATE TABLE IF NOT EXISTS regulations (
    regulation_id VARCHAR(100) PRIMARY KEY,
    regulation_name VARCHAR(255) NOT NULL,
    regulation_code VARCHAR(50) UNIQUE,
    description TEXT,
    issuing_authority VARCHAR(255),
    effective_date DATE NOT NULL,
    expiration_date DATE,
    version VARCHAR(50),
    status VARCHAR(20) NOT NULL CHECK (status IN ('DRAFT', 'ACTIVE', 'SUPERSEDED', 'RETIRED')),
    parent_regulation_id VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_parent_regulation FOREIGN KEY (parent_regulation_id) 
        REFERENCES regulations(regulation_id)
);

CREATE INDEX idx_regulations_code ON regulations(regulation_code);
CREATE INDEX idx_regulations_status ON regulations(status);
CREATE INDEX idx_regulations_dates ON regulations(effective_date, expiration_date);

-- Table: regulation_templates
-- Stores reusable rule templates
CREATE TABLE IF NOT EXISTS regulation_templates (
    template_id VARCHAR(100) PRIMARY KEY,
    template_name VARCHAR(255) NOT NULL,
    template_code VARCHAR(50) UNIQUE,
    description TEXT,
    template_type VARCHAR(50) NOT NULL,
    business_logic TEXT NOT NULL,
    parameter_schema JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_templates_type ON regulation_templates(template_type);

-- Table: business_rules
-- Stores configurable business rules
CREATE TABLE IF NOT EXISTS business_rules (
    rule_id VARCHAR(100) PRIMARY KEY,
    regulation_id VARCHAR(100) NOT NULL,
    template_id VARCHAR(100),
    rule_name VARCHAR(255) NOT NULL,
    rule_code VARCHAR(50) UNIQUE,
    description TEXT,
    rule_type VARCHAR(50) NOT NULL CHECK (rule_type IN ('VALIDATION', 'THRESHOLD', 'CALCULATION', 'BUSINESS_LOGIC', 'DATA_QUALITY')),
    rule_category VARCHAR(50),
    severity VARCHAR(20) NOT NULL CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    business_logic TEXT NOT NULL,
    execution_order INTEGER NOT NULL DEFAULT 100,
    effective_date DATE NOT NULL,
    expiration_date DATE,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    version INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    CONSTRAINT fk_rule_regulation FOREIGN KEY (regulation_id) 
        REFERENCES regulations(regulation_id),
    CONSTRAINT fk_rule_template FOREIGN KEY (template_id) 
        REFERENCES regulation_templates(template_id)
);

CREATE INDEX idx_rules_regulation ON business_rules(regulation_id);
CREATE INDEX idx_rules_type ON business_rules(rule_type);
CREATE INDEX idx_rules_category ON business_rules(rule_category);
CREATE INDEX idx_rules_enabled ON business_rules(enabled);
CREATE INDEX idx_rules_dates ON business_rules(effective_date, expiration_date);
CREATE INDEX idx_rules_execution_order ON business_rules(execution_order);

-- Table: rule_parameters
-- Stores configurable parameters for rules
CREATE TABLE IF NOT EXISTS rule_parameters (
    parameter_id BIGSERIAL PRIMARY KEY,
    rule_id VARCHAR(100) NOT NULL,
    parameter_name VARCHAR(100) NOT NULL,
    parameter_value TEXT NOT NULL,
    parameter_type VARCHAR(50) NOT NULL CHECK (parameter_type IN ('NUMERIC', 'PERCENTAGE', 'FORMULA', 'CONDITION', 'THRESHOLD', 'LIST')),
    data_type VARCHAR(20),
    unit VARCHAR(20),
    min_value NUMERIC(20, 4),
    max_value NUMERIC(20, 4),
    description TEXT,
    is_configurable BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_parameter_rule FOREIGN KEY (rule_id) 
        REFERENCES business_rules(rule_id) ON DELETE CASCADE,
    CONSTRAINT unique_rule_parameter UNIQUE (rule_id, parameter_name)
);

CREATE INDEX idx_parameters_rule ON rule_parameters(rule_id);
CREATE INDEX idx_parameters_name ON rule_parameters(parameter_name);

-- Table: rule_exemptions
-- Stores exemptions from specific rules
CREATE TABLE IF NOT EXISTS rule_exemptions (
    exemption_id BIGSERIAL PRIMARY KEY,
    rule_id VARCHAR(100) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id VARCHAR(100),
    exemption_reason TEXT NOT NULL,
    exemption_type VARCHAR(50) NOT NULL CHECK (exemption_type IN ('PERMANENT', 'TEMPORARY', 'CONDITIONAL')),
    approved_by VARCHAR(100) NOT NULL,
    approval_date DATE NOT NULL,
    effective_date DATE NOT NULL,
    expiration_date DATE,
    conditions JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_exemption_rule FOREIGN KEY (rule_id) 
        REFERENCES business_rules(rule_id) ON DELETE CASCADE
);

CREATE INDEX idx_exemptions_rule ON rule_exemptions(rule_id);
CREATE INDEX idx_exemptions_entity ON rule_exemptions(entity_type, entity_id);
CREATE INDEX idx_exemptions_dates ON rule_exemptions(effective_date, expiration_date);

-- Table: rule_execution_log
-- Stores audit trail of rule executions
CREATE TABLE IF NOT EXISTS rule_execution_log (
    execution_id BIGSERIAL PRIMARY KEY,
    rule_id VARCHAR(100) NOT NULL,
    execution_timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    entity_type VARCHAR(50),
    entity_id VARCHAR(100),
    execution_result VARCHAR(20) NOT NULL CHECK (execution_result IN ('SUCCESS', 'FAILURE', 'ERROR', 'SKIPPED')),
    violation_count INTEGER DEFAULT 0,
    execution_time_ms BIGINT,
    context_data JSONB,
    error_message TEXT,
    executed_by VARCHAR(100),
    CONSTRAINT fk_execution_rule FOREIGN KEY (rule_id) 
        REFERENCES business_rules(rule_id)
);

CREATE INDEX idx_execution_rule ON rule_execution_log(rule_id);
CREATE INDEX idx_execution_timestamp ON rule_execution_log(execution_timestamp);
CREATE INDEX idx_execution_entity ON rule_execution_log(entity_type, entity_id);
CREATE INDEX idx_execution_result ON rule_execution_log(execution_result);

-- Table: rule_violations
-- Stores detected rule violations
CREATE TABLE IF NOT EXISTS rule_violations (
    violation_id BIGSERIAL PRIMARY KEY,
    rule_id VARCHAR(100) NOT NULL,
    execution_id BIGINT NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id VARCHAR(100) NOT NULL,
    violation_type VARCHAR(100) NOT NULL,
    violation_description TEXT NOT NULL,
    severity VARCHAR(20) NOT NULL CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    detected_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    violation_details JSONB,
    resolution_status VARCHAR(20) DEFAULT 'OPEN' CHECK (resolution_status IN ('OPEN', 'IN_PROGRESS', 'RESOLVED', 'WAIVED')),
    resolved_at TIMESTAMP,
    resolved_by VARCHAR(100),
    resolution_notes TEXT,
    CONSTRAINT fk_violation_rule FOREIGN KEY (rule_id) 
        REFERENCES business_rules(rule_id),
    CONSTRAINT fk_violation_execution FOREIGN KEY (execution_id) 
        REFERENCES rule_execution_log(execution_id)
);

CREATE INDEX idx_violations_rule ON rule_violations(rule_id);
CREATE INDEX idx_violations_execution ON rule_violations(execution_id);
CREATE INDEX idx_violations_entity ON rule_violations(entity_type, entity_id);
CREATE INDEX idx_violations_status ON rule_violations(resolution_status);
CREATE INDEX idx_violations_severity ON rule_violations(severity);
CREATE INDEX idx_violations_detected ON rule_violations(detected_at);

-- Comments for documentation
COMMENT ON TABLE regulations IS 'Stores regulatory frameworks and compliance requirements';
COMMENT ON TABLE regulation_templates IS 'Reusable rule templates with parameterized logic';
COMMENT ON TABLE business_rules IS 'Configurable business rules for data quality validation';
COMMENT ON TABLE rule_parameters IS 'Dynamic parameters for business rules';
COMMENT ON TABLE rule_exemptions IS 'Approved exemptions from specific rules';
COMMENT ON TABLE rule_execution_log IS 'Audit trail of all rule executions';
COMMENT ON TABLE rule_violations IS 'Detected violations of business rules';
