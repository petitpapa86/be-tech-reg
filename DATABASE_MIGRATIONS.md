# Database Migrations Guide

## Overview

The RegTech Platform uses Flyway for database schema management and version control. All database schemas, tables, and structural changes are managed through versioned SQL migration scripts, ensuring consistent and reproducible database states across all environments.

## Migration Folder Structure

All migrations are located in `regtech-app/src/main/resources/db/migration/` with the following organization:

```
regtech-app/src/main/resources/db/migration/
├── V1__init_schemas.sql                    # Creates all schemas (MUST run first)
├── common/                                  # Shared/core infrastructure
│   ├── V2__create_core_event_tables.sql
│   └── V3__Increase_event_type_length.sql
├── iam/                                     # IAM module migrations
│   ├── V10__Create_roles_and_permissions_tables.sql
│   ├── V11__Add_role_id_to_user_roles_table.sql
│   ├── V12__Create_refresh_tokens_table.sql
│   ├── V13__Create_banks_table.sql
│   └── V14__Clear_refresh_tokens_for_hash_change.sql
├── billing/                                 # Billing module migrations
│   └── V20__create_billing_tables.sql
├── ingestion/                               # Ingestion module migrations
│   └── V30__create_ingestion_tables.sql
├── dataquality/                             # Data Quality migrations
│   ├── V40__create_rules_engine_tables.sql
│   ├── V41__insert_initial_business_rules.sql
│   ├── V42__update_business_rules_constraints.sql
│   └── V43__insert_regulations.sql
├── riskcalculation/                         # Risk Calculation migrations
│   ├── V50__Create_risk_calculation_tables.sql
│   └── V51__Create_chunk_metadata_table.sql
└── reportgeneration/                        # Report Generation migrations
    └── V60__create_report_generation_tables.sql
```

## Version Numbering Strategy

Migrations use a structured version numbering system to organize changes by module:

| Version Range | Module | Purpose |
|--------------|--------|---------|
| **V1** | Schema Init | Creates all database schemas |
| **V2-V9** | Common | Shared infrastructure (outbox, inbox, events, sagas) |
| **V10-V19** | IAM | Identity and access management tables |
| **V20-V29** | Billing | Payment processing and subscription tables |
| **V30-V39** | Ingestion | Data ingestion and batch processing tables |
| **V40-V49** | Data Quality | Validation rules and quality reports |
| **V50-V59** | Risk Calculation | Risk assessment and portfolio analysis |
| **V60-V69** | Report Generation | Regulatory reporting and metadata |
| **V70+** | Future | Reserved for new modules or cross-cutting concerns |

### Naming Convention

Migration files follow the Flyway naming convention:

```
V{version}__{description}.sql
```

**Examples:**
- `V1__init_schemas.sql` - Initial schema creation
- `V10__Create_roles_and_permissions_tables.sql` - IAM tables
- `V40__create_rules_engine_tables.sql` - Data quality tables

**Rules:**
- Version numbers must be unique across all migration folders
- Use double underscore (`__`) between version and description
- Use descriptive names that explain what the migration does
- Use snake_case or PascalCase for descriptions (be consistent)

## Database Schemas

The platform uses PostgreSQL schemas to organize tables by bounded context:

| Schema | Purpose | Tables |
|--------|---------|--------|
| **public** | Shared infrastructure | outbox_messages, inbox_messages, event_processing_failures, sagas |
| **iam** | User management | users, roles, permissions, refresh_tokens, banks |
| **billing** | Payment processing | billing_accounts, subscriptions, invoices, dunning_cases |
| **ingestion** | Data ingestion | ingestion_batches, bank_info |
| **dataquality** | Quality validation | quality_reports, business_rules, rule_violations |
| **riskcalculation** | Risk assessment | batches, exposures, mitigations, portfolio_analysis |
| **reportgeneration** | Report generation | generated_reports |

## Running Migrations

### Using Maven (Recommended)

Flyway is configured as a Maven plugin in `regtech-app/pom.xml`. Use these commands:

#### Apply All Pending Migrations
```bash
mvn flyway:migrate -pl regtech-app
```

#### Check Migration Status
```bash
mvn flyway:info -pl regtech-app
```

#### Validate Applied Migrations
```bash
mvn flyway:validate -pl regtech-app
```

#### Clean Database (⚠️ DESTRUCTIVE - Development Only)
```bash
mvn flyway:clean -pl regtech-app
```
**Warning:** This drops ALL schemas and data. Never use in production!

#### Repair Migration History
```bash
mvn flyway:repair -pl regtech-app
```
Use this to fix checksum mismatches or remove failed migration entries.

### Using Application Startup

Flyway runs automatically when the application starts if migrations are pending:

```bash
mvn spring-boot:run -pl regtech-app
```

The application configuration (`application.yml`) has Flyway enabled:

```yaml
spring:
  flyway:
    enabled: true
    baseline-on-migrate: true
```

### Environment-Specific Configuration

Configure database connection in `.env` or environment variables:

```bash
# Database Configuration
DB_HOST=localhost
DB_PORT=5432
DB_NAME=regtech
DB_USERNAME=regtech_user
DB_PASSWORD=your_password

# For Maven Flyway plugin
export FLYWAY_URL=jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}
export FLYWAY_USER=${DB_USERNAME}
export FLYWAY_PASSWORD=${DB_PASSWORD}
```

## Creating New Migrations

### Step 1: Determine Version Number

Choose the next available version in your module's range:

- IAM: V10-V19
- Billing: V20-V29
- Ingestion: V30-V39
- Data Quality: V40-V49
- Risk Calculation: V50-V59
- Report Generation: V60-V69

### Step 2: Create Migration File

Create a new SQL file in the appropriate module folder:

```bash
# Example: Adding a new table to IAM module
touch regtech-app/src/main/resources/db/migration/iam/V15__create_audit_log_table.sql
```

### Step 3: Write Migration SQL

```sql
-- V15__create_audit_log_table.sql
-- Description: Add audit logging for user actions

CREATE TABLE IF NOT EXISTS iam.audit_log (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    action VARCHAR(100) NOT NULL,
    resource_type VARCHAR(50) NOT NULL,
    resource_id VARCHAR(255),
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(45),
    user_agent TEXT,
    
    CONSTRAINT fk_audit_user FOREIGN KEY (user_id) 
        REFERENCES iam.users(id) ON DELETE CASCADE
);

CREATE INDEX idx_audit_log_user_id ON iam.audit_log(user_id);
CREATE INDEX idx_audit_log_timestamp ON iam.audit_log(timestamp);
CREATE INDEX idx_audit_log_action ON iam.audit_log(action);

COMMENT ON TABLE iam.audit_log IS 'Audit trail for user actions';
```

### Step 4: Test Migration

```bash
# Test on clean database
mvn flyway:clean -pl regtech-app
mvn flyway:migrate -pl regtech-app

# Verify migration applied
mvn flyway:info -pl regtech-app
```

### Step 5: Update Entity Annotations

If adding new tables, ensure JPA entities have correct schema annotations:

```java
@Entity
@Table(name = "audit_log", schema = "iam")
public class AuditLogEntity {
    // ...
}
```

## Migration Best Practices

### ✅ DO

1. **Always use IF NOT EXISTS** for idempotent migrations:
   ```sql
   CREATE TABLE IF NOT EXISTS schema.table_name (...);
   ```

2. **Add comments** to document table purposes:
   ```sql
   COMMENT ON TABLE iam.users IS 'User accounts with authentication credentials';
   ```

3. **Create indexes** for foreign keys and frequently queried columns:
   ```sql
   CREATE INDEX idx_table_column ON schema.table(column);
   ```

4. **Use transactions** implicitly (Flyway wraps each migration in a transaction)

5. **Test migrations** on a clean database before committing

6. **Keep migrations small** and focused on a single change

7. **Use descriptive names** that explain the migration purpose

### ❌ DON'T

1. **Never modify existing migrations** after they've been applied to any environment

2. **Don't use DROP TABLE** without careful consideration (data loss!)

3. **Avoid complex data transformations** in migrations (use application code)

4. **Don't skip version numbers** - maintain sequential numbering

5. **Never commit migrations with syntax errors**

6. **Don't use database-specific features** without documenting compatibility

## Troubleshooting

### Problem: Migration Fails with Syntax Error

**Symptoms:**
```
ERROR: syntax error at or near "TABLE"
Migration V15__create_audit_log_table.sql failed
```

**Solution:**
1. Fix the SQL syntax error in the migration file
2. Run `mvn flyway:repair -pl regtech-app` to remove the failed entry
3. Run `mvn flyway:migrate -pl regtech-app` again

### Problem: Checksum Mismatch

**Symptoms:**
```
ERROR: Migration checksum mismatch for migration version 10
```

**Solution:**
This means a previously-applied migration file was modified. You have two options:

**Option 1: Repair (if change was intentional)**
```bash
mvn flyway:repair -pl regtech-app
```

**Option 2: Revert (if change was accidental)**
```bash
git checkout HEAD -- regtech-app/src/main/resources/db/migration/iam/V10__*.sql
```

### Problem: Schema Already Exists

**Symptoms:**
```
ERROR: schema "iam" already exists
```

**Solution:**
This happens when running migrations on an existing database. Flyway's `baseline-on-migrate: true` should handle this, but if not:

```bash
mvn flyway:baseline -pl regtech-app
mvn flyway:migrate -pl regtech-app
```

### Problem: Table Already Exists

**Symptoms:**
```
ERROR: relation "iam.users" already exists
```

**Solution:**
Ensure all CREATE TABLE statements use `IF NOT EXISTS`:

```sql
CREATE TABLE IF NOT EXISTS iam.users (...);
```

### Problem: Migration Out of Order

**Symptoms:**
```
ERROR: Detected resolved migration not applied to database: V15
```

**Solution:**
Flyway detected a migration with a lower version number than already-applied migrations. This happens when:
- A team member created V15 while you had already applied V16
- Solution: Rename the new migration to the next available version

### Problem: Application Won't Start After Migration

**Symptoms:**
```
Error creating bean with name 'entityManagerFactory'
Table 'schema.table_name' doesn't exist
```

**Solution:**
1. Check that migrations ran successfully:
   ```bash
   mvn flyway:info -pl regtech-app
   ```

2. Verify entity annotations match migration schemas:
   ```java
   @Table(name = "table_name", schema = "schema_name")
   ```

3. Check that `spring.sql.init.mode` is set to `never` in `application.yml`

### Problem: Flyway Not Running on Startup

**Symptoms:**
Application starts but migrations don't run

**Solution:**
Check `application.yml` configuration:

```yaml
spring:
  flyway:
    enabled: true  # Must be true
    baseline-on-migrate: true
  sql:
    init:
      mode: never  # Must be never (not 'always')
```

## Migration Workflow

### Development Environment

1. **Create migration** in appropriate module folder
2. **Test locally** with clean database:
   ```bash
   mvn flyway:clean -pl regtech-app
   mvn flyway:migrate -pl regtech-app
   ```
3. **Run application** to verify entities work:
   ```bash
   mvn spring-boot:run -pl regtech-app
   ```
4. **Commit migration** to version control

### Staging Environment

1. **Backup database** before applying migrations
2. **Run migrations**:
   ```bash
   mvn flyway:migrate -pl regtech-app
   ```
3. **Verify migration status**:
   ```bash
   mvn flyway:info -pl regtech-app
   ```
4. **Test application** functionality

### Production Environment

1. **Schedule maintenance window** (if needed)
2. **Backup database** (critical!)
3. **Run migrations** with monitoring:
   ```bash
   mvn flyway:migrate -pl regtech-app
   ```
4. **Verify migration status**:
   ```bash
   mvn flyway:info -pl regtech-app
   ```
5. **Monitor application** logs and health checks
6. **Rollback plan**: Have compensating migrations ready if needed

## Rollback Strategy

Flyway Community Edition does not support automatic rollback. For this project:

### Forward-Only Migrations

All changes should be additive when possible:

**✅ Good:**
```sql
-- V16__add_email_column.sql
ALTER TABLE iam.users ADD COLUMN IF NOT EXISTS email VARCHAR(255);
```

**❌ Avoid:**
```sql
-- V16__remove_username_column.sql
ALTER TABLE iam.users DROP COLUMN username;  -- Can't easily undo!
```

### Compensating Migrations

If you need to undo a change, create a new migration:

```sql
-- V17__remove_email_column.sql
-- Compensating migration for V16
ALTER TABLE iam.users DROP COLUMN IF EXISTS email;
```

### Database Backups

Always backup before major migrations:

```bash
# PostgreSQL backup
pg_dump -h localhost -U regtech_user -d regtech > backup_$(date +%Y%m%d_%H%M%S).sql

# Restore if needed
psql -h localhost -U regtech_user -d regtech < backup_20241203_120000.sql
```

## Monitoring and Validation

### Check Migration History

View all applied migrations:

```bash
mvn flyway:info -pl regtech-app
```

Output shows:
- Version number
- Description
- Type (SQL)
- Installed date
- State (Success/Pending/Failed)
- Checksum

### Validate Migrations

Ensure applied migrations match source files:

```bash
mvn flyway:validate -pl regtech-app
```

### Query Flyway History Table

Flyway tracks migrations in `flyway_schema_history`:

```sql
SELECT 
    installed_rank,
    version,
    description,
    type,
    script,
    installed_on,
    execution_time,
    success
FROM flyway_schema_history
ORDER BY installed_rank;
```

## Additional Resources

- **Flyway Documentation**: https://flywaydb.org/documentation/
- **Entity Schema Mapping**: See `.kiro/specs/database-consolidation-flyway/ENTITY_SCHEMA_MAPPING.md`
- **Design Document**: See `.kiro/specs/database-consolidation-flyway/design.md`
- **Requirements**: See `.kiro/specs/database-consolidation-flyway/requirements.md`

## Support

For questions or issues with database migrations:

1. Check this guide's troubleshooting section
2. Review Flyway logs in application output
3. Consult the design document for architecture decisions
4. Contact the development team for complex migration scenarios


## Recent Migrations

### V22 - Update Invoices Schema (December 2025)

**File:** `billing/V22__Update_invoices_schema.sql`

**Purpose:** Updates the invoices table schema to match the current domain model with detailed amount breakdown.

**Changes:**

**Invoices table:**
- **Removed columns:**
  - `amount_due` (replaced with detailed breakdown)
  - `currency` (replaced with per-amount currencies)
  - `subscription_id` (no longer needed)

- **Added columns:**
  - `subscription_amount_value` + `subscription_amount_currency` - Subscription fee
  - `overage_amount_value` + `overage_amount_currency` - Overage charges
  - `total_amount_value` + `total_amount_currency` - Total amount
  - `billing_period_start_date` + `billing_period_end_date` - Billing period
  - `invoice_number` - Human-readable invoice number (unique)
  - `issue_date` - Invoice issue date
  - `sent_at` - When invoice was sent to customer

**Invoice Line Items table:**
- **Removed columns:**
  - `amount` (replaced with unit and total amounts)
  - `currency` (replaced with per-amount currencies)

- **Added columns:**
  - `unit_amount_value` + `unit_amount_currency` - Unit price
  - `total_amount_value` + `total_amount_currency` - Total (unit × quantity)

**Impact:** Breaking change - updates invoice schema to support detailed amount breakdown and proper billing period tracking.

**Related:** See `INVOICES_SCHEMA_FIX.md` for detailed documentation and troubleshooting.

### V21 - Update Subscriptions Schema (December 2025)

**File:** `billing/V21__Update_subscriptions_schema.sql`

**Purpose:** Updates the subscriptions table schema to match the current domain model with tier-based pricing.

**Changes:**
- Dropped and recreated subscriptions table with correct schema
- Added `tier` column for subscription tiers (STARTER, PROFESSIONAL, ENTERPRISE)
- Updated foreign key relationships
- Made `billing_account_id` nullable for subscriptions created before account

**Impact:** Breaking change - requires subscription data migration if any exists.

**Related:** See `SUBSCRIPTIONS_SCHEMA_FIX.md` for detailed documentation.

## Troubleshooting

### Migration Failed: Column Already Exists

If you see errors like "column already exists" or "relation already exists":

1. Check if the migration was partially applied:
   ```sql
   SELECT * FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 10;
   ```

2. If the migration shows as failed, you may need to manually fix the schema and mark it as successful:
   ```sql
   -- Fix the schema manually, then:
   UPDATE flyway_schema_history 
   SET success = true 
   WHERE version = 'XX' AND success = false;
   ```

### Migration Failed: Constraint Violation

If you see constraint violations during migration:

1. Check existing data that might violate new constraints
2. Add data migration steps before adding constraints
3. Use `ALTER TABLE ... DISABLE TRIGGER ALL` if needed (carefully!)

### Schema Mismatch Between Database and Domain Model

If you see errors like "column does not exist" or "null value violates not-null constraint":

1. Check the Flyway migration history:
   ```sql
   SELECT version, description, installed_on, success 
   FROM flyway_schema_history 
   ORDER BY installed_rank;
   ```

2. Compare database schema with JPA entities:
   ```sql
   \d+ schema_name.table_name
   ```

3. Apply any missing migrations or create new ones to align schemas

4. See module-specific fix documents (e.g., `INVOICES_SCHEMA_FIX.md`, `SUBSCRIPTIONS_SCHEMA_FIX.md`)

## Best Practices

1. **Always test migrations locally first** before applying to production
2. **Use transactions** - Flyway wraps each migration in a transaction by default
3. **Make migrations idempotent** when possible using `IF EXISTS` / `IF NOT EXISTS`
4. **Never modify applied migrations** - create new migrations to fix issues
5. **Document breaking changes** in migration comments and separate docs
6. **Backup before major migrations** especially when dropping columns or tables
7. **Keep migrations small and focused** - one logical change per migration
8. **Test rollback procedures** even though Flyway doesn't support automatic rollback

## Migration Checklist

Before creating a new migration:

- [ ] Determine the correct version number based on module
- [ ] Follow naming convention: `V{version}__{description}.sql`
- [ ] Place in correct module folder
- [ ] Add comments explaining the purpose
- [ ] Use `IF EXISTS` / `IF NOT EXISTS` where appropriate
- [ ] Test locally with clean database
- [ ] Test with existing data if applicable
- [ ] Document breaking changes
- [ ] Update this guide if adding new patterns
- [ ] Create helper scripts if manual intervention needed (e.g., `apply-vXX-migration.ps1`)
