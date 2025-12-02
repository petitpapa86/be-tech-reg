# Requirements Document

## Introduction

The RegTech application currently uses a hybrid database initialization approach with both `schema.sql` (for schema creation) and scattered Flyway migrations across modules. This creates inconsistency, makes it difficult to track schema evolution, and complicates deployment. This feature consolidates all database schema management under Flyway for proper version control and migration tracking.

## Glossary

- **Flyway**: Database migration tool that manages versioned SQL scripts
- **Schema**: A PostgreSQL namespace that groups related database tables
- **Migration**: A versioned SQL script that modifies the database structure
- **Entity**: A JPA/Hibernate class that maps to a database table
- **V1__init_schemas.sql**: The initial Flyway migration that creates all schemas
- **Module**: A bounded context in the application (IAM, Billing, Ingestion, Data Quality, Risk Calculation, Report Generation)

## Requirements

### Requirement 1

**User Story:** As a database administrator, I want all database schemas to be created through Flyway migrations, so that I can track schema creation in version control.

#### Acceptance Criteria

1. WHEN the application starts with an empty database THEN Flyway SHALL create all required schemas (billing, iam, ingestion, dataquality, riskcalculation, reportgeneration)
2. WHEN Flyway runs the initial migration THEN the system SHALL create schemas in a single V1__init_schemas.sql file
3. WHEN reviewing the migration history THEN the system SHALL show V1__init_schemas.sql as the first migration
4. WHEN the schema.sql file exists THEN the system SHALL not execute it (spring.sql.init.mode set to never)
5. WHERE Flyway is enabled THEN the system SHALL disable spring.sql.init to prevent conflicts

### Requirement 2

**User Story:** As a developer, I want all existing table creation scripts moved to appropriate schema-specific migration folders, so that I can understand which tables belong to which module.

#### Acceptance Criteria

1. WHEN organizing migrations THEN the system SHALL place IAM-related migrations in db/migration/iam folder
2. WHEN organizing migrations THEN the system SHALL place Billing-related migrations in db/migration/billing folder
3. WHEN organizing migrations THEN the system SHALL place Ingestion-related migrations in db/migration/ingestion folder
4. WHEN organizing migrations THEN the system SHALL place Data Quality-related migrations in db/migration/dataquality folder
5. WHEN organizing migrations THEN the system SHALL place Risk Calculation-related migrations in db/migration/riskcalculation folder
6. WHEN organizing migrations THEN the system SHALL place Report Generation-related migrations in db/migration/reportgeneration folder
7. WHEN organizing migrations THEN the system SHALL place shared/core migrations in db/migration/common folder
8. WHEN a migration file is moved THEN the system SHALL preserve its version number and timestamp

### Requirement 3

**User Story:** As a developer, I want to map all JPA entities to their corresponding database schemas, so that I can verify schema organization is correct.

#### Acceptance Criteria

1. WHEN mapping entities THEN the system SHALL document which entities belong to the iam schema
2. WHEN mapping entities THEN the system SHALL document which entities belong to the billing schema
3. WHEN mapping entities THEN the system SHALL document which entities belong to the ingestion schema
4. WHEN mapping entities THEN the system SHALL document which entities belong to the dataquality schema
5. WHEN mapping entities THEN the system SHALL document which entities belong to the riskcalculation schema
6. WHEN mapping entities THEN the system SHALL document which entities belong to the reportgeneration schema
7. WHEN an entity lacks a @Table annotation with schema THEN the system SHALL flag it for correction

### Requirement 4

**User Story:** As a developer, I want to test the Flyway migration process with clean/migrate cycles, so that I can ensure migrations work correctly from scratch.

#### Acceptance Criteria

1. WHEN running flyway:clean THEN the system SHALL drop all schemas and tables
2. WHEN running flyway:migrate after clean THEN the system SHALL recreate all schemas and tables
3. WHEN migrations complete successfully THEN the system SHALL record all migrations in flyway_schema_history table
4. WHEN a migration fails THEN the system SHALL provide clear error messages indicating which migration failed
5. WHEN running the application after migration THEN the system SHALL start successfully without schema errors

### Requirement 5

**User Story:** As a developer, I want Flyway enabled in the main application configuration, so that migrations run automatically on application startup.

#### Acceptance Criteria

1. WHEN the application starts THEN Flyway SHALL be enabled (spring.flyway.enabled: true)
2. WHEN Flyway is enabled THEN spring.sql.init.mode SHALL be set to never
3. WHEN Flyway runs THEN the system SHALL scan all configured migration locations
4. WHEN Flyway completes THEN the system SHALL log the number of migrations applied
5. WHERE migrations are pending THEN the system SHALL apply them before the application starts
