# 🚨 URGENT: Apply V22 Migration to Fix Invoice Creation

## Quick Fix

Your application is failing to create invoices because the database schema doesn't match the domain model. Apply this migration immediately:

### Windows PowerShell (Recommended)
```powershell
.\apply-v22-migration.ps1
```

### Windows Command Prompt
```cmd
apply-v22-migration.bat
```

### Or Let Flyway Handle It
Just restart your application - Flyway will automatically apply the migration.

## What This Fixes

The error you're seeing:
```
ERRORE: il valore nullo nella colonna "amount_due" della relazione "invoices" viola il vincolo non nullo
```

This happens because the database has an old `amount_due` column, but the application is trying to use new columns like `subscription_amount_value`, `overage_amount_value`, and `total_amount_value`.

## After Applying

1. Restart your application
2. Try creating an invoice again
3. The error should be resolved

## More Details

See `INVOICES_SCHEMA_FIX.md` for complete documentation.
