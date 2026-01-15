# APPLY V65 MIGRATION NOW - Validation Rule Categories

## ⚠️ Version Conflict Resolved

**Issue:** Original V62 migration conflicted with IAM module's V62 migration  
**Solution:** Renamed to **V65** to avoid conflicts

## Quick Start

### Step 1: Apply Migration

**Option A: PowerShell (Recommended)**
```powershell
.\apply-v65-migration.ps1
```

**Option B: Batch Script**
```cmd
apply-v65-migration.bat
```

**Option C: Manual**
```bash
cd regtech-app
..\mvnw flyway:migrate -Dflyway.outOfOrder=true
```

### Step 2: Verify Migration

```sql
docker exec regtech-postgres-1 psql -U myuser -d regtech -c "
SELECT validation_category, COUNT(*) as rule_count 
FROM dataquality.business_rules 
GROUP BY validation_category 
ORDER BY rule_count DESC;
"
```

Expected output:
```
 validation_category | rule_count
---------------------+------------
 DATA_QUALITY        |         12
 CODE_VALIDATION     |          5
 TEMPORAL_COHERENCE  |          4
 NUMERIC_RANGES      |          3
```

### Step 3: Check Column

```sql
docker exec regtech-postgres-1 psql -U myuser -d regtech -c "\d dataquality.business_rules"
```

Should show:
```
 validation_category | character varying(50) |
```

### Step 4: Restart Application

```powershell
cd regtech-app
..\mvnw spring-boot:run
```

### Step 5: Test API

```bash
curl -X GET "http://localhost:8080/api/v1/data-quality/config?bankId=default-bank" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

Expected response includes category fields:
```json
{
  "validation": {
    "activeRules": [
      {
        "code": "COMPLETENESS_EXPOSURE_AMOUNT",
        "category": "DATA_QUALITY",
        "categoryDisplayName": "Controlli Attivi",
        "categoryIcon": "check-circle"
      }
    ]
  }
}
```

## What This Migration Does

1. ✅ Adds `validation_category VARCHAR(50)` column to `business_rules` table
2. ✅ Categorizes all existing rules based on rule_code patterns
3. ✅ Creates index `idx_business_rules_validation_category`
4. ✅ Updates ~24 existing rules with appropriate categories

## Categories Assigned

| Category | Rule Code Pattern | Count |
|----------|------------------|-------|
| DATA_QUALITY | COMPLETENESS_* | ~12 |
| CODE_VALIDATION | *VALID_CURRENCY*, *VALID_LEI*, *RATING_FORMAT* | ~5 |
| TEMPORAL_COHERENCE | TIMELINESS_*, *MATURITY*, *DATE* | ~4 |
| NUMERIC_RANGES | *POSITIVE_AMOUNT*, *REASONABLE_AMOUNT* | ~3 |
| DUPLICATE_DETECTION | *DUPLICATE*, *UNIQUE* | 0 |
| CROSS_REFERENCE | *CROSS*, *REFERENCE* | 0 |

## Files Changed

- ✅ `V65__add_validation_category_to_business_rules.sql` - Migration script
- ✅ `ValidationCategory.java` - Domain enum with 6 categories
- ✅ `ConfigurationDto.java` - Updated ValidationRuleDto with category fields
- ✅ `GetConfigurationQueryHandler.java` - Auto-maps categories in response
- ✅ `BusinessRuleEntity.java` - Added validationCategory field
- ✅ `apply-v65-migration.ps1` - PowerShell automation script
- ✅ `apply-v65-migration.bat` - Batch automation script

## Troubleshooting

### Problem: Migration already applied as V62

```sql
-- Check current migration status
docker exec regtech-postgres-1 psql -U myuser -d regtech -c "
SELECT version, description FROM flyway_schema_history 
WHERE version IN ('62', '65') 
ORDER BY version;
"

-- If V62 was already applied, remove it
docker exec regtech-postgres-1 psql -U myuser -d regtech -c "
DELETE FROM flyway_schema_history WHERE version = '62' 
AND description LIKE '%validation_category%';
"
```

### Problem: Column already exists

```sql
-- Check if column exists
docker exec regtech-postgres-1 psql -U myuser -d regtech -c "
SELECT column_name FROM information_schema.columns 
WHERE table_schema='dataquality' AND table_name='business_rules' 
AND column_name='validation_category';
"

-- If exists, just mark migration as applied
docker exec regtech-postgres-1 psql -U myuser -d regtech -c "
INSERT INTO flyway_schema_history (version, description, type, script, installed_rank, execution_time, success)
VALUES ('65', 'add validation category to business rules', 'SQL', 
        'V65__add_validation_category_to_business_rules.sql', 999, 0, true);
"
```

## Complete Documentation

See `VALIDATION_RULE_CATEGORIES_IMPLEMENTATION.md` for:
- Detailed implementation guide
- Frontend integration examples
- Category icon mapping
- Testing checklist
- Full API examples

## Status

✅ **Ready to Apply**
- Migration script: V65 (renamed from V62 to avoid IAM conflict)
- All files updated with correct version
- Scripts tested and ready
- Documentation complete

**Run the migration now:** `.\apply-v65-migration.ps1`
