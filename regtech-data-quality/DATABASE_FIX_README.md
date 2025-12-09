# Database Schema Fix: Batch ID and Bank ID Length Issue

## Problem
The application was throwing a `DataException` error:
```
org.hibernate.exception.DataException: could not execute statement 
[ERRORE: il valore è troppo lungo per il tipo character varying(36)]
```

This error occurred because the `batch_id` and `bank_id` columns in the database were defined as `VARCHAR(36)`, but the actual batch IDs being used are much longer (80+ characters).

### Example Batch IDs
Actual batch IDs look like:
- `batch_20251115_233742_batch_20251115_233722_74546acb-a0df-44c1-a09b-a1fd884a8af4`

These are approximately 80-90 characters long, far exceeding the 36-character limit.

## Solution

### 1. Updated JPA Entities
Changed column lengths from `VARCHAR(36)` to `VARCHAR(255)` in:
- `QualityReportEntity.java`
  - `batch_id` column
  - `bank_id` column
- `QualityErrorSummaryEntity.java`
  - `batch_id` column
  - `bank_id` column

### 2. Updated Schema Definition
**Note**: The `schema.sql` file has been deprecated and replaced by Flyway migrations. Schema changes are now managed through versioned migration scripts in `db/migration/`.

### 3. Database Migration Required

**IMPORTANT**: You must run the database migration script to update the existing database schema.

#### Option A: Using Docker Compose
If your database is running in Docker Compose:

```powershell
# Connect to the PostgreSQL container
docker compose exec postgres psql -U myuser -d mydatabase

# Then run:
\i /path/to/manual_migration.sql
```

#### Option B: Using psql directly
```powershell
psql -U myuser -d mydatabase -f "c:\Users\alseny\Desktop\react projects\regtech\regtech-data-quality\infrastructure\src\main\resources\db\migration\manual_migration.sql"
```

#### Option C: Manual SQL Execution
Connect to your PostgreSQL database and run:

```sql
-- Alter quality_reports table
ALTER TABLE dataquality.quality_reports 
    ALTER COLUMN batch_id TYPE VARCHAR(255),
    ALTER COLUMN bank_id TYPE VARCHAR(255);

-- Alter quality_error_summaries table  
ALTER TABLE dataquality.quality_error_summaries
    ALTER COLUMN batch_id TYPE VARCHAR(255),
    ALTER COLUMN bank_id TYPE VARCHAR(255);
```

### 4. Verify the Migration

After running the migration, verify the changes:

```sql
SELECT 
    table_name, 
    column_name, 
    data_type, 
    character_maximum_length 
FROM information_schema.columns 
WHERE table_schema = 'dataquality' 
    AND table_name IN ('quality_reports', 'quality_error_summaries')
    AND column_name IN ('batch_id', 'bank_id')
ORDER BY table_name, column_name;
```

Expected output:
```
      table_name        | column_name | data_type | character_maximum_length 
------------------------+-------------+-----------+-------------------------
 quality_error_summaries| bank_id     | varchar   | 255
 quality_error_summaries| batch_id    | varchar   | 255
 quality_reports        | bank_id     | varchar   | 255
 quality_reports        | batch_id    | varchar   | 255
```

## Files Changed

### Java Entities
1. `regtech-data-quality/infrastructure/src/main/java/com/bcbs239/regtech/dataquality/infrastructure/reporting/QualityReportEntity.java`
2. `regtech-data-quality/infrastructure/src/main/java/com/bcbs239/regtech/dataquality/infrastructure/reporting/QualityErrorSummaryEntity.java`

### Schema Files
3. `regtech-data-quality/infrastructure/src/main/resources/schema.sql` (DEPRECATED - replaced by Flyway migrations)

### Migration Scripts (New)
4. `regtech-data-quality/infrastructure/src/main/resources/db/migration/V202511180001__Increase_batch_and_bank_id_length.sql`
5. `regtech-data-quality/infrastructure/src/main/resources/db/migration/manual_migration.sql`

## Next Steps

1. **Run the database migration** (see Option A, B, or C above)
2. **Rebuild the application**:
   ```powershell
   cd "c:\Users\alseny\Desktop\react projects\regtech"
   mvn clean package -DskipTests
   ```
3. **Restart the application**
4. **Test the batch ingestion** to verify the fix works

## Root Cause Analysis

The issue was caused by a mismatch between:
- **Expected format**: UUID format (36 characters)
- **Actual format**: Timestamp-based batch IDs with embedded UUIDs (80+ characters)

The batch ID generation in the ingestion module creates composite IDs that include:
- Timestamp prefix: `batch_YYYYMMDD_HHMMSS_`
- Additional timestamp: `batch_YYYYMMDD_HHMMSS_`
- UUID suffix: `xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx`

This results in batch IDs much longer than the standard UUID format.

## Prevention

To prevent similar issues in the future:
1. Consider standardizing on a single ID format (pure UUID or shortened composite)
2. If using composite IDs, document the maximum expected length
3. Use VARCHAR(255) or larger for ID fields that may contain composite identifiers
4. Add validation in the domain layer to enforce ID length constraints
