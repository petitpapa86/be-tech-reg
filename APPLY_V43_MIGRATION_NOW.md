# URGENT: Apply V43 Migration

## Problem
The application is failing to start with the following error:
```
Schema-validation: missing table [dataquality.quality_error_summaries]
```

## Root Cause
The `quality_reports` and `quality_error_summaries` tables were defined in `schema.sql` but never added to Flyway migrations. Hibernate's schema validation is correctly detecting that these tables don't exist in the database.

## Solution
A new Flyway migration `V43__create_quality_reports_tables.sql` has been created to add these missing tables.

## How to Apply

### Option 1: Windows Batch Script
```cmd
apply-v43-migration.bat
```

### Option 2: PowerShell Script
```powershell
.\apply-v43-migration.ps1
```

### Option 3: Manual Maven Command
```bash
mvn flyway:migrate -Dflyway.schemas=dataquality
```

## What This Migration Does

1. Creates `dataquality.quality_reports` table:
   - Stores quality validation reports for batches
   - Includes dimension scores (completeness, accuracy, consistency, etc.)
   - Tracks S3 storage locations for detailed reports
   - Records processing timestamps and durations

2. Creates `dataquality.quality_error_summaries` table:
   - Stores aggregated error summaries by dimension
   - Links to business rules via rule_code
   - Tracks affected exposure IDs
   - Categorizes by severity and dimension

3. Creates all necessary indexes for optimal query performance

## After Migration
Once the migration is applied successfully, restart your application. The schema validation error should be resolved.

## Verification
After applying the migration, you can verify the tables exist:

```sql
-- Check if tables exist
SELECT table_name 
FROM information_schema.tables 
WHERE table_schema = 'dataquality' 
  AND table_name IN ('quality_reports', 'quality_error_summaries');

-- Check table structure
DESCRIBE dataquality.quality_reports;
DESCRIBE dataquality.quality_error_summaries;
```

## Related Files
- Migration: `regtech-app/src/main/resources/db/migration/dataquality/V43__create_quality_reports_tables.sql`
- Entity: `regtech-data-quality/infrastructure/src/main/java/com/bcbs239/regtech/dataquality/infrastructure/reporting/QualityErrorSummaryEntity.java`
- Original schema: `regtech-data-quality/infrastructure/src/main/resources/schema.sql`
