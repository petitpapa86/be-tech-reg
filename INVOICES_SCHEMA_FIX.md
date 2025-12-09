# Invoices Schema Fix - V22 Migration

## Problem

The application was failing when trying to insert invoices into the database with the following error:

```
ERRORE: il valore nullo nella colonna "amount_due" della relazione "invoices" viola il vincolo non nullo
(ERROR: null value in column "amount_due" of relation "invoices" violates not-null constraint)
```

## Root Cause

There was a **schema mismatch** between the database and the domain model:

### Database Schema (V20 - Old)
The `billing.invoices` table had these columns:
- `amount_due` (DECIMAL, NOT NULL)
- `currency` (VARCHAR, NOT NULL)
- `subscription_id` (VARCHAR)

### Domain Model & JPA Entity (Current)
The `Invoice` domain entity and `InvoiceEntity` JPA mapping expect:
- `subscription_amount_value` + `subscription_amount_currency`
- `overage_amount_value` + `overage_amount_currency`
- `total_amount_value` + `total_amount_currency`
- `billing_period_start_date` + `billing_period_end_date`
- `invoice_number`, `issue_date`, `sent_at`

When the application tried to save an invoice, JPA attempted to insert values into columns that matched the entity fields, but the database still had the old schema. This resulted in:
1. The `amount_due` column receiving NULL (because JPA wasn't mapping to it)
2. The database rejecting the insert due to the NOT NULL constraint

## Solution

Created **V22 migration** to update the invoices table schema to match the current domain model.

### Migration File
`regtech-app/src/main/resources/db/migration/billing/V22__Update_invoices_schema.sql`

### Changes Applied

**Invoices table:**

Removed columns:
- `amount_due`
- `currency`
- `subscription_id`

Added columns:
- `subscription_amount_value` (DECIMAL 19,4)
- `subscription_amount_currency` (VARCHAR 3, NOT NULL, DEFAULT 'USD')
- `overage_amount_value` (DECIMAL 19,4)
- `overage_amount_currency` (VARCHAR 3, NOT NULL, DEFAULT 'USD')
- `total_amount_value` (DECIMAL 19,4)
- `total_amount_currency` (VARCHAR 3, NOT NULL, DEFAULT 'USD')
- `billing_period_start_date` (DATE)
- `billing_period_end_date` (DATE)
- `invoice_number` (VARCHAR 255, UNIQUE)
- `issue_date` (DATE)
- `sent_at` (TIMESTAMP)

**Invoice Line Items table:**

Removed columns:
- `amount`
- `currency`

Added columns:
- `unit_amount_value` (DECIMAL 19,4, NOT NULL)
- `unit_amount_currency` (VARCHAR 3, NOT NULL, DEFAULT 'USD')
- `total_amount_value` (DECIMAL 19,4, NOT NULL)
- `total_amount_currency` (VARCHAR 3, NOT NULL, DEFAULT 'USD')

## How to Apply

### Option 1: Using PowerShell (Recommended for Windows)
```powershell
.\apply-v22-migration.ps1
```

### Option 2: Using Batch Script
```cmd
apply-v22-migration.bat
```

### Option 3: Manual Application
```bash
psql -h localhost -p 5432 -U your_user -d your_database \
  -f regtech-app/src/main/resources/db/migration/billing/V22__Update_invoices_schema.sql
```

### Option 4: Let Flyway Apply Automatically
If you have Flyway configured in your application, it will automatically detect and apply the V22 migration on the next application startup.

## Verification

After applying the migration, verify the schema:

```sql
-- Check the invoices table structure
\d billing.invoices

-- Verify new columns exist
SELECT column_name, data_type, is_nullable 
FROM information_schema.columns 
WHERE table_schema = 'billing' 
  AND table_name = 'invoices'
ORDER BY ordinal_position;
```

Expected columns in `invoices` table:
- `subscription_amount_value`
- `subscription_amount_currency`
- `overage_amount_value`
- `overage_amount_currency`
- `total_amount_value`
- `total_amount_currency`
- `billing_period_start_date`
- `billing_period_end_date`
- `invoice_number`
- `issue_date`
- `sent_at`

Expected columns in `invoice_line_items` table:
- `unit_amount_value`
- `unit_amount_currency`
- `total_amount_value`
- `total_amount_currency`

## Impact

- **Breaking Change**: Yes - existing invoice data will need to be migrated if any exists
- **Data Loss**: The old columns (`amount_due`, `currency` from invoices; `amount`, `currency` from invoice_line_items) will be dropped
- **Backward Compatibility**: No - the application requires this schema to function

## Related Files

- Migration: `regtech-app/src/main/resources/db/migration/billing/V22__Update_invoices_schema.sql`
- Domain Models: 
  - `regtech-billing/domain/src/main/java/com/bcbs239/regtech/billing/domain/invoices/Invoice.java`
  - `regtech-billing/domain/src/main/java/com/bcbs239/regtech/billing/domain/invoices/InvoiceLineItem.java`
- JPA Entities: 
  - `regtech-billing/infrastructure/src/main/java/com/bcbs239/regtech/billing/infrastructure/database/entities/InvoiceEntity.java`
  - `regtech-billing/infrastructure/src/main/java/com/bcbs239/regtech/billing/infrastructure/database/entities/InvoiceLineItemEntity.java`
- Application Scripts: `apply-v22-migration.ps1`, `apply-v22-migration.bat`

## Notes

- This migration follows the same pattern as V21 which updated the subscriptions table
- The migration uses `IF EXISTS` and `IF NOT EXISTS` clauses for idempotency
- Default values are provided for currency columns to handle any edge cases
- The `invoice_number` column has a unique index for data integrity

## Next Steps

1. Apply the V22 migration using one of the methods above
2. Restart your application
3. Test invoice creation to verify the fix
4. Monitor logs for any remaining schema-related issues
