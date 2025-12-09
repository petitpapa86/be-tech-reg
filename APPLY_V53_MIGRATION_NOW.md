# Apply V53 Migration - Add calculation_results_uri Column

## Overview

This migration adds the `calculation_results_uri` column to the `riskcalculation.batches` table as part of the Risk Calculation Storage Refactoring. This column will store the S3 URI or filesystem path to JSON files containing complete calculation results, establishing JSON files as the single source of truth for detailed calculation data.

## Migration Details

**Migration File**: `regtech-app/src/main/resources/db/migration/riskcalculation/V53__Add_calculation_results_uri_to_batches.sql`

**Changes**:
1. Adds `calculation_results_uri VARCHAR(500)` column to `riskcalculation.batches` table
2. Creates index `idx_batches_results_uri` for efficient URI lookups
3. Adds column comment documenting its purpose

**Requirements**: 2.1, 2.5

## Prerequisites

Before running this migration, ensure:

1. **PostgreSQL is running** and accessible
2. **Database connection** is properly configured in `regtech-app/src/main/resources/application.yml`
3. **Flyway is configured** and has successfully run previous migrations (V50-V52)
4. You have **database admin privileges** to alter tables and create indexes

## How to Apply

### Option 1: Using PowerShell Script (Recommended for Windows)

```powershell
.\apply-v53-migration.ps1
```

### Option 2: Using Batch Script (Windows)

```cmd
apply-v53-migration.bat
```

### Option 3: Using Maven Directly

```bash
mvn flyway:migrate -Dflyway.schemas=riskcalculation
```

### Option 4: Manual SQL Execution

If you prefer to run the migration manually:

```sql
-- Connect to your PostgreSQL database
\c your_database_name

-- Run the migration
ALTER TABLE riskcalculation.batches 
ADD COLUMN calculation_results_uri VARCHAR(500);

CREATE INDEX idx_batches_results_uri 
ON riskcalculation.batches(calculation_results_uri);

COMMENT ON COLUMN riskcalculation.batches.calculation_results_uri IS 
'S3 URI or filesystem path to the JSON file containing complete calculation results (exposures, mitigations, portfolio analysis). This serves as the single source of truth for detailed calculation data.';
```

## Verification

After applying the migration, verify it was successful:

### 1. Check Flyway Schema History

```sql
SELECT version, description, installed_on, success 
FROM flyway_schema_history 
WHERE version = '53';
```

Expected result:
```
version | description                              | installed_on        | success
--------|------------------------------------------|---------------------|--------
53      | Add calculation results uri to batches   | 2024-12-07 ...      | true
```

### 2. Verify Column Exists

```sql
SELECT column_name, data_type, character_maximum_length, is_nullable
FROM information_schema.columns
WHERE table_schema = 'riskcalculation' 
  AND table_name = 'batches' 
  AND column_name = 'calculation_results_uri';
```

Expected result:
```
column_name              | data_type        | character_maximum_length | is_nullable
-------------------------|------------------|--------------------------|------------
calculation_results_uri  | character varying| 500                      | YES
```

### 3. Verify Index Exists

```sql
SELECT indexname, indexdef
FROM pg_indexes
WHERE schemaname = 'riskcalculation' 
  AND tablename = 'batches' 
  AND indexname = 'idx_batches_results_uri';
```

Expected result:
```
indexname               | indexdef
------------------------|----------------------------------------------------------
idx_batches_results_uri | CREATE INDEX idx_batches_results_uri ON riskcalculation.batches USING btree (calculation_results_uri)
```

### 4. Test Query

```sql
-- This should run without errors (will return empty or existing batches)
SELECT batch_id, status, calculation_results_uri 
FROM riskcalculation.batches 
LIMIT 5;
```

## Impact Assessment

### Data Impact
- **No data loss**: This is an additive migration (ADD COLUMN)
- **Existing batches**: Will have `NULL` value for `calculation_results_uri`
- **New batches**: Will populate this field when calculation completes

### Performance Impact
- **Minimal**: Adding a column with NULL values is a metadata-only operation in PostgreSQL
- **Index creation**: Fast on empty or small tables; may take time on large tables
- **Query performance**: Index will improve URI lookup performance

### Application Impact
- **Backward compatible**: Existing code will continue to work
- **No immediate changes required**: Application can be updated incrementally
- **Next steps**: Implement BatchRepository enhancements (Task 2)

## Rollback

If you need to rollback this migration:

```sql
-- Remove the index
DROP INDEX IF EXISTS riskcalculation.idx_batches_results_uri;

-- Remove the column
ALTER TABLE riskcalculation.batches 
DROP COLUMN IF EXISTS calculation_results_uri;

-- Update Flyway history (if needed)
DELETE FROM flyway_schema_history WHERE version = '53';
```

**Warning**: Only rollback if absolutely necessary and no data has been written to the new column.

## Troubleshooting

### Error: "relation 'riskcalculation.batches' does not exist"

**Cause**: Previous migrations (V50) haven't been run.

**Solution**: Run all pending migrations:
```bash
mvn flyway:migrate
```

### Error: "column 'calculation_results_uri' already exists"

**Cause**: Migration has already been applied.

**Solution**: Check Flyway history:
```sql
SELECT * FROM flyway_schema_history WHERE version = '53';
```

If the migration shows as successful, no action needed. If it shows as failed, you may need to repair:
```bash
mvn flyway:repair
```

### Error: "permission denied for schema riskcalculation"

**Cause**: Database user lacks necessary privileges.

**Solution**: Grant privileges:
```sql
GRANT ALL PRIVILEGES ON SCHEMA riskcalculation TO your_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA riskcalculation TO your_user;
```

## Next Steps

After successfully applying this migration:

1. ✅ **Task 1 Complete**: Column and index created
2. ⏭️ **Task 2**: Enhance BatchRepository with URI management methods
   - Add `updateCalculationResultsUri()` method
   - Add `getCalculationResultsUri()` method
   - Add `markAsCompleted()` method
3. ⏭️ **Task 3**: Implement CalculationResultsJsonSerializer
4. Continue with remaining tasks in the implementation plan

## References

- **Spec**: `.kiro/specs/risk-calculation-storage-refactoring/`
- **Requirements**: 2.1 (batch metadata storage), 2.5 (batch completion with URI)
- **Design**: File-first storage architecture
- **Migration File**: `regtech-app/src/main/resources/db/migration/riskcalculation/V53__Add_calculation_results_uri_to_batches.sql`
