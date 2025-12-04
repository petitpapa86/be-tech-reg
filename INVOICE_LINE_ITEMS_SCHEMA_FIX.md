# Invoice Line Items Schema Fix - Included in V22 Migration

## Problem

After fixing the invoices table schema, a second error appeared:

```
ERRORE: il valore nullo nella colonna "amount" della relazione "invoice_line_items" viola il vincolo non nullo
(ERROR: null value in column "amount" of relation "invoice_line_items" violates not-null constraint)
```

## Root Cause

Same pattern as the invoices table - the `invoice_line_items` table had old schema columns that didn't match the JPA entity:

### Database Schema (V20 - Old)
- `amount` (DECIMAL, NOT NULL)
- `currency` (VARCHAR, NOT NULL)

### JPA Entity (Current)
- `unit_amount_value` + `unit_amount_currency`
- `total_amount_value` + `total_amount_currency`

## Solution

The V22 migration has been **updated** to also fix the `invoice_line_items` table schema.

### Changes to Invoice Line Items Table

**Removed columns:**
- `amount`
- `currency`

**Added columns:**
- `unit_amount_value` (DECIMAL 19,4, NOT NULL, DEFAULT 0)
- `unit_amount_currency` (VARCHAR 3, NOT NULL, DEFAULT 'USD')
- `total_amount_value` (DECIMAL 19,4, NOT NULL, DEFAULT 0)
- `total_amount_currency` (VARCHAR 3, NOT NULL, DEFAULT 'USD')

## How to Apply

The fix is included in the V22 migration. Simply apply it using one of these methods:

### Option 1: PowerShell
```powershell
.\apply-v22-migration.ps1
```

### Option 2: Batch Script
```cmd
apply-v22-migration.bat
```

### Option 3: Let Flyway Apply Automatically
Restart your application - Flyway will detect and apply the V22 migration automatically.

## What This Fixes

Both errors are now resolved:
1. ✅ Invoice creation - `amount_due` NULL error
2. ✅ Invoice line items creation - `amount` NULL error

The V22 migration updates both tables in a single transaction.

## Related Files

- Migration: `regtech-app/src/main/resources/db/migration/billing/V22__Update_invoices_schema.sql`
- Domain Model: `regtech-billing/domain/src/main/java/com/bcbs239/regtech/billing/domain/invoices/InvoiceLineItem.java`
- JPA Entity: `regtech-billing/infrastructure/src/main/java/com/bcbs239/regtech/billing/infrastructure/database/entities/InvoiceLineItemEntity.java`
- Full Documentation: `INVOICES_SCHEMA_FIX.md`

## Note

This is a **comprehensive fix** that updates both the `invoices` and `invoice_line_items` tables to match the current domain model. After applying this migration, invoice creation should work without any schema-related errors.
