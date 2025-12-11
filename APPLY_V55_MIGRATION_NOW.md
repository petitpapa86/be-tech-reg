# Apply V55 Migration - Add version Column for Optimistic Locking

## Overview

This migration adds the `version` column to the `riskcalculation.portfolio_analysis` table to enable optimistic locking for the PortfolioAnalysisEntity. This prevents concurrent modification conflicts when multiple processes attempt to update portfolio analysis results simultaneously.

## Migration Details

**Migration File**: `regtech-app/src/main/resources/db/migration/riskcalculation/V55__Add_version_to_portfolio_analysis.sql`

**Changes**:
1. Adds `version BIGINT` column to `riskcalculation.portfolio_analysis` table
2. Sets default value to 0 for existing rows
3. Adds column comment documenting its purpose for optimistic locking

**Requirements**: JPA Optimistic Locking Implementation

## Prerequisites

Before running this migration, ensure:

1. **PostgreSQL is running** and accessible
2. **Database connection** is properly configured in `regtech-app/src/main/resources/application.yml`
3. **Flyway is configured** and has successfully run previous migrations (V50-V54)
4. You have **database admin privileges** to alter tables
5. **PortfolioAnalysisEntity has been updated** with `@Version` annotation (already completed)

## How to Apply

### Option 1: Using PowerShell Script (Recommended for Windows)

```powershell
.\apply-v55-migration.ps1
```

### Option 2: Using Batch Script (Windows)

```cmd
apply-v55-migration.bat
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
ALTER TABLE riskcalculation.portfolio_analysis
ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

COMMENT ON COLUMN riskcalculation.portfolio_analysis.version IS 'Version field for optimistic locking';
```

## Verification

After applying the migration, verify it was successful:

### 1. Check Flyway Schema History

```sql
SELECT version, description, installed_on, success
FROM flyway_schema_history
WHERE version = '55';
```

Expected result:
```
version | description                      | installed_on        | success
--------|----------------------------------|---------------------|--------
55      | Add version to portfolio analysis| 2024-12-11 ...      | true
```

### 2. Verify Column Exists

```sql
SELECT column_name, data_type, is_nullable, column_default
FROM information_schema.columns
WHERE table_schema = 'riskcalculation'
  AND table_name = 'portfolio_analysis'
  AND column_name = 'version';
```

Expected result:
```
column_name | data_type | is_nullable | column_default
------------|-----------|-------------|---------------
version     | bigint    | NO          | 0
```

### 3. Verify Entity Configuration

Ensure the PortfolioAnalysisEntity.java has been updated with:

```java
@Version
@Column(name = "version", nullable = false)
private Long version;
```

And in the `@PrePersist` method:
```java
version = 0L;
```

## Impact

- **Performance**: Minimal impact, version field is lightweight
- **Concurrency**: Prevents lost updates in concurrent portfolio analysis processing scenarios
- **Compatibility**: Fully backward compatible, existing code continues to work
- **Storage**: Adds 8 bytes per row to the portfolio_analysis table</content>
<parameter name="filePath">C:\Users\alseny\Desktop\react projects\regtech\APPLY_V55_MIGRATION_NOW.md