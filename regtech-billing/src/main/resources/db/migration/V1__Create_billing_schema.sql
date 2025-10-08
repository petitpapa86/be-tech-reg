-- Billing Context Database Schema
-- This migration creates all tables required for the billing context

-- Billing Accounts table
CREATE TABLE billing_accounts (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    stripe_customer_id VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    default_payment_method_id VARCHAR(255),
    account_balance_amount DECIMAL(19,4) NOT NULL DEFAULT 0.0000,
    account_balance_currency VARCHAR(3) NOT NULL DEFAULT 'EUR',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    
    CONSTRAINT uk_billing_accounts_user_id UNIQUE (user_id),
    CONSTRAINT uk_billing_accounts_stripe_customer_id UNIQUE (stripe_customer_id),
    CONSTRAINT chk_billing_accounts_status CHECK (status IN ('PENDING_VERIFICATION', 'ACTIVE', 'PAST_DUE', 'SUSPENDED', 'CANCELLED')),
    CONSTRAINT chk_billing_accounts_currency CHECK (account_balance_currency IN ('EUR', 'USD', 'GBP'))
);

-- Subscriptions table
CREATE TABLE subscriptions (
    id VARCHAR(36) PRIMARY KEY,
    billing_account_id VARCHAR(36) NOT NULL,
    stripe_subscription_id VARCHAR(255) NOT NULL,
    tier VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    
    CONSTRAINT fk_subscriptions_billing_account FOREIGN KEY (billing_account_id) REFERENCES billing_accounts(id) ON DELETE CASCADE,
    CONSTRAINT uk_subscriptions_stripe_subscription_id UNIQUE (stripe_subscription_id),
    CONSTRAINT chk_subscriptions_tier CHECK (tier IN ('STARTER')),
    CONSTRAINT chk_subscriptions_status CHECK (status IN ('ACTIVE', 'PAST_DUE', 'CANCELLED', 'SUSPENDED'))
);

-- Invoices table
CREATE TABLE invoices (
    id VARCHAR(36) PRIMARY KEY,
    billing_account_id VARCHAR(36) NOT NULL,
    invoice_number VARCHAR(50) NOT NULL,
    stripe_invoice_id VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    subscription_amount_value DECIMAL(19,4) NOT NULL,
    subscription_amount_currency VARCHAR(3) NOT NULL DEFAULT 'EUR',
    overage_amount_value DECIMAL(19,4) NOT NULL DEFAULT 0.0000,
    overage_amount_currency VARCHAR(3) NOT NULL DEFAULT 'EUR',
    total_amount_value DECIMAL(19,4) NOT NULL,
    total_amount_currency VARCHAR(3) NOT NULL DEFAULT 'EUR',
    billing_period_start_date DATE NOT NULL,
    billing_period_end_date DATE NOT NULL,
    issue_date DATE NOT NULL,
    due_date DATE NOT NULL,
    paid_at TIMESTAMP,
    sent_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    
    CONSTRAINT fk_invoices_billing_account FOREIGN KEY (billing_account_id) REFERENCES billing_accounts(id) ON DELETE CASCADE,
    CONSTRAINT uk_invoices_invoice_number UNIQUE (invoice_number),
    CONSTRAINT uk_invoices_stripe_invoice_id UNIQUE (stripe_invoice_id),
    CONSTRAINT chk_invoices_status CHECK (status IN ('PENDING', 'PAID', 'FAILED', 'OVERDUE', 'CANCELLED')),
    CONSTRAINT chk_invoices_subscription_currency CHECK (subscription_amount_currency IN ('EUR', 'USD', 'GBP')),
    CONSTRAINT chk_invoices_overage_currency CHECK (overage_amount_currency IN ('EUR', 'USD', 'GBP')),
    CONSTRAINT chk_invoices_total_currency CHECK (total_amount_currency IN ('EUR', 'USD', 'GBP'))
);

-- Invoice Line Items table
CREATE TABLE invoice_line_items (
    id VARCHAR(36) PRIMARY KEY,
    invoice_id VARCHAR(36) NOT NULL,
    description VARCHAR(500) NOT NULL,
    unit_amount_value DECIMAL(19,4) NOT NULL,
    unit_amount_currency VARCHAR(3) NOT NULL DEFAULT 'EUR',
    quantity INTEGER NOT NULL DEFAULT 1,
    total_amount_value DECIMAL(19,4) NOT NULL,
    total_amount_currency VARCHAR(3) NOT NULL DEFAULT 'EUR',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_invoice_line_items_invoice FOREIGN KEY (invoice_id) REFERENCES invoices(id) ON DELETE CASCADE,
    CONSTRAINT chk_invoice_line_items_unit_currency CHECK (unit_amount_currency IN ('EUR', 'USD', 'GBP')),
    CONSTRAINT chk_invoice_line_items_total_currency CHECK (total_amount_currency IN ('EUR', 'USD', 'GBP')),
    CONSTRAINT chk_invoice_line_items_quantity CHECK (quantity > 0)
);

-- Dunning Cases table
CREATE TABLE dunning_cases (
    id VARCHAR(36) PRIMARY KEY,
    billing_account_id VARCHAR(36) NOT NULL,
    invoice_id VARCHAR(36) NOT NULL,
    status VARCHAR(50) NOT NULL,
    current_step VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    
    CONSTRAINT fk_dunning_cases_billing_account FOREIGN KEY (billing_account_id) REFERENCES billing_accounts(id) ON DELETE CASCADE,
    CONSTRAINT fk_dunning_cases_invoice FOREIGN KEY (invoice_id) REFERENCES invoices(id) ON DELETE CASCADE,
    CONSTRAINT chk_dunning_cases_status CHECK (status IN ('IN_PROGRESS', 'RESOLVED', 'ESCALATED')),
    CONSTRAINT chk_dunning_cases_step CHECK (current_step IN ('STEP_1_REMINDER', 'STEP_2_WARNING', 'STEP_3_FINAL_NOTICE', 'STEP_4_SUSPENSION'))
);

-- Dunning Actions table
CREATE TABLE dunning_actions (
    id VARCHAR(36) PRIMARY KEY,
    dunning_case_id VARCHAR(36) NOT NULL,
    step VARCHAR(50) NOT NULL,
    action_type VARCHAR(50) NOT NULL,
    executed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    result VARCHAR(50) NOT NULL,
    notes TEXT,
    
    CONSTRAINT fk_dunning_actions_dunning_case FOREIGN KEY (dunning_case_id) REFERENCES dunning_cases(id) ON DELETE CASCADE,
    CONSTRAINT chk_dunning_actions_step CHECK (step IN ('STEP_1_REMINDER', 'STEP_2_WARNING', 'STEP_3_FINAL_NOTICE', 'STEP_4_SUSPENSION')),
    CONSTRAINT chk_dunning_actions_type CHECK (action_type IN ('EMAIL_REMINDER', 'PAYMENT_RETRY', 'ACCOUNT_SUSPENSION', 'COLLECTION_NOTICE')),
    CONSTRAINT chk_dunning_actions_result CHECK (result IN ('SUCCESS', 'FAILURE', 'PENDING'))
);

-- Processed Webhook Events table (for idempotency)
CREATE TABLE processed_webhook_events (
    id VARCHAR(36) PRIMARY KEY,
    stripe_event_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    processed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    result VARCHAR(50) NOT NULL,
    error_message TEXT,
    
    CONSTRAINT uk_processed_webhook_events_stripe_event_id UNIQUE (stripe_event_id),
    CONSTRAINT chk_processed_webhook_events_result CHECK (result IN ('SUCCESS', 'FAILURE'))
);

-- Saga Audit Log table (for compliance and monitoring)
CREATE TABLE saga_audit_log (
    id VARCHAR(36) PRIMARY KEY,
    saga_id VARCHAR(255) NOT NULL,
    saga_type VARCHAR(100) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    event_data TEXT NOT NULL,
    user_id VARCHAR(36),
    billing_account_id VARCHAR(36),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_saga_audit_log_billing_account FOREIGN KEY (billing_account_id) REFERENCES billing_accounts(id) ON DELETE SET NULL
);

-- Performance Indexes
-- Billing Accounts indexes
CREATE INDEX idx_billing_accounts_user_id ON billing_accounts(user_id);
CREATE INDEX idx_billing_accounts_status ON billing_accounts(status);
CREATE INDEX idx_billing_accounts_created_at ON billing_accounts(created_at);

-- Subscriptions indexes
CREATE INDEX idx_subscriptions_billing_account_id ON subscriptions(billing_account_id);
CREATE INDEX idx_subscriptions_status ON subscriptions(status);
CREATE INDEX idx_subscriptions_start_date ON subscriptions(start_date);
CREATE INDEX idx_subscriptions_end_date ON subscriptions(end_date);

-- Invoices indexes
CREATE INDEX idx_invoices_billing_account_id ON invoices(billing_account_id);
CREATE INDEX idx_invoices_status ON invoices(status);
CREATE INDEX idx_invoices_due_date ON invoices(due_date);
CREATE INDEX idx_invoices_issue_date ON invoices(issue_date);
CREATE INDEX idx_invoices_billing_period ON invoices(billing_period_start_date, billing_period_end_date);

-- Invoice Line Items indexes
CREATE INDEX idx_invoice_line_items_invoice_id ON invoice_line_items(invoice_id);

-- Dunning Cases indexes
CREATE INDEX idx_dunning_cases_billing_account_id ON dunning_cases(billing_account_id);
CREATE INDEX idx_dunning_cases_invoice_id ON dunning_cases(invoice_id);
CREATE INDEX idx_dunning_cases_status ON dunning_cases(status);
CREATE INDEX idx_dunning_cases_current_step ON dunning_cases(current_step);
CREATE INDEX idx_dunning_cases_created_at ON dunning_cases(created_at);

-- Dunning Actions indexes
CREATE INDEX idx_dunning_actions_dunning_case_id ON dunning_actions(dunning_case_id);
CREATE INDEX idx_dunning_actions_executed_at ON dunning_actions(executed_at);

-- Processed Webhook Events indexes
CREATE INDEX idx_processed_webhook_events_event_type ON processed_webhook_events(event_type);
CREATE INDEX idx_processed_webhook_events_processed_at ON processed_webhook_events(processed_at);

-- Saga Audit Log indexes
CREATE INDEX idx_saga_audit_log_saga_id ON saga_audit_log(saga_id);
CREATE INDEX idx_saga_audit_log_saga_type ON saga_audit_log(saga_type);
CREATE INDEX idx_saga_audit_log_user_id ON saga_audit_log(user_id);
CREATE INDEX idx_saga_audit_log_billing_account_id ON saga_audit_log(billing_account_id);
CREATE INDEX idx_saga_audit_log_created_at ON saga_audit_log(created_at);