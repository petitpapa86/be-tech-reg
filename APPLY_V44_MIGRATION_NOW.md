# V44 Migration - Increase report_id Column Length

## Problem
The `quality_reports.report_id` column is defined as `VARCHAR(36)` but the application generates IDs with format `qr_<uuid>` which is 41 characters long, causing this error:

```
ERRORE: il valore è troppo lungo per il tipo character varying(36)
```

## Solution
Increase the `report_id` column length from `VARCHAR(36)` to `VARCHAR(50)`.

## Apply Migration

### Windows (PowerShell)
```powershell
.\apply-v44-migration.ps1
```

### Windows (CMD)
```cmd
apply-v44-migration.bat
```

### Manual Application
```bash
psql -h localhost -p 5432 -U regtech_user -d regtech_db -f regtech-app/src/main/resources/db/migration/dataquality/V44__increase_report_id_length.sql
```

## Verification
After applying the migration, verify the column type:

```sql
SELECT column_name, data_type, character_maximum_length 
FROM information_schema.columns 
WHERE table_schema = 'dataquality' 
  AND table_name = 'quality_reports' 
  AND column_name = 'report_id';
```

Expected result:
- `data_type`: character varying
- `character_maximum_length`: 50

## Impact
- **Downtime**: None (ALTER COLUMN is fast for varchar length increase)
- **Data Loss**: None
- **Rollback**: Can decrease back to VARCHAR(36) if no data exists, but not recommended

## Next Steps
After applying this migration, restart your application to clear the failed transaction state.
