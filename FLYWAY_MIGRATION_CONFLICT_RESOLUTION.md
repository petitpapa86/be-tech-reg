# Flyway Migration Conflict Resolution

## Issue
Application startup was failing with a Flyway migration conflict error:
```
Found more than one migration with version 2
Offenders:
-> regtech-risk-calculation/infrastructure/target/classes/db/migration/V2__Create_risk_calculation_tables.sql
-> regtech-app/target/classes/db/migration/common/V2__create_core_event_tables.sql
```

## Root Cause
The risk-calculation module had a duplicate `V2__Create_risk_calculation_tables.sql` migration file that conflicted with the consolidated `V2__create_core_event_tables.sql` migration in the regtech-app module.

## Resolution Steps

### 1. Removed Duplicate Migration File
Deleted the conflicting migration file:
```
regtech-risk-calculation/infrastructure/src/main/resources/db/migration/V2__Create_risk_calculation_tables.sql
```

### 2. Cleaned Build Artifacts
Ran `mvn clean` to remove all compiled artifacts from the target directories, ensuring the old migration file is no longer present in the classpath.

### 3. Verified Consolidated Migrations
Confirmed that the consolidated migrations in `regtech-app/src/main/resources/db/migration/` are properly organized:

- **V1**: Schema initialization
- **V2**: Core event tables (common)
- **V10-V13, V61**: IAM tables
- **V20**: Billing tables
- **V30-V31**: Ingestion tables
- **V40-V42**: Data quality tables
- **V50-V51**: Risk calculation tables (consolidated)
- **V60**: Report generation tables

## Current Migration Structure

All migrations are now centralized in `regtech-app/src/main/resources/db/migration/` with subdirectories for each module:

```
db/migration/
├── V1__init_schemas.sql
├── common/
│   ├── V2__create_core_event_tables.sql
│   └── V3__Increase_event_type_length.sql
├── iam/
│   ├── V10__Create_roles_and_permissions_tables.sql
│   ├── V11__Create_refresh_tokens_table.sql
│   ├── V12__Create_banks_table.sql
│   ├── V13__Clear_refresh_tokens_for_hash_change.sql
│   └── V61__Insert_italian_banks.sql
├── billing/
│   └── V20__create_billing_tables.sql
├── ingestion/
│   ├── V30__create_ingestion_tables.sql
│   └── V31__Increase_batch_id_length.sql
├── dataquality/
│   ├── V40__create_rules_engine_tables.sql
│   ├── V41__insert_regulations.sql
│   └── V42__insert_initial_business_rules.sql
├── riskcalculation/
│   ├── V50__Create_risk_calculation_tables.sql
│   └── V51__Create_chunk_metadata_table.sql
└── reportgeneration/
    └── V60__create_report_generation_tables.sql
```

## Next Steps

1. Rebuild the application: `mvn install -DskipTests`
2. Start the application: `mvn spring-boot:run -pl regtech-app`
3. Verify that Flyway migrations run successfully without conflicts

## Prevention

To prevent similar issues in the future:
- All Flyway migrations should be added to `regtech-app/src/main/resources/db/migration/`
- Module-specific migration directories should not contain migration files
- Version numbers should follow the established numbering scheme (V10s for IAM, V20s for billing, etc.)
