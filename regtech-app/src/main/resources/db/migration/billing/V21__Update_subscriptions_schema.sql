-- V21__Update_subscriptions_schema.sql
-- Update subscriptions table schema to match current domain model (SubscriptionEntity)
-- The original V20 migration had outdated columns that don't match the entity

-- Drop the old subscriptions table and recreate with correct schema
DROP TABLE IF EXISTS billing.subscriptions CASCADE;

-- Recreate subscriptions table with correct schema matching SubscriptionEntity
CREATE TABLE billing.subscriptions (
    id VARCHAR(36) PRIMARY KEY,
    billing_account_id VARCHAR(36),
    stripe_subscription_id VARCHAR(255) NOT NULL,
    tier VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    FOREIGN KEY (billing_account_id) REFERENCES billing.billing_accounts(id) ON DELETE CASCADE
);

-- Recreate indexes
CREATE INDEX idx_subscriptions_billing_account_id ON billing.subscriptions(billing_account_id);
CREATE INDEX idx_subscriptions_stripe_subscription_id ON billing.subscriptions(stripe_subscription_id);
CREATE INDEX idx_subscriptions_status ON billing.subscriptions(status);
CREATE INDEX idx_subscriptions_tier ON billing.subscriptions(tier);

-- Update comment
COMMENT ON TABLE billing.subscriptions IS 'Stores subscription information with tier-based pricing model';
COMMENT ON COLUMN billing.subscriptions.tier IS 'Subscription tier: STARTER, PROFESSIONAL, ENTERPRISE';
COMMENT ON COLUMN billing.subscriptions.status IS 'Subscription status: PENDING, ACTIVE, PAST_DUE, PAUSED, CANCELLED';
COMMENT ON COLUMN billing.subscriptions.billing_account_id IS 'Optional reference to billing account (nullable for subscriptions created before account)';
