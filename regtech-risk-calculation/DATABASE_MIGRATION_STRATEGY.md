# Risk Calculation Storage Migration Strategy

## Overview

This document outlines the migration strategy for transitioning from database-first storage (exposures and mitigations in PostgreSQL) to file-first storage (JSON files as single source of truth). The migration is designed to be gradual, safe, and reversible.

## Migration Phases

### Phase 1: Dual-Write Implementation (COMPLETED)

**Status**: ✅ Complete

**Changes**:
- Added `calculation_results_uri` column to `batches` table (Migration V53)
- Implemented JSON serialization for complete calculation results
- Implemented file storage service integration
- Modified `CalculateRiskMetricsCommandHandler` to store results in both:
  - JSON files (via `ICalculationResultsStorageService`)
  - Database tables (exposures, mitigations) - for backward compatibility

**Validation**:
- New batches have both database records AND JSON files
- `calculation_results_uri` is populated for all new batches
- Existing query endpoints continue to work

### Phase 2: Read Migration (CURRENT)

**Status**: 🔄 In Progress

**Changes**:
- Query services updated to prefer JSON files over database
- `ExposureQueryService.findByBatchId()` retrieves from JSON files
- Database queries remain as fallback for legacy batches
- Repository methods marked as `@Deprecated`

**Validation**:
- Queries for new batches use JSON files
- Queries for old batches fall back to database
- Performance metrics show file-based queries are working

### Phase 3: Write Cutover (NEXT)

**Status**: ⏳ Pending

**Changes**:
- Stop writing to exposures and mitigations tables
- Remove database persistence calls from command handlers
- JSON files become the only write target
- Database tables become read-only for legacy data

**Timeline**: After Phase 2 validation (2-4 weeks)

**Validation**:
- New batches have ONLY JSON files (no database records)
- Old batches remain queryable via database
- No new records in exposures/mitigations tables

### Phase 4: Legacy Data Migration (OPTIONAL)

**Status**: ⏳ Pending

**Purpose**: Migrate historical batches from database to JSON files

**Approach**:
```
For each batch in database without calculation_results_uri:
  1. Query exposures and mitigations from database
  2. Reconstruct RiskCalculationResult object
  3. Serialize to JSON and store via file storage service
  4. Update batch record with calculation_results_uri
  5. Verify JSON file is retrievable
```

**Timeline**: After Phase 3 stabilization (1-3 months)

**Considerations**:
- Run during low-traffic periods
- Process in batches to avoid overwhelming storage
- Maintain database records as backup during migration
- Validate each migrated batch before proceeding

### Phase 5: Table Deprecation (FINAL)

**Status**: ⏳ Pending

**Changes**:
- Mark exposures and mitigations tables as deprecated in schema
- Add database comments indicating tables are legacy
- Update documentation to reflect file-first architecture
- Remove foreign key constraints (if any)

**Timeline**: After Phase 4 completion or 6 months after Phase 3

**Validation**:
- All active batches use JSON files
- Database tables contain only historical data
- No application code writes to deprecated tables

### Phase 6: Table Removal (OPTIONAL)

**Status**: ⏳ Pending

**Prerequisites**:
- All batches migrated to JSON files OR
- Retention policy allows deletion of old batches OR
- Business decision to archive/export old data

**Changes**:
- Create final backup of exposures and mitigations tables
- Drop foreign key constraints
- Drop indexes
- Drop tables: `exposures`, `mitigations`

**Timeline**: 12+ months after Phase 3, pending business approval

## Handling Mixed State

### Current State Detection

The system supports mixed state where some batches have database records and others have JSON files:

```java
// In ExposureQueryService
public List<ExposureRecording> findByBatchId(String batchId) {
    // Check if batch has JSON file
    Optional<String> uri = batchRepository.getCalculationResultsUri(batchId);
    
    if (uri.isPresent()) {
        // New approach: retrieve from JSON file
        return storageService.retrieveCalculationResults(batchId)
            .map(RiskCalculationResult::getExposures)
            .orElseThrow(() -> new FileNotFoundException(batchId));
    } else {
        // Legacy approach: query database
        return exposureRepository.findByBatchId(batchId);
    }
}
```

### Query Strategy

**Status Queries** (Fast, metadata-only):
- Always use database: `batches` table
- No file I/O required
- Works for both old and new batches

**Summary Queries** (Dashboard metrics):
- Use `portfolio_analysis` table when available
- Falls back to JSON file parsing if needed
- Cached for performance

**Detail Queries** (Full exposure data):
- Check `calculation_results_uri` in batch record
- If present: download and parse JSON file
- If absent: query exposures/mitigations tables
- Transparent to API consumers

### Data Consistency

**Dual-Write Period** (Phase 1-2):
- Both database and JSON files are written
- JSON file is source of truth
- Database serves as backup and fallback

**Write Cutover** (Phase 3+):
- Only JSON files are written
- Database contains legacy data only
- Clear separation between old and new batches

## Table Deprecation Timeline

### Exposures Table

| Phase | Status | Timeline | Actions |
|-------|--------|----------|---------|
| Phase 1-2 | Active | Current | Dual-write: both DB and files |
| Phase 3 | Read-Only | +2-4 weeks | Stop writing, read legacy only |
| Phase 4 | Legacy | +1-3 months | Optional migration of old data |
| Phase 5 | Deprecated | +6 months | Mark as deprecated in schema |
| Phase 6 | Removed | +12 months | Drop table (pending approval) |

### Mitigations Table

| Phase | Status | Timeline | Actions |
|-------|--------|----------|---------|
| Phase 1-2 | Active | Current | Dual-write: both DB and files |
| Phase 3 | Read-Only | +2-4 weeks | Stop writing, read legacy only |
| Phase 4 | Legacy | +1-3 months | Optional migration of old data |
| Phase 5 | Deprecated | +6 months | Mark as deprecated in schema |
| Phase 6 | Removed | +12 months | Drop table (pending approval) |

### Batches Table

**Status**: Retained permanently

**Changes**:
- Added `calculation_results_uri` column (V53)
- Remains as metadata store
- Essential for operational queries

### Portfolio Analysis Table

**Status**: Retained permanently

**Purpose**: Optional summary metrics for fast dashboard queries

**No changes required**

## Dropping Tables: Detailed Steps

### Prerequisites Checklist

Before dropping exposures and mitigations tables, verify:

- [ ] All active batches have `calculation_results_uri` populated
- [ ] JSON files are accessible and validated
- [ ] No application code references deprecated tables
- [ ] Business stakeholders approve data archival
- [ ] Backup of tables created and verified
- [ ] Retention policies allow deletion of old data
- [ ] Monitoring confirms no queries to deprecated tables

### Step 1: Create Final Backup

```sql
-- Create backup schema
CREATE SCHEMA IF NOT EXISTS risk_calculation_archive;

-- Backup exposures table
CREATE TABLE risk_calculation_archive.exposures_backup AS 
SELECT * FROM exposures;

-- Backup mitigations table
CREATE TABLE risk_calculation_archive.mitigations_backup AS 
SELECT * FROM mitigations;

-- Verify backup
SELECT COUNT(*) FROM risk_calculation_archive.exposures_backup;
SELECT COUNT(*) FROM risk_calculation_archive.mitigations_backup;
```

### Step 2: Export to Long-Term Storage (Optional)

```bash
# Export to CSV for archival
pg_dump -h localhost -U postgres -d regtech \
  -t exposures -t mitigations \
  --format=custom \
  --file=risk_calculation_legacy_$(date +%Y%m%d).dump

# Upload to S3 for long-term retention
aws s3 cp risk_calculation_legacy_*.dump \
  s3://regtech-archives/database-backups/
```

### Step 3: Drop Foreign Key Constraints

```sql
-- List all foreign keys referencing exposures/mitigations
SELECT 
    tc.constraint_name,
    tc.table_name,
    kcu.column_name,
    ccu.table_name AS foreign_table_name
FROM information_schema.table_constraints AS tc
JOIN information_schema.key_column_usage AS kcu
  ON tc.constraint_name = kcu.constraint_name
JOIN information_schema.constraint_column_usage AS ccu
  ON ccu.constraint_name = tc.constraint_name
WHERE tc.constraint_type = 'FOREIGN KEY'
  AND (ccu.table_name = 'exposures' OR ccu.table_name = 'mitigations');

-- Drop foreign keys (if any exist)
-- ALTER TABLE <referencing_table> DROP CONSTRAINT <constraint_name>;
```

### Step 4: Drop Indexes

```sql
-- Drop indexes on exposures table
DROP INDEX IF EXISTS idx_exposures_batch_id;
DROP INDEX IF EXISTS idx_exposures_counterparty_lei;
DROP INDEX IF EXISTS idx_exposures_geographic_area;
DROP INDEX IF EXISTS idx_exposures_sector;

-- Drop indexes on mitigations table
DROP INDEX IF EXISTS idx_mitigations_batch_id;
DROP INDEX IF EXISTS idx_mitigations_exposure_id;
DROP INDEX IF EXISTS idx_mitigations_type;
```

### Step 5: Drop Tables

```sql
-- Drop mitigations table first (if it has FK to exposures)
DROP TABLE IF EXISTS mitigations CASCADE;

-- Drop exposures table
DROP TABLE IF EXISTS exposures CASCADE;

-- Verify tables are dropped
SELECT table_name 
FROM information_schema.tables 
WHERE table_schema = 'public' 
  AND table_name IN ('exposures', 'mitigations');
```

### Step 6: Create Migration Record

```sql
-- Create migration to document the table removal
-- File: V54__Drop_exposures_and_mitigations_tables.sql

-- This migration is applied after all batches have been migrated to JSON files
-- Backup created: risk_calculation_archive.exposures_backup
-- Backup created: risk_calculation_archive.mitigations_backup
-- Date: [YYYY-MM-DD]
-- Approved by: [Stakeholder Name]

DROP TABLE IF EXISTS mitigations CASCADE;
DROP TABLE IF EXISTS exposures CASCADE;

-- Add comment to batches table documenting the change
COMMENT ON COLUMN batches.calculation_results_uri IS 
  'URI to JSON file containing complete calculation results. ' ||
  'Replaces exposures and mitigations tables (dropped in V54).';
```

### Step 7: Update Application Configuration

```yaml
# application-risk-calculation.yml

risk-calculation:
  storage:
    # Enforce file-only storage after table removal
    legacy-database-fallback: false
    
  migration:
    # Document migration completion
    exposures-table-dropped: true
    mitigations-table-dropped: true
    migration-completed-date: "2025-XX-XX"
```

### Step 8: Post-Removal Validation

```java
// Add startup check to verify tables are dropped
@Component
public class StorageMigrationValidator {
    
    @PostConstruct
    public void validateMigrationComplete() {
        // Verify exposures table doesn't exist
        boolean exposuresExists = checkTableExists("exposures");
        boolean mitigationsExists = checkTableExists("mitigations");
        
        if (exposuresExists || mitigationsExists) {
            log.warn("Legacy tables still exist after migration");
        } else {
            log.info("Storage migration complete: legacy tables removed");
        }
    }
}
```

## Rollback Strategy

### Phase 3 Rollback (Stop Writing to Database)

If issues arise after stopping database writes:

1. **Immediate**: Re-enable database persistence in command handler
2. **Verify**: New batches write to both database and files
3. **Investigate**: Determine root cause of issues
4. **Fix**: Address issues before attempting cutover again

### Phase 4 Rollback (Legacy Data Migration)

If migrated data has issues:

1. **Stop**: Halt migration process immediately
2. **Verify**: Database records still exist as backup
3. **Delete**: Remove problematic JSON files
4. **Investigate**: Fix serialization or storage issues
5. **Retry**: Re-run migration for affected batches

### Phase 6 Rollback (Table Removal)

If tables are dropped prematurely:

1. **Restore**: Use backup schema to recreate tables
   ```sql
   CREATE TABLE exposures AS 
   SELECT * FROM risk_calculation_archive.exposures_backup;
   
   CREATE TABLE mitigations AS 
   SELECT * FROM risk_calculation_archive.mitigations_backup;
   ```

2. **Rebuild Indexes**: Recreate all indexes for performance

3. **Update Code**: Re-enable database fallback in query services

4. **Verify**: Test queries for old batches work correctly

## Monitoring and Validation

### Key Metrics to Track

**Storage Distribution**:
- Batches with JSON files only: `COUNT(*) WHERE calculation_results_uri IS NOT NULL AND NOT EXISTS (SELECT 1 FROM exposures WHERE batch_id = batches.batch_id)`
- Batches with database only: `COUNT(*) WHERE calculation_results_uri IS NULL`
- Batches with both: `COUNT(*) WHERE calculation_results_uri IS NOT NULL AND EXISTS (SELECT 1 FROM exposures WHERE batch_id = batches.batch_id)`

**Query Performance**:
- Average response time for file-based queries
- Average response time for database queries
- Cache hit rate for JSON file retrieval

**Storage Usage**:
- Total size of exposures table: `pg_total_relation_size('exposures')`
- Total size of mitigations table: `pg_total_relation_size('mitigations')`
- Total size of JSON files in S3/filesystem

**Error Rates**:
- File not found errors
- Deserialization errors
- Database query failures

### Validation Queries

```sql
-- Check migration progress
SELECT 
    COUNT(*) FILTER (WHERE calculation_results_uri IS NOT NULL) as with_json,
    COUNT(*) FILTER (WHERE calculation_results_uri IS NULL) as without_json,
    COUNT(*) as total
FROM batches;

-- Find batches needing migration
SELECT batch_id, report_date, status
FROM batches
WHERE calculation_results_uri IS NULL
  AND status = 'COMPLETED'
ORDER BY report_date DESC;

-- Verify no new database writes (after Phase 3)
SELECT batch_id, created_at
FROM exposures
WHERE created_at > '2025-XX-XX'  -- Date of Phase 3 cutover
ORDER BY created_at DESC;

-- Check storage usage
SELECT 
    pg_size_pretty(pg_total_relation_size('exposures')) as exposures_size,
    pg_size_pretty(pg_total_relation_size('mitigations')) as mitigations_size,
    pg_size_pretty(pg_total_relation_size('batches')) as batches_size;
```

## Risk Mitigation

### Data Loss Prevention

1. **Dual-Write Period**: Maintain both storage methods during transition
2. **Backup Before Drop**: Create comprehensive backups before table removal
3. **Gradual Rollout**: Migrate in phases with validation at each step
4. **Rollback Plan**: Document and test rollback procedures

### Performance Degradation

1. **Caching**: Implement caching for frequently accessed JSON files
2. **Monitoring**: Track query performance metrics continuously
3. **Fallback**: Maintain database queries as fallback during transition
4. **Load Testing**: Validate performance under production load

### Operational Issues

1. **File Storage Failures**: Implement retry logic and error handling
2. **Deserialization Errors**: Validate JSON format and handle version mismatches
3. **Storage Quota**: Monitor S3/filesystem usage and set up alerts
4. **Access Permissions**: Verify file storage permissions are correct

## Communication Plan

### Stakeholder Updates

**Weekly During Migration**:
- Progress report on migration phases
- Metrics on storage distribution
- Any issues or blockers encountered

**Before Each Phase**:
- Detailed plan for upcoming phase
- Expected timeline and validation criteria
- Rollback procedures if needed

**After Table Removal**:
- Final migration report
- Storage savings achieved
- Lessons learned and recommendations

### Documentation Updates

- [ ] Update API documentation to reflect file-first architecture
- [ ] Update operational runbooks for new query patterns
- [ ] Update disaster recovery procedures
- [ ] Update data retention policies
- [ ] Update architecture diagrams

## Success Criteria

### Phase 3 Success (Write Cutover)

- ✅ No new records in exposures/mitigations tables for 2+ weeks
- ✅ All new batches have `calculation_results_uri` populated
- ✅ Query performance meets SLA requirements
- ✅ Error rates remain below threshold
- ✅ Stakeholder approval to proceed

### Phase 6 Success (Table Removal)

- ✅ All active batches use JSON files
- ✅ Backup verified and stored securely
- ✅ Tables dropped successfully
- ✅ Application functions normally without tables
- ✅ Storage costs reduced as expected
- ✅ Documentation updated completely

## Appendix: Migration Scripts

### Script 1: Check Migration Readiness

```sql
-- Check if system is ready for Phase 3 (write cutover)
DO $$
DECLARE
    total_batches INTEGER;
    batches_with_json INTEGER;
    recent_batches_without_json INTEGER;
BEGIN
    SELECT COUNT(*) INTO total_batches FROM batches;
    SELECT COUNT(*) INTO batches_with_json 
    FROM batches WHERE calculation_results_uri IS NOT NULL;
    
    SELECT COUNT(*) INTO recent_batches_without_json
    FROM batches 
    WHERE calculation_results_uri IS NULL 
      AND processed_at > NOW() - INTERVAL '7 days';
    
    RAISE NOTICE 'Total batches: %', total_batches;
    RAISE NOTICE 'Batches with JSON: % (%.1f%%)', 
        batches_with_json, 
        (batches_with_json::FLOAT / total_batches * 100);
    RAISE NOTICE 'Recent batches without JSON: %', recent_batches_without_json;
    
    IF recent_batches_without_json = 0 THEN
        RAISE NOTICE '✅ READY for Phase 3: All recent batches have JSON files';
    ELSE
        RAISE NOTICE '❌ NOT READY: Recent batches still missing JSON files';
    END IF;
END $$;
```

### Script 2: Migrate Single Batch

```java
// Utility to migrate a single batch from database to JSON
public class BatchMigrationService {
    
    public Result<String> migrateBatch(String batchId) {
        try {
            // 1. Check if already migrated
            Optional<String> existingUri = batchRepository.getCalculationResultsUri(batchId);
            if (existingUri.isPresent()) {
                return Result.success(existingUri.get());
            }
            
            // 2. Retrieve from database
            List<ExposureRecording> exposures = exposureRepository.findByBatchId(batchId);
            List<RawMitigationData> mitigations = mitigationRepository.findByBatchId(batchId);
            
            if (exposures.isEmpty()) {
                return Result.failure("No exposures found for batch: " + batchId);
            }
            
            // 3. Reconstruct calculation result
            RiskCalculationResult result = reconstructResult(batchId, exposures, mitigations);
            
            // 4. Store as JSON
            Result<String> storageResult = storageService.storeCalculationResults(result);
            if (storageResult.isFailure()) {
                return storageResult;
            }
            
            // 5. Update batch metadata
            String uri = storageResult.getValue();
            batchRepository.updateCalculationResultsUri(batchId, uri);
            
            // 6. Verify retrieval works
            Result<RiskCalculationResult> verifyResult = 
                storageService.retrieveCalculationResults(batchId);
            
            if (verifyResult.isFailure()) {
                log.error("Migration verification failed for batch: {}", batchId);
                return Result.failure("Verification failed");
            }
            
            log.info("Successfully migrated batch {} to JSON: {}", batchId, uri);
            return Result.success(uri);
            
        } catch (Exception e) {
            log.error("Failed to migrate batch: " + batchId, e);
            return Result.failure("Migration failed: " + e.getMessage());
        }
    }
}
```

## References

- Requirements Document: `.kiro/specs/risk-calculation-storage-refactoring/requirements.md`
- Design Document: `.kiro/specs/risk-calculation-storage-refactoring/design.md`
- Migration V53: `regtech-app/src/main/resources/db/migration/riskcalculation/V53__Add_calculation_results_uri_to_batches.sql`
- Storage Service: `regtech-risk-calculation/infrastructure/src/main/java/com/bcbs239/regtech/riskcalculation/infrastructure/filestorage/CalculationResultsStorageServiceImpl.java`
