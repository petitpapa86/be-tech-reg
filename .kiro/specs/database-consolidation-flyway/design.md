# Design Document

## Overview

This design consolidates all database schema management under Flyway, replacing the current hybrid approach (schema.sql + scattered migrations) with a unified, versioned migration strategy. The design organizes migrations by bounded context (module) while maintaining a clear initialization sequence.

## Architecture

### Migration Organization Structure

```
regtech-app/src/main/resources/db/migration/
├── V1__init_schemas.sql                    # Creates all schemas
├── common/                                  # Shared/core tables
│   ├── V2__create_event_processing_tables.sql
│   └── V3__create_outbox_inbox_tables.sql
├── iam/                                     # IAM module migrations
│   ├── V10__create_users_table.sql
│   ├── V11__create_roles_permissions.sql
│   ├── V12__create_refresh_tokens.sql
│   └── V13__create_banks_table.sql
├── billing/                                 # Billing module migrations
│   ├── V20__create_billing_accounts.sql
│   ├── V21__create_subscriptions.sql
│   └── V22__create_invoices.sql
├── ingestion/                               # Ingestion module migrations
│   ├── V30__create_batches_table.sql
│   └── V31__create_batch_metadata.sql
├── dataquality/                             # Data Quality migrations
│   ├── V40__create_quality_reports.sql
│   ├── V41__create_rules_engine_tables.sql
│   └── V42__insert_initial_rules.sql
├── riskcalculation/                         # Risk Calculation migrations
│   ├── V50__create_exposures_table.sql
│   ├── V51__create_mitigations_table.sql
│   ├── V52__create_portfolio_analysis.sql
│   └── V53__create_chunk_metadata.sql
└── reportgeneration/                        # Report Generation migrations
    ├── V60__create_generated_reports.sql
    └── V61__create_report_metadata.sql
```

### Version Numbering Strategy

- **V1**: Schema initialization (all schemas)
- **V2-V9**: Common/shared infrastructure
- **V10-V19**: IAM module
- **V20-V29**: Billing module
- **V30-V39**: Ingestion module
- **V40-V49**: Data Quality module
- **V50-V59**: Risk Calculation module
- **V60-V69**: Report Generation module
- **V70+**: Future modules or cross-cutting concerns

This numbering provides clear separation and room for growth within each module.

## Components and Interfaces

### Flyway Configuration

**Location**: `regtech-app/src/main/resources/application.yml`

```yaml
spring:
  flyway:
    enabled: true
    baseline-on-migrate: true
    baseline-version: 0
    locations:
      - classpath:db/migration
      - classpath:db/migration/common
      - classpath:db/migration/iam
      - classpath:db/migration/billing
      - classpath:db/migration/ingestion
      - classpath:db/migration/dataquality
      - classpath:db/migration/riskcalculation
      - classpath:db/migration/reportgeneration
    schemas:
      - public
      - iam
      - billing
      - ingestion
      - dataquality
      - riskcalculation
      - reportgeneration
  
  sql:
    init:
      mode: never  # Disable schema.sql execution
```

### Maven Flyway Plugin Configuration

**Location**: `regtech-app/pom.xml`

```xml
<plugin>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-maven-plugin</artifactId>
    <version>9.22.3</version>
    <configuration>
        <url>jdbc:postgresql://localhost:5432/regtech</url>
        <user>${DB_USERNAME}</user>
        <password>${DB_PASSWORD}</password>
        <schemas>
            <schema>public</schema>
            <schema>iam</schema>
            <schema>billing</schema>
            <schema>ingestion</schema>
            <schema>dataquality</schema>
            <schema>riskcalculation</schema>
            <schema>reportgeneration</schema>
        </schemas>
        <locations>
            <location>filesystem:src/main/resources/db/migration</location>
        </locations>
    </configuration>
</plugin>
```

## Data Models

### Entity-to-Schema Mapping

#### IAM Schema
- `UserEntity` → iam.users
- `RoleEntity` → iam.roles
- `RolePermissionEntity` → iam.role_permissions
- `UserRoleEntity` → iam.user_roles
- `RefreshTokenEntity` → iam.refresh_tokens
- `BankEntity` → iam.banks
- `UserBankAssignmentEntity` → iam.user_bank_assignments
- `InboxEventEntity` → iam.iam_inbox_events

#### Billing Schema
- `BillingAccountEntity` → billing.billing_accounts
- `SubscriptionEntity` → billing.subscriptions
- `InvoiceEntity` → billing.invoices
- `InvoiceLineItemEntity` → billing.invoice_line_items
- `DunningCaseEntity` → billing.dunning_cases
- `DunningActionEntity` → billing.dunning_actions
- `ProcessedWebhookEventEntity` → billing.processed_webhook_events
- `SagaAuditLogEntity` → billing.saga_audit_log
- `BillingDomainEventEntity` → billing.billing_domain_events

#### Ingestion Schema
- `IngestionBatchEntity` → ingestion.ingestion_batches
- `BankInfoEntity` → ingestion.bank_info

#### Data Quality Schema
- `QualityReportEntity` → dataquality.quality_reports
- `QualityErrorSummaryEntity` → dataquality.quality_error_summaries
- `BusinessRuleEntity` → dataquality.business_rules
- `RuleViolationEntity` → dataquality.rule_violations
- `RuleExecutionLogEntity` → dataquality.rule_execution_log

#### Risk Calculation Schema
- `BatchEntity` → riskcalculation.batches
- `ExposureEntity` → riskcalculation.exposures
- `MitigationEntity` → riskcalculation.mitigations
- `PortfolioAnalysisEntity` → riskcalculation.portfolio_analysis
- `ChunkMetadataEntity` → riskcalculation.chunk_metadata

#### Report Generation Schema
- `GeneratedReportEntity` → reportgeneration.generated_reports

### Common/Public Schema
- `OutboxMessageEntity` → public.outbox_messages
- `InboxMessageEntity` → public.inbox_messages
- `EventProcessingFailureEntity` → public.event_processing_failures
- `SagaEntity` → public.sagas

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system-essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Acceptance Criteria Testing Prework

1.1 WHEN the application starts with an empty database THEN Flyway SHALL create all required schemas
Thoughts: This is testing that Flyway correctly executes V1__init_schemas.sql and creates all 6 schemas. We can verify by querying the database for schema existence after migration.
Testable: yes - example

1.2 WHEN Flyway runs the initial migration THEN the system SHALL create schemas in a single V1__init_schemas.sql file
Thoughts: This is verifying file structure and organization, not runtime behavior.
Testable: no

1.3 WHEN reviewing the migration history THEN the system SHALL show V1__init_schemas.sql as the first migration
Thoughts: This tests that the flyway_schema_history table contains V1 as the first entry. This is a specific example we can verify.
Testable: yes - example

1.4 WHEN the schema.sql file exists THEN the system SHALL not execute it
Thoughts: This tests that spring.sql.init.mode=never prevents schema.sql execution. We can verify by checking that tables from schema.sql are not created when only Flyway runs.
Testable: yes - example

1.5 WHERE Flyway is enabled THEN the system SHALL disable spring.sql.init to prevent conflicts
Thoughts: This is a configuration requirement, not a runtime property.
Testable: no

2.1-2.7 WHEN organizing migrations THEN the system SHALL place [module]-related migrations in appropriate folders
Thoughts: These are all about file organization, not runtime behavior.
Testable: no

2.8 WHEN a migration file is moved THEN the system SHALL preserve its version number and timestamp
Thoughts: This is about file management process, not a testable system property.
Testable: no

3.1-3.7 WHEN mapping entities THEN the system SHALL document which entities belong to schemas
Thoughts: These are documentation requirements, not testable properties.
Testable: no

4.1 WHEN running flyway:clean THEN the system SHALL drop all schemas and tables
Thoughts: This is testing Flyway's clean command behavior. We can verify by checking that schemas don't exist after clean.
Testable: yes - example

4.2 WHEN running flyway:migrate after clean THEN the system SHALL recreate all schemas and tables
Thoughts: This is a round-trip property - clean then migrate should restore the database to a known state.
Testable: yes - property

4.3 WHEN migrations complete successfully THEN the system SHALL record all migrations in flyway_schema_history table
Thoughts: This tests that Flyway properly tracks migrations. We can verify by counting entries in flyway_schema_history.
Testable: yes - example

4.4 WHEN a migration fails THEN the system SHALL provide clear error messages
Thoughts: This is about error message quality, which is subjective.
Testable: no

4.5 WHEN running the application after migration THEN the system SHALL start successfully without schema errors
Thoughts: This is an integration test - after migrations, the app should start. This is a specific example.
Testable: yes - example

5.1 WHEN the application starts THEN Flyway SHALL be enabled
Thoughts: This is a configuration check, not a runtime property.
Testable: no

5.2 WHEN Flyway is enabled THEN spring.sql.init.mode SHALL be set to never
Thoughts: This is a configuration check, not a runtime property.
Testable: no

5.3 WHEN Flyway runs THEN the system SHALL scan all configured migration locations
Thoughts: This is testing Flyway's internal behavior, which we trust.
Testable: no

5.4 WHEN Flyway completes THEN the system SHALL log the number of migrations applied
Thoughts: This is about logging behavior, which is observable but not a correctness property.
Testable: no

5.5 WHERE migrations are pending THEN the system SHALL apply them before the application starts
Thoughts: This is testing Flyway's standard behavior of applying pending migrations.
Testable: yes - example

### Property Reflection

After reviewing the prework, most testable items are specific examples rather than universal properties. The one true property identified is:

- 4.2: Clean then migrate round-trip - this is a property that should hold for any database state

The other testable items (1.1, 1.3, 1.4, 4.1, 4.3, 4.5, 5.5) are all specific examples that verify correct setup and configuration.

### Correctness Properties

Property 1: Clean-Migrate Idempotence
*For any* database state, running flyway:clean followed by flyway:migrate should produce the same final schema structure with all tables, indexes, and constraints properly created.
**Validates: Requirements 4.2**

## Error Handling

### Migration Failure Scenarios

1. **Syntax Error in Migration**
   - Flyway will mark the migration as failed
   - Application startup will be blocked
   - Error message will indicate which migration failed
   - Resolution: Fix the SQL syntax and re-run

2. **Duplicate Version Number**
   - Flyway will detect version conflicts
   - Application will fail to start
   - Resolution: Renumber migrations to avoid conflicts

3. **Missing Schema**
   - If a migration references a non-existent schema
   - Migration will fail with clear error
   - Resolution: Ensure V1__init_schemas.sql runs first

4. **Checksum Mismatch**
   - If a previously-run migration is modified
   - Flyway will detect the change and fail
   - Resolution: Create a new migration or use repair command

### Rollback Strategy

Flyway Community Edition does not support automatic rollback. For this project:

1. **Forward-Only Migrations**: All changes are additive
2. **Manual Rollback**: Create compensating migrations (e.g., V14__drop_column.sql)
3. **Backup Before Migration**: Always backup production databases before major migrations
4. **Testing**: Test all migrations in development/staging before production

## Testing Strategy

### Unit Testing

Unit tests will verify:
- Configuration loading (Flyway enabled, correct locations)
- Entity annotations include correct schema names
- Migration file naming follows conventions

### Integration Testing

Integration tests will verify:
- **Clean-Migrate Cycle**: Run flyway:clean, then flyway:migrate, verify all tables exist
- **Application Startup**: After migrations, application starts without errors
- **Schema Verification**: Query information_schema to verify all schemas exist
- **Table Verification**: Query information_schema to verify all expected tables exist in correct schemas
- **Migration History**: Verify flyway_schema_history contains all expected migrations in correct order

### Property-Based Testing

Using jqwik (Java property-based testing library):

**Property Test 1: Clean-Migrate Idempotence**
- Generate random database states (with/without existing tables)
- Run flyway:clean
- Run flyway:migrate
- Verify the resulting schema structure matches expected state
- Repeat 100 times to ensure consistency

### Manual Testing Checklist

1. ✅ Run `mvn flyway:clean` - verify all schemas dropped
2. ✅ Run `mvn flyway:migrate` - verify all migrations applied
3. ✅ Check `flyway_schema_history` table - verify migration order
4. ✅ Start application - verify no schema errors
5. ✅ Run application tests - verify all tests pass
6. ✅ Check logs - verify Flyway reports correct number of migrations

## Migration Execution Order

1. **V1__init_schemas.sql**: Creates all schemas (iam, billing, ingestion, dataquality, riskcalculation, reportgeneration)
2. **Common migrations (V2-V9)**: Create shared infrastructure (outbox, inbox, event processing)
3. **Module migrations (V10+)**: Create module-specific tables in order:
   - IAM (V10-V19)
   - Billing (V20-V29)
   - Ingestion (V30-V39)
   - Data Quality (V40-V49)
   - Risk Calculation (V50-V59)
   - Report Generation (V60-V69)

## Implementation Notes

### Existing Migration Preservation

When moving existing migrations:
1. Keep original version numbers (e.g., V202511142041 stays as-is)
2. Move to appropriate schema folder
3. Update any schema references if needed
4. Do NOT modify checksums (Flyway will detect changes)

### Schema.sql Deprecation

The current `schema.sql` file will be:
1. Replaced by V1__init_schemas.sql
2. Kept in repository for reference (renamed to schema.sql.deprecated)
3. Not executed (spring.sql.init.mode=never)

### Flyway Baseline

For existing databases with tables already created:
- Use `flyway.baseline-on-migrate=true`
- Flyway will baseline at version 0
- Only new migrations will be applied
- Existing tables are assumed to match V1 state
