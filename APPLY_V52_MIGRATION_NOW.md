# Apply V52 Migration - Increase counterparty_lei Length

## Issue
The `counterparty_lei` column in `riskcalculation.exposures` is currently `VARCHAR(20)`, which is the standard LEI length. However, test data or edge cases may contain longer values, causing insertion failures:

```
ERRORE: il valore è troppo lungo per il tipo character varying(20)
```

## Solution
Migration V52 increases the column length to `VARCHAR(30)` to handle edge cases while maintaining reasonable constraints.

## Apply Migration

### Windows (PowerShell)
```powershell
.\apply-v52-migration.ps1
```

### Windows (CMD)
```cmd
apply-v52-migration.bat
```

### Verify Migration
After applying, verify the change:

```sql
SELECT column_name, data_type, character_maximum_length 
FROM information_schema.columns 
WHERE table_schema = 'riskcalculation' 
  AND table_name = 'exposures' 
  AND column_name = 'counterparty_lei';
```

Expected result:
- `character_maximum_length`: 30

## Note on LEI Codes
Legal Entity Identifier (LEI) codes are standardized at exactly 20 characters per ISO 17442. The increase to 30 characters provides flexibility for:
- Test data
- Future extensions
- Edge cases in data ingestion

## Rollback (if needed)
```sql
ALTER TABLE riskcalculation.exposures 
    ALTER COLUMN counterparty_lei TYPE VARCHAR(20);
```

**Warning**: Rollback will fail if any existing data exceeds 20 characters.
