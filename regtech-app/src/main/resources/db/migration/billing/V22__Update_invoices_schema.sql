-- V22__Update_invoices_schema.sql
-- Update invoices table schema to match current domain model (InvoiceEntity)
-- The original V20 migration had outdated columns that don't match the entity

-- Drop old columns that are no longer used
ALTER TABLE billing.invoices DROP COLUMN IF EXISTS amount_due;
ALTER TABLE billing.invoices DROP COLUMN IF EXISTS currency;
ALTER TABLE billing.invoices DROP COLUMN IF EXISTS subscription_id;

-- Add new columns for subscription amount
ALTER TABLE billing.invoices ADD COLUMN IF NOT EXISTS subscription_amount_value DECIMAL(19, 4);
ALTER TABLE billing.invoices ADD COLUMN IF NOT EXISTS subscription_amount_currency VARCHAR(3) NOT NULL DEFAULT 'USD';

-- Add new columns for overage amount
ALTER TABLE billing.invoices ADD COLUMN IF NOT EXISTS overage_amount_value DECIMAL(19, 4);
ALTER TABLE billing.invoices ADD COLUMN IF NOT EXISTS overage_amount_currency VARCHAR(3) NOT NULL DEFAULT 'USD';

-- Add new columns for total amount
ALTER TABLE billing.invoices ADD COLUMN IF NOT EXISTS total_amount_value DECIMAL(19, 4);
ALTER TABLE billing.invoices ADD COLUMN IF NOT EXISTS total_amount_currency VARCHAR(3) NOT NULL DEFAULT 'USD';

-- Add new columns for billing period
ALTER TABLE billing.invoices ADD COLUMN IF NOT EXISTS billing_period_start_date DATE;
ALTER TABLE billing.invoices ADD COLUMN IF NOT EXISTS billing_period_end_date DATE;

-- Add new columns for invoice metadata
ALTER TABLE billing.invoices ADD COLUMN IF NOT EXISTS invoice_number VARCHAR(255);
ALTER TABLE billing.invoices ADD COLUMN IF NOT EXISTS issue_date DATE;
ALTER TABLE billing.invoices ADD COLUMN IF NOT EXISTS sent_at TIMESTAMP;

-- Create unique constraint on invoice_number
CREATE UNIQUE INDEX IF NOT EXISTS idx_invoices_invoice_number ON billing.invoices(invoice_number);

-- Update invoice_line_items table schema
-- Drop old columns that are no longer used
ALTER TABLE billing.invoice_line_items DROP COLUMN IF EXISTS amount;
ALTER TABLE billing.invoice_line_items DROP COLUMN IF EXISTS currency;

-- Add new columns for unit amount
ALTER TABLE billing.invoice_line_items ADD COLUMN IF NOT EXISTS unit_amount_value DECIMAL(19, 4) NOT NULL DEFAULT 0;
ALTER TABLE billing.invoice_line_items ADD COLUMN IF NOT EXISTS unit_amount_currency VARCHAR(3) NOT NULL DEFAULT 'USD';

-- Add new columns for total amount
ALTER TABLE billing.invoice_line_items ADD COLUMN IF NOT EXISTS total_amount_value DECIMAL(19, 4) NOT NULL DEFAULT 0;
ALTER TABLE billing.invoice_line_items ADD COLUMN IF NOT EXISTS total_amount_currency VARCHAR(3) NOT NULL DEFAULT 'USD';

-- Update comments
COMMENT ON TABLE billing.invoices IS 'Stores invoice information with detailed amount breakdown';
COMMENT ON COLUMN billing.invoices.subscription_amount_value IS 'Subscription fee amount';
COMMENT ON COLUMN billing.invoices.overage_amount_value IS 'Overage charges amount';
COMMENT ON COLUMN billing.invoices.total_amount_value IS 'Total invoice amount (subscription + overage)';
COMMENT ON COLUMN billing.invoices.billing_period_start_date IS 'Start date of the billing period';
COMMENT ON COLUMN billing.invoices.billing_period_end_date IS 'End date of the billing period';
COMMENT ON COLUMN billing.invoices.invoice_number IS 'Human-readable invoice number (e.g., INV-20251204-0001)';
COMMENT ON COLUMN billing.invoices.issue_date IS 'Date when the invoice was issued';

COMMENT ON TABLE billing.invoice_line_items IS 'Stores individual line items for invoices with unit and total amounts';
COMMENT ON COLUMN billing.invoice_line_items.unit_amount_value IS 'Unit price for this line item';
COMMENT ON COLUMN billing.invoice_line_items.total_amount_value IS 'Total amount (unit_amount * quantity)';
