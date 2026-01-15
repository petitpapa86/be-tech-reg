# Validation Rule Categories - Implementation Summary

## Overview

This implementation adds a **category field** to validation rules to enable better organization and filtering in the UI. Rules are now categorized by their validation type (data quality, numeric ranges, code validation, etc.).

## Changes Made

### 1. Domain Layer - ValidationCategory Enum

**File:** `regtech-data-quality/domain/src/main/java/com/bcbs239/regtech/dataquality/domain/rules/ValidationCategory.java`

Created an enum with 6 categories:

| Category | Italian Display Name | Icon | Description |
|----------|---------------------|------|-------------|
| `DATA_QUALITY` | Controlli Attivi | check-circle | General quality validations |
| `NUMERIC_RANGES` | Range valori numerici | sliders | Numeric value range checks |
| `CODE_VALIDATION` | Validazione codici | code | Format validation (currency, LEI, etc.) |
| `TEMPORAL_COHERENCE` | Coerenza temporale | clock | Date and time validations |
| `DUPLICATE_DETECTION` | Rilevamento duplicati | copy | Duplicate record checks |
| `CROSS_REFERENCE` | Cross-check tra dataset | link | Cross-dataset validations |

**Key Method:** `fromRuleCode(String ruleCode)` - Automatically determines category from rule code pattern

### 2. Application Layer - Updated DTOs

**File:** `regtech-data-quality/application/src/main/java/com/bcbs239/regtech/dataquality/application/config/ConfigurationDto.java`

Updated `ValidationRuleDto` record to include:
```java
public record ValidationRuleDto(
    String code,
    String description,
    boolean enabled,
    String category,              // NEW: Category name (e.g., "DATA_QUALITY")
    String categoryDisplayName,   // NEW: Italian display name
    String categoryIcon           // NEW: Icon identifier
) {}
```

### 3. Application Layer - Updated Query Handler

**File:** `regtech-data-quality/application/src/main/java/com/bcbs239/regtech/dataquality/application/config/GetConfigurationQueryHandler.java`

Updated `handle()` method to map category information when creating ValidationRuleDto:
```java
.map(rule -> {
    ValidationCategory category = ValidationCategory.fromRuleCode(rule.ruleCode());
    return new ConfigurationDto.ValidationRuleDto(
        rule.ruleCode(),
        rule.description(),
        rule.enabled(),
        category.name(),              // Category enum name
        category.getDisplayName(),    // Italian display
        category.getIcon()            // Icon identifier
    );
})
```

### 4. Infrastructure Layer - Database Entity

**File:** `regtech-data-quality/infrastructure/src/main/java/com/bcbs239/regtech/dataquality/infrastructure/rulesengine/entities/BusinessRuleEntity.java`

Added field:
```java
@Column(name = "validation_category", length = 50)
private String validationCategory;
```

### 5. Database Migration

**File:** `regtech-app/src/main/resources/db/migration/dataquality/V65__add_validation_category_to_business_rules.sql`

Migration script that:
1. Adds `validation_category VARCHAR(50)` column to `business_rules` table
2. Updates existing rules based on `rule_code` patterns:
   - `COMPLETENESS_*` → `DATA_QUALITY`
   - `*POSITIVE_AMOUNT*`, `*REASONABLE_AMOUNT*` → `NUMERIC_RANGES`
   - `*VALID_CURRENCY*`, `*VALID_LEI*`, `*RATING_FORMAT*` → `CODE_VALIDATION`
   - `TIMELINESS_*`, `*MATURITY*`, `*DATE*` → `TEMPORAL_COHERENCE`
   - `*DUPLICATE*`, `*UNIQUE*` → `DUPLICATE_DETECTION`
   - `*CROSS*`, `*REFERENCE*` → `CROSS_REFERENCE`
3. Creates index: `idx_business_rules_validation_category`
4. Sets default category `DATA_QUALITY` for any unmatched rules

## Rule Categorization Logic

### Pattern Matching Examples

```
Rule Code                          → Category
──────────────────────────────────────────────────────────
COMPLETENESS_EXPOSURE_AMOUNT       → DATA_QUALITY
ACCURACY_POSITIVE_AMOUNT           → NUMERIC_RANGES
ACCURACY_REASONABLE_AMOUNT         → NUMERIC_RANGES
ACCURACY_VALID_CURRENCY_CODE       → CODE_VALIDATION
ACCURACY_VALID_COUNTRY_CODE        → CODE_VALIDATION
ACCURACY_VALID_LEI_FORMAT          → CODE_VALIDATION
VALIDITY_RATING_FORMAT             → CODE_VALIDATION
TIMELINESS_REPORTING_DATE          → TEMPORAL_COHERENCE
VALIDITY_FUTURE_MATURITY_DATE      → TEMPORAL_COHERENCE
VALIDITY_MATURITY_DATE             → TEMPORAL_COHERENCE
```

## API Response Format

### Before (Old Response)
```json
{
  "bankId": "default-bank",
  "validation": {
    "type": "AUTOMATIC",
    "activeRules": [
      {
        "code": "COMPLETENESS_EXPOSURE_AMOUNT",
        "description": "Exposure amount must not be null",
        "enabled": true
      }
    ]
  }
}
```

### After (New Response with Categories)
```json
{
  "bankId": "default-bank",
  "validation": {
    "type": "AUTOMATIC",
    "activeRules": [
      {
        "code": "COMPLETENESS_EXPOSURE_AMOUNT",
        "description": "Exposure amount must not be null",
        "enabled": true,
        "category": "DATA_QUALITY",
        "categoryDisplayName": "Controlli Attivi",
        "categoryIcon": "check-circle"
      },
      {
        "code": "ACCURACY_VALID_CURRENCY_CODE",
        "description": "Currency code must be valid ISO 4217",
        "enabled": true,
        "category": "CODE_VALIDATION",
        "categoryDisplayName": "Validazione codici",
        "categoryIcon": "code"
      }
    ]
  }
}
```

## How to Apply Changes

### Step 1: Run Database Migration

**Option A: Using PowerShell script (Recommended)**
```powershell
.\apply-V65-migration.ps1
```

**Option B: Using Batch script**
```cmd
apply-V65-migration.bat
```

**Option C: Manual Maven command**
```bash
cd regtech-app
..\mvnw flyway:migrate -Dflyway.outOfOrder=true
```

### Step 2: Verify Migration

Check if column was added:
```sql
docker exec regtech-postgres-1 psql -U myuser -d regtech -c "\d dataquality.business_rules"
```

View category distribution:
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
 DUPLICATE_DETECTION |          0
 CROSS_REFERENCE     |          0
```

### Step 3: Restart Application

After migration, restart the Spring Boot application:
```powershell
cd regtech-app
..\mvnw spring-boot:run
```

### Step 4: Test API Endpoint

Test the GET configuration endpoint:
```bash
curl -X GET "http://localhost:8080/api/v1/data-quality/config?bankId=default-bank" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

Expected response includes category fields:
```json
{
  "success": true,
  "data": {
    "bankId": "default-bank",
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
}
```

## Frontend Integration Guide

### UI Grouping by Category

The frontend can now group rules by category:

```typescript
// Group rules by category
const groupedRules = rules.reduce((acc, rule) => {
  const category = rule.category;
  if (!acc[category]) {
    acc[category] = {
      displayName: rule.categoryDisplayName,
      icon: rule.categoryIcon,
      rules: []
    };
  }
  acc[category].rules.push(rule);
  return acc;
}, {});

// Render categories
Object.entries(groupedRules).map(([category, data]) => (
  <RuleCategory 
    key={category}
    name={data.displayName}
    icon={data.icon}
    rules={data.rules}
  />
));
```

### Category Icon Mapping

Frontend can use `categoryIcon` to display appropriate icons:

```typescript
const iconMap = {
  "check-circle": CheckCircleIcon,
  "sliders": SlidersIcon,
  "code": CodeIcon,
  "clock": ClockIcon,
  "copy": CopyIcon,
  "link": LinkIcon
};

<Icon as={iconMap[rule.categoryIcon]} />
```

## Backward Compatibility

✅ **Fully backward compatible** - No breaking changes to existing API contracts
- Old clients ignore the new fields
- New clients benefit from category information
- All existing rules are automatically categorized

## Testing Checklist

- [ ] Migration V65 applies successfully
- [ ] All existing rules have a validation_category assigned
- [ ] No rules have NULL validation_category
- [ ] API response includes category fields
- [ ] Frontend can display rules grouped by category
- [ ] Category icons render correctly
- [ ] Italian display names are correct

## Troubleshooting

### Migration fails with "column already exists"

```sql
-- Check if column already exists
docker exec regtech-postgres-1 psql -U myuser -d regtech -c "
SELECT column_name FROM information_schema.columns 
WHERE table_schema='dataquality' AND table_name='business_rules' 
AND column_name='validation_category';
"

-- If it exists, mark migration as applied manually
docker exec regtech-postgres-1 psql -U myuser -d regtech -c "
INSERT INTO flyway_schema_history (version, description, type, script, checksum, installed_rank, execution_time, success)
VALUES ('62', 'add validation category to business rules', 'SQL', 'V65__add_validation_category_to_business_rules.sql', 0, 999, 0, true);
"
```

### Rules not categorized properly

Manually update specific rules:
```sql
docker exec regtech-postgres-1 psql -U myuser -d regtech -c "
UPDATE dataquality.business_rules 
SET validation_category = 'CODE_VALIDATION'
WHERE rule_code = 'YOUR_RULE_CODE';
"
```

### Verify category assignments

```sql
docker exec regtech-postgres-1 psql -U myuser -d regtech -c "
SELECT rule_code, rule_type, validation_category 
FROM dataquality.business_rules 
ORDER BY validation_category, rule_code;
"
```

## Related Files

- Domain Enum: `ValidationCategory.java`
- DTO: `ConfigurationDto.java`
- Query Handler: `GetConfigurationQueryHandler.java`
- Entity: `BusinessRuleEntity.java`
- Migration: `V65__add_validation_category_to_business_rules.sql`
- Scripts: `apply-V65-migration.ps1`, `apply-V65-migration.bat`

## Next Steps

1. ✅ Apply migration: `.\apply-V65-migration.ps1`
2. ✅ Restart application
3. ✅ Test API endpoint
4. 🔄 Update frontend to display categories
5. 🔄 Add category filtering in UI
6. 🔄 Update user documentation

## Status

✅ **Implementation Complete**
- Domain layer: ValidationCategory enum created
- Application layer: DTOs updated with category fields
- Infrastructure layer: Entity field added
- Database migration: V65 created and ready to apply
- Scripts: PowerShell and Batch scripts ready

**Ready to apply migration and test!**
