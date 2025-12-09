-- V20__create_billing_tables.sql
-- Create billing schema tables for subscription and invoice management

-- Create schema if it doesn't exist
CREATE SCHEMA IF NOT EXISTS billing;

-- Billing Accounts table - stores customer billing account information
CREATE TABLE IF NOT EXISTS billing.billing_accounts (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    stripe_customer_id VARCHAR(255) UNIQUE,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_billing_accounts_user_id ON billing.billing_accounts(user_id);
CREATE INDEX idx_billing_accounts_stripe_customer_id ON billing.billing_accounts(stripe_customer_id);
CREATE INDEX idx_billing_accounts_status ON billing.billing_accounts(status);

-- Subscriptions table - stores subscription information
CREATE TABLE IF NOT EXISTS billing.subscriptions (
    id VARCHAR(36) PRIMARY KEY,
    billing_account_id VARCHAR(36) NOT NULL,
    stripe_subscription_id VARCHAR(255) UNIQUE,
    plan_id VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL,
    current_period_start TIMESTAMP,
    current_period_end TIMESTAMP,
    cancel_at_period_end BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    FOREIGN KEY (billing_account_id) REFERENCES billing.billing_accounts(id) ON DELETE CASCADE
);

CREATE INDEX idx_subscriptions_billing_account_id ON billing.subscriptions(billing_account_id);
CREATE INDEX idx_subscriptions_stripe_subscription_id ON billing.subscriptions(stripe_subscription_id);
CREATE INDEX idx_subscriptions_status ON billing.subscriptions(status);

-- Invoices table - stores invoice information
CREATE TABLE IF NOT EXISTS billing.invoices (
    id VARCHAR(36) PRIMARY KEY,
    billing_account_id VARCHAR(36) NOT NULL,
    stripe_invoice_id VARCHAR(255) UNIQUE,
    subscription_id VARCHAR(36),
    amount_due DECIMAL(10, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(20) NOT NULL,
    due_date DATE,
    paid_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    FOREIGN KEY (billing_account_id) REFERENCES billing.billing_accounts(id) ON DELETE CASCADE,
    FOREIGN KEY (subscription_id) REFERENCES billing.subscriptions(id) ON DELETE SET NULL
);

CREATE INDEX idx_invoices_billing_account_id ON billing.invoices(billing_account_id);
CREATE INDEX idx_invoices_stripe_invoice_id ON billing.invoices(stripe_invoice_id);
CREATE INDEX idx_invoices_subscription_id ON billing.invoices(subscription_id);
CREATE INDEX idx_invoices_status ON billing.invoices(status);
CREATE INDEX idx_invoices_due_date ON billing.invoices(due_date);

-- Invoice Line Items table - stores individual line items for invoices
CREATE TABLE IF NOT EXISTS billing.invoice_line_items (
    id VARCHAR(36) PRIMARY KEY,
    invoice_id VARCHAR(36) NOT NULL,
    description VARCHAR(500) NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    quantity INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (invoice_id) REFERENCES billing.invoices(id) ON DELETE CASCADE
);

CREATE INDEX idx_invoice_line_items_invoice_id ON billing.invoice_line_items(invoice_id);

-- Dunning Cases table - stores dunning case information for overdue payments
CREATE TABLE IF NOT EXISTS billing.dunning_cases (
    id VARCHAR(36) PRIMARY KEY,
    billing_account_id VARCHAR(36) NOT NULL,
    invoice_id VARCHAR(36) NOT NULL,
    status VARCHAR(20) NOT NULL,
    current_step INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    FOREIGN KEY (billing_account_id) REFERENCES billing.billing_accounts(id) ON DELETE CASCADE,
    FOREIGN KEY (invoice_id) REFERENCES billing.invoices(id) ON DELETE CASCADE
);

CREATE INDEX idx_dunning_cases_billing_account_id ON billing.dunning_cases(billing_account_id);
CREATE INDEX idx_dunning_cases_invoice_id ON billing.dunning_cases(invoice_id);
CREATE INDEX idx_dunning_cases_status ON billing.dunning_cases(status);

-- Dunning Actions table - stores actions taken during dunning process
CREATE TABLE IF NOT EXISTS billing.dunning_actions (
    id VARCHAR(36) PRIMARY KEY,
    dunning_case_id VARCHAR(36) NOT NULL,
    action_type VARCHAR(50) NOT NULL,
    step_number INTEGER NOT NULL,
    executed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    result VARCHAR(20) NOT NULL,
    notes TEXT,
    FOREIGN KEY (dunning_case_id) REFERENCES billing.dunning_cases(id) ON DELETE CASCADE
);

CREATE INDEX idx_dunning_actions_dunning_case_id ON billing.dunning_actions(dunning_case_id);
CREATE INDEX idx_dunning_actions_executed_at ON billing.dunning_actions(executed_at);

-- Processed Webhook Events table - stores processed webhook events for idempotency
CREATE TABLE IF NOT EXISTS billing.processed_webhook_events (
    id VARCHAR(36) PRIMARY KEY,
    event_id VARCHAR(255) NOT NULL UNIQUE,
    event_type VARCHAR(100) NOT NULL,
    processed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    payload TEXT
);

CREATE INDEX idx_processed_webhook_events_event_id ON billing.processed_webhook_events(event_id);
CREATE INDEX idx_processed_webhook_events_event_type ON billing.processed_webhook_events(event_type);
CREATE INDEX idx_processed_webhook_events_processed_at ON billing.processed_webhook_events(processed_at);

-- Saga Audit Log table - stores saga execution audit trail
CREATE TABLE IF NOT EXISTS billing.saga_audit_log (
    id BIGSERIAL PRIMARY KEY,
    saga_id VARCHAR(255) NOT NULL,
    saga_type VARCHAR(100) NOT NULL,
    step_name VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL,
    executed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    error_message TEXT,
    payload TEXT
);

CREATE INDEX idx_saga_audit_log_saga_id ON billing.saga_audit_log(saga_id);
CREATE INDEX idx_saga_audit_log_saga_type ON billing.saga_audit_log(saga_type);
CREATE INDEX idx_saga_audit_log_executed_at ON billing.saga_audit_log(executed_at);

-- Billing Domain Events table - stores domain events for billing context
CREATE TABLE IF NOT EXISTS billing.billing_domain_events (
    id BIGSERIAL PRIMARY KEY,
    event_id VARCHAR(255) NOT NULL UNIQUE,
    event_type VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(255) NOT NULL,
    aggregate_type VARCHAR(100) NOT NULL,
    occurred_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    payload TEXT NOT NULL,
    published BOOLEAN NOT NULL DEFAULT FALSE,
    published_at TIMESTAMP
);

CREATE INDEX idx_billing_domain_events_event_id ON billing.billing_domain_events(event_id);
CREATE INDEX idx_billing_domain_events_aggregate ON billing.billing_domain_events(aggregate_type, aggregate_id);
CREATE INDEX idx_billing_domain_events_published ON billing.billing_domain_events(published);
CREATE INDEX idx_billing_domain_events_occurred_at ON billing.billing_domain_events(occurred_at);

-- Comments for documentation
COMMENT ON TABLE billing.billing_accounts IS 'Stores customer billing account information';
COMMENT ON TABLE billing.subscriptions IS 'Stores subscription information linked to billing accounts';
COMMENT ON TABLE billing.invoices IS 'Stores invoice information for billing accounts';
COMMENT ON TABLE billing.invoice_line_items IS 'Stores individual line items for invoices';
COMMENT ON TABLE billing.dunning_cases IS 'Stores dunning cases for overdue payments';
COMMENT ON TABLE billing.dunning_actions IS 'Stores actions taken during the dunning process';
COMMENT ON TABLE billing.processed_webhook_events IS 'Stores processed webhook events for idempotency';
COMMENT ON TABLE billing.saga_audit_log IS 'Stores saga execution audit trail';
COMMENT ON TABLE billing.billing_domain_events IS 'Stores domain events for the billing context';
