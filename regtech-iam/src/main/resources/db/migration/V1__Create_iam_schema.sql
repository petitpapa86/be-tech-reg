-- IAM Module Database Schema
-- Creates tables for user management, authentication, and authorization

-- Create IAM schema
CREATE SCHEMA IF NOT EXISTS iam;

-- Users table
CREATE TABLE iam.users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    status VARCHAR(50) NOT NULL,
    google_id VARCHAR(255),
    facebook_id VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

-- User roles table for authorization
CREATE TABLE iam.user_roles (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    role VARCHAR(50) NOT NULL,
    organization_id VARCHAR(255) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    FOREIGN KEY (user_id) REFERENCES iam.users(id) ON DELETE CASCADE
);

-- User bank assignments table
CREATE TABLE iam.user_bank_assignments (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    bank_id VARCHAR(255) NOT NULL,
    role VARCHAR(100) NOT NULL,
    assigned_at TIMESTAMP NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    FOREIGN KEY (user_id) REFERENCES iam.users(id) ON DELETE CASCADE
);

-- Domain events outbox table for reliable event publishing
CREATE TABLE iam.domain_events_outbox (
    id UUID PRIMARY KEY,
    aggregate_id VARCHAR(255) NOT NULL,
    aggregate_type VARCHAR(100) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    event_data TEXT NOT NULL,
    correlation_id VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    processed_at TIMESTAMP,
    processed BOOLEAN NOT NULL DEFAULT false,
    retry_count INTEGER NOT NULL DEFAULT 0,
    last_error TEXT
);

-- Indexes for performance
CREATE INDEX idx_users_email ON iam.users(email);
CREATE INDEX idx_users_status ON iam.users(status);
CREATE INDEX idx_users_google_id ON iam.users(google_id);
CREATE INDEX idx_users_facebook_id ON iam.users(facebook_id);

CREATE INDEX idx_user_roles_user_id ON iam.user_roles(user_id);
CREATE INDEX idx_user_roles_organization_id ON iam.user_roles(organization_id);
CREATE INDEX idx_user_roles_role ON iam.user_roles(role);
CREATE INDEX idx_user_roles_active ON iam.user_roles(active);

CREATE INDEX idx_user_bank_assignments_user_id ON iam.user_bank_assignments(user_id);
CREATE INDEX idx_user_bank_assignments_bank_id ON iam.user_bank_assignments(bank_id);

CREATE INDEX idx_domain_events_outbox_processed ON iam.domain_events_outbox(processed);
CREATE INDEX idx_domain_events_outbox_created_at ON iam.domain_events_outbox(created_at);
CREATE INDEX idx_domain_events_outbox_aggregate ON iam.domain_events_outbox(aggregate_id, aggregate_type);

-- Comments for documentation
COMMENT ON TABLE iam.users IS 'User accounts with authentication credentials';
COMMENT ON TABLE iam.user_roles IS 'User role assignments for authorization';
COMMENT ON TABLE iam.user_bank_assignments IS 'User assignments to specific banks';
COMMENT ON TABLE iam.domain_events_outbox IS 'Outbox pattern for reliable event publishing';

COMMENT ON COLUMN iam.users.status IS 'User status: PENDING_PAYMENT, ACTIVE, SUSPENDED, CANCELLED';
COMMENT ON COLUMN iam.user_roles.role IS 'Role enum: USER, PREMIUM_USER, BILLING_ADMIN, COMPLIANCE_OFFICER, ADMIN';
COMMENT ON COLUMN iam.user_roles.organization_id IS 'Organization/tenant identifier for multi-tenant support';