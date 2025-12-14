-- Add columns required by BillingAccountEntity

ALTER TABLE billing.billing_accounts
    ADD COLUMN IF NOT EXISTS default_payment_method_id VARCHAR(255);

ALTER TABLE billing.billing_accounts
    ADD COLUMN IF NOT EXISTS account_balance_amount NUMERIC(19, 4);

ALTER TABLE billing.billing_accounts
    ADD COLUMN IF NOT EXISTS account_balance_currency VARCHAR(3);

CREATE INDEX IF NOT EXISTS idx_billing_accounts_default_payment_method_id
    ON billing.billing_accounts (default_payment_method_id);
