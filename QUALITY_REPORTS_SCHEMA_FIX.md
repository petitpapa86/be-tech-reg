# Quality Reports Schema Fix - V44 Migration

## Issue Summary
The application was failing with a PostgreSQL error when trying to insert quality reports:

```
ERRORE: il valore è troppo lungo per il tipo character varying(36)
```

**Root Cause**: The `dataquality.quality_reports.report_id` column was defined as `VARCHAR(36)`, but the application generates report IDs with the format `qr_<uuid>`, which is 41 characters long.

**Example**: `qr_e407ac2e-e8c8-44d8-94dc-2487413e6e79` (41 chars)

## Solution
Created migration V44 to increase the column length from `VARCHAR(36)` to `VARCHAR(50)`.

## Files Created
1. **Migration SQL**: `regtech-app/src/main/resources/db/migration/dataquality/V44__increase_report_id_length.sql`
2. **PowerShell Script**: `apply-v44-migration.ps1`
3. **Batch Script**: `apply-v44-migration.bat`
4. **Documentation**: `APPLY_V44_MIGRATION_NOW.md`

## How to Apply

### Option 1: Using PowerShell (Recommended for Windows)
```powershell
.\apply-v44-migration.ps1
```

### Option 2: Using Batch Script
```cmd
apply-v44-migration.bat
```

### Option 3: Manual Application
```bash
psql -h localhost -p 5432 -U regtech_user -d regtech_db \
  -f regtech-app/src/main/resources/db/migration/dataquality/V44__increase_report_id_length.sql
```

## Verification Steps

### 1. Check Column Definition
```sql
SELECT column_name, data_type, character_maximum_length 
FROM information_schema.columns 
WHERE table_schema = 'dataquality' 
  AND table_name = 'quality_reports' 
  AND column_name = 'report_id';
```

Expected: `character_maximum_length = 50`

### 2. Test Insert
```sql
INSERT INTO dataquality.quality_reports (
    report_id, batch_id, bank_id, status, compliance_status, created_at, updated_at, version
) VALUES (
    'qr_e407ac2e-e8c8-44d8-94dc-2487413e6e79',
    'test_batch_id',
    'test_bank_id',
    'IN_PROGRESS',
    FALSE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    0
);

-- Clean up test data
DELETE FROM dataquality.quality_reports WHERE batch_id = 'test_batch_id';
```

## Impact Assessment
- **Downtime**: None (ALTER COLUMN for varchar length increase is instant)
- **Data Loss**: None
- **Backward Compatibility**: Fully compatible (existing data remains valid)
- **Performance**: No impact (varchar length increase doesn't affect performance)

## Related Issues
This fix resolves the transaction abort error that was preventing quality report creation and causing cascading failures in the data quality validation pipeline.

## Next Steps
1. Apply the migration using one of the methods above
2. Restart the application to clear any failed transaction states
3. Monitor the logs to confirm quality reports are being created successfully
4. Verify that the data quality validation pipeline completes without errors

## Additional Notes
- The migration is idempotent and safe to run multiple times
- No other columns in the database have this issue (verified all VARCHAR(36) columns)
- The V43 migration that created this table should be updated in future to use VARCHAR(50) from the start
