# URGENT: Apply V45 Migration - Fix Quality Grade Column Length

## Problem

The application is failing with the following error when saving quality reports:

```
ERRORE: il valore è troppo lungo per il tipo character varying(2)
ERROR: value too long for type character varying(2)
```

## Root Cause

The `quality_grade` column in the `dataquality.quality_reports` table was created as `VARCHAR(2)` in migration V43, but the JPA entity uses `@Enumerated(EnumType.STRING)` which stores the full enum name (e.g., "ACCEPTABLE", "EXCELLENT") instead of just the letter grade (e.g., "C", "A+").

**Schema mismatch:**
- Database: `quality_grade VARCHAR(2)` - expects letter grades like "A+", "C", "F"
- Entity: `@Column(name = "quality_grade", length = 20)` - stores enum names like "ACCEPTABLE", "EXCELLENT", "POOR"

## Solution

Migration V45 increases the `quality_grade` column length from `VARCHAR(2)` to `VARCHAR(20)` to accommodate the enum names.

## How to Apply

### Option 1: PowerShell (Recommended for Windows)

```powershell
.\apply-v45-migration.ps1
```

### Option 2: Batch File

```cmd
apply-v45-migration.bat
```

### Option 3: Manual psql

```bash
psql -h localhost -p 5432 -U regtech_user -d regtech_db -f regtech-app/src/main/resources/db/migration/dataquality/V45__increase_quality_grade_length.sql
```

## What This Migration Does

```sql
ALTER TABLE dataquality.quality_reports 
ALTER COLUMN quality_grade TYPE VARCHAR(20);
```

This changes the column type to accommodate enum values:
- `EXCELLENT` (9 chars)
- `VERY_GOOD` (9 chars)
- `GOOD` (4 chars)
- `ACCEPTABLE` (10 chars)
- `POOR` (4 chars)

## Verification

After applying the migration, verify the column type:

```sql
SELECT column_name, data_type, character_maximum_length 
FROM information_schema.columns 
WHERE table_schema = 'dataquality' 
  AND table_name = 'quality_reports' 
  AND column_name = 'quality_grade';
```

Expected result:
```
 column_name  | data_type | character_maximum_length 
--------------+-----------+-------------------------
 quality_grade| character varying | 20
```

## Impact

- **Downtime**: None - this is a simple column type change
- **Data Loss**: None - existing data will be preserved
- **Rollback**: Can be rolled back by changing back to VARCHAR(2), but only if no data has been inserted with longer values

## Next Steps

After applying this migration:

1. Restart the application
2. Test quality report generation
3. Verify that quality grades are being stored correctly

## Related Files

- Migration: `regtech-app/src/main/resources/db/migration/dataquality/V45__increase_quality_grade_length.sql`
- Entity: `regtech-data-quality/infrastructure/src/main/java/com/bcbs239/regtech/dataquality/infrastructure/reporting/QualityReportEntity.java`
- Enum: `regtech-data-quality/domain/src/main/java/com/bcbs239/regtech/dataquality/domain/quality/QualityGrade.java`
