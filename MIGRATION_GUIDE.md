# RegTech Database Migration Guide

## Overview

This guide covers database migration procedures for the RegTech application using Flyway. The application uses PostgreSQL with schema-based organization across multiple modules.

## Prerequisites

### 1. Docker Environment
```bash
# Start PostgreSQL container
docker-compose up -d

# Verify container is running
docker ps | grep postgres
```

### 2. Database Connection
- **Host**: localhost
- **Port**: 5433 (to avoid conflicts with local PostgreSQL on 5432)
- **Database**: regtech
- **Username**: myuser
- **Password**: secret

### 3. Environment Variables (for Application Startup)
```powershell
$env:STRIPE_API_KEY="STRIPE_TEST_SECRET_KEY_REDACTED"
$env:STRIPE_WEBHOOK_SECRET="acct_1SHmgAF6vJewYjaQ"
$env:CURRENCY_API_KEY="cur_live_uQYFLcNvMmlt34FHKxBLr7tRrEe7BV8R6GguneHl"
```

## Migration Structure

### Schema Organization
```
regtech-app/src/main/resources/db/migration/
├── V3__init_schemas.sql                    # Creates all schemas (MUST run first)
├── common/                                  # Shared/core infrastructure
│   ├── V4__create_core_event_tables.sql
│   ├── V5__Increase_event_type_length.sql
│   └── V6__Migrate_outbox_messages_schema.sql
├── iam/                                     # Identity and access management
│   ├── V10__Create_roles_and_permissions_tables.sql
│   ├── V11__Create_refresh_tokens_table.sql
│   ├── V12__Create_banks_table.sql
│   ├── V13__Clear_refresh_tokens_for_hash_change.sql
│   └── V61__Insert_italian_banks.sql
├── billing/                                 # Payment processing
│   ├── V20__create_billing_tables.sql
│   ├── V21__Update_subscriptions_schema.sql
│   └── V22__Update_invoices_schema.sql
├── ingestion/                               # Data ingestion
│   ├── V30__create_ingestion_tables.sql
│   ├── V31__Increase_batch_id_length.sql
│   ├── V32__Insert_sample_bank_info.sql
│   └── V33__Increase_bank_id_length.sql
├── dataquality/                             # Validation rules
│   ├── V40__create_rules_engine_tables.sql
│   ├── V41__insert_regulations.sql
│   ├── V42__insert_initial_business_rules.sql
│   ├── V43__create_quality_reports_tables.sql
│   ├── V44__increase_report_id_length.sql
│   └── V45__increase_quality_grade_length.sql
├── riskcalculation/                         # Risk analysis
│   ├── V50__Create_risk_calculation_tables.sql
│   ├── V51__Create_chunk_metadata_table.sql
│   ├── V52__Increase_counterparty_lei_length.sql
│   ├── V53__Add_calculation_results_uri_to_batches.sql
│   ├── V54__Add_version_to_batches.sql
│   └── V55__Add_version_to_portfolio_analysis.sql
└── reportgeneration/                        # Report generation
    └── V60__create_report_generation_tables.sql
```

## Migration Methods

### Method 1: Manual Migration Scripts (Recommended for Production)

#### Available Scripts
- `apply-v6-migration.ps1` / `apply-v6-migration.bat`
- `apply-v21-migration.ps1` / `apply-v21-migration.bat`
- `apply-v22-migration.ps1` / `apply-v22-migration.bat`
- `apply-v43-migration.ps1` / `apply-v43-migration.bat`
- `apply-v44-migration.ps1` / `apply-v44-migration.bat`
- `apply-v45-migration.ps1` / `apply-v45-migration.bat`
- `apply-v52-migration.ps1` / `apply-v52-migration.bat`
- `apply-v53-migration.ps1` / `apply-v53-migration.bat`
- `apply-v54-migration.ps1` / `apply-v54-migration.bat`
- `apply-v55-migration.ps1` / `apply-v55-migration.bat`

#### Usage Example
```powershell
# Apply V55 migration
.\apply-v55-migration.ps1
```

### Method 2: Automatic Migration via Maven (Development/Testing)

#### Basic Migration
```bash
cd regtech-app
../mvnw flyway:migrate
```

#### Migration with Custom Properties
```bash
cd regtech-app
../mvnw flyway:migrate -Dflyway.url="jdbc:postgresql://localhost:5433/regtech" -Dflyway.user=myuser -Dflyway.password=secret
```

#### Migration with Out-of-Order Support
```bash
cd regtech-app
../mvnw flyway:migrate "-Dflyway.outOfOrder=true"
```

#### Using Properties File
Create `regtech-app/flyway.properties`:
```properties
flyway.outOfOrder=true
flyway.url=jdbc:postgresql://localhost:5433/regtech
flyway.user=myuser
flyway.password=secret
```

Then run:
```bash
cd regtech-app
../mvnw flyway:migrate
```

## Verification Steps

### 1. Check Migration History
```sql
docker exec regtech-postgres-1 psql -U myuser -d regtech -c "SELECT version, description, installed_on FROM flyway_schema_history ORDER BY installed_rank;"
```

### 2. Verify Schema Creation
```sql
docker exec regtech-postgres-1 psql -U myuser -d regtech -c "\dn"
```

Expected schemas:
- billing
- core
- dataquality
- iam
- ingestion
- public
- reportgeneration
- riskcalculation

### 3. Check Table Counts by Schema
```sql
docker exec regtech-postgres-1 psql -U myuser -d regtech -c "SELECT schemaname, COUNT(*) as table_count FROM pg_tables WHERE schemaname NOT IN ('pg_catalog', 'information_schema') GROUP BY schemaname ORDER BY schemaname;"
```

Expected results:
```
schemaname    | table_count
--------------+-------------
billing       | 9
core          | 4
dataquality   | 9
iam           | 6
ingestion     | 2
public        | 4
reportgeneration | 2
riskcalculation | 5
```

### 4. List All Tables
```sql
docker exec regtech-postgres-1 psql -U myuser -d regtech -c "SELECT schemaname, tablename FROM pg_tables WHERE schemaname NOT IN ('pg_catalog', 'information_schema') ORDER BY schemaname, tablename;"
```

## Application Startup

### With Automatic Flyway Migration
```bash
cd regtech-app
$env:STRIPE_API_KEY="STRIPE_TEST_SECRET_KEY_REDACTED"
$env:STRIPE_WEBHOOK_SECRET="acct_1SHmgAF6vJewYjaQ"
$env:CURRENCY_API_KEY="cur_live_uQYFLcNvMmlt34FHKxBLr7tRrEe7BV8R6GguneHl"
../mvnw spring-boot:run -Dspring.profiles.active=development
```

### With Flyway Disabled (After Manual Migration)
```bash
cd regtech-app
$env:STRIPE_API_KEY="STRIPE_TEST_SECRET_KEY_REDACTED"
$env:STRIPE_WEBHOOK_SECRET="acct_1SHmgAF6vJewYjaQ"
$env:CURRENCY_API_KEY="cur_live_uQYFLcNvMmlt34FHKxBLr7tRrEe7BV8R6GguneHl"
../mvnw spring-boot:run -Dspring.profiles.active=development -Dspring.flyway.enabled=false
```

## Troubleshooting

### Common Issues

#### 1. Flyway Validation Errors
**Error**: "Detected resolved migration not applied to database"
**Solution**: Use `-Dflyway.outOfOrder=true` or apply migrations in order

#### 2. Port Conflicts
**Error**: Connection refused on port 5432
**Solution**: Use port 5433 for Docker PostgreSQL (compose.yaml maps 5433:5432)

#### 3. Migration Order Issues
**Error**: Later migrations fail because dependent tables don't exist
**Solution**: Ensure V3 (schema creation) runs first, then apply migrations in numerical order

#### 4. Maven Property Parsing Issues
**Error**: Unknown lifecycle phase
**Solution**: Quote properties with special characters: `"-Dflyway.outOfOrder=true"`

#### 5. PostgreSQL Version Warnings
**Warning**: PostgreSQL 18.1 newer than Flyway support
**Solution**: This is just a warning, migrations should still work

### Recovery Procedures

#### Reset Database
```bash
# Stop application
# Drop and recreate database
docker exec regtech-postgres-1 psql -U myuser -c "DROP DATABASE regtech;"
docker exec regtech-postgres-1 psql -U myuser -c "CREATE DATABASE regtech;"

# Re-run migrations from beginning
```

#### Clean Flyway History
```sql
docker exec regtech-postgres-1 psql -U myuser -d regtech -c "DELETE FROM flyway_schema_history WHERE version IN ('4', '5', '6', '10', '11', '12', '13', '20', '21', '22', '30', '31', '32', '33', '40', '41', '42', '43', '44', '45', '60', '61');"
```

## Migration Scripts Reference

### Core Infrastructure (V1-V9)
- **V3**: Creates all database schemas
- **V4-V6**: Event processing tables (outbox, inbox, sagas)

### IAM Module (V10-V19)
- **V10-V13**: User management, roles, permissions, authentication
- **V61**: Sample data insertion

### Billing Module (V20-V29)
- **V20**: Core billing tables
- **V21-V22**: Schema updates for subscriptions and invoices

### Ingestion Module (V30-V39)
- **V30-V33**: Data ingestion and batch processing tables

### Data Quality Module (V40-V49)
- **V40-V45**: Rules engine, regulations, quality reports

### Risk Calculation Module (V50-V59)
- **V50-V55**: Risk analysis tables and schema updates

### Report Generation Module (V60-V69)
- **V60**: Report generation and metadata tables

## Best Practices

1. **Backup Before Migration**: Always backup production data
2. **Test in Development**: Apply migrations in dev environment first
3. **Version Control**: Keep migration scripts in version control
4. **Order Matters**: Apply migrations in numerical order when possible
5. **Verify After Migration**: Always verify table creation and data integrity
6. **Document Changes**: Update this guide when adding new migrations

## Quick Start Commands

```powershell
# Start database
docker-compose up -d

# Apply all migrations automatically
cd regtech-app
../mvnw flyway:migrate "-Dflyway.outOfOrder=true"

# Start application
$env:STRIPE_API_KEY="STRIPE_TEST_SECRET_KEY_REDACTED"
$env:STRIPE_WEBHOOK_SECRET="acct_1SHmgAF6vJewYjaQ"
$env:CURRENCY_API_KEY="cur_live_uQYFLcNvMmlt34FHKxBLr7tRrEe7BV8R6GguneHl"
../mvnw spring-boot:run -Dspring.profiles.active=development
```