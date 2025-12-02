# Implementation Plan

- [x] 1. Map existing entities to schemas and document current state





  - Review all entity classes across modules
  - Document which tables exist in which schemas
  - Identify any entities missing @Table schema annotations
  - Create entity-to-schema mapping document
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7_

- [x] 2. Create V1__init_schemas.sql migration





  - Create regtech-app/src/main/resources/db/migration directory
  - Write V1__init_schemas.sql with CREATE SCHEMA statements for all 6 schemas
  - Include IF NOT EXISTS clauses for idempotency
  - Add comments documenting each schema's purpose
  - _Requirements: 1.1, 1.2, 1.3_

- [x] 3. Organize existing migrations into schema-specific folders




- [x] 3.1 Create migration folder structure


  - Create db/migration/common folder
  - Create db/migration/iam folder
  - Create db/migration/billing folder
  - Create db/migration/ingestion folder
  - Create db/migration/dataquality folder
  - Create db/migration/riskcalculation folder
  - Create db/migration/reportgeneration folder
  - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7_


- [x] 3.2 Move IAM migrations

  - Move V202511142041__Create_roles_and_permissions_tables.sql to db/migration/iam/
  - Move V202511142042__Add_role_id_to_user_roles_table.sql to db/migration/iam/
  - Move V202511242100__Create_refresh_tokens_table.sql to db/migration/iam/
  - Move V202511242200__Create_banks_table.sql to db/migration/iam/
  - Move V202511261744__Clear_refresh_tokens_for_hash_change.sql to db/migration/iam/
  - Renumber to V10-V14 range
  - _Requirements: 2.1, 2.8_



- [x] 3.3 Move Data Quality migrations

  - Move V1.8__create_rules_engine_tables.sql to db/migration/dataquality/
  - Move V1.9__insert_initial_business_rules.sql to db/migration/dataquality/
  - Move V1.8.1__update_business_rules_constraints.sql to db/migration/dataquality/
  - Move V1.8.2__insert_regulations.sql to db/migration/dataquality/
  - Renumber to V40-V43 range

  - _Requirements: 2.4, 2.8_

- [x] 3.4 Move Risk Calculation migrations

  - Move V2__Create_risk_calculation_tables.sql to db/migration/riskcalculation/
  - Move V4__Create_chunk_metadata_table.sql to db/migration/riskcalculation/
  - Renumber to V50-V51 range
  - _Requirements: 2.5, 2.8_


- [x] 3.5 Create missing table migrations

  - Create V2__create_core_event_tables.sql for common schema (outbox_messages, inbox_messages, event_processing_failures, sagas)
  - Create V30__create_ingestion_tables.sql for ingestion schema
  - Create V60__create_report_generation_tables.sql for report generation schema
  - Create V20__create_billing_tables.sql for billing schema
  - _Requirements: 2.1, 2.2, 2.3, 2.6_

- [ ] 4. Configure Flyway in application
- [ ] 4.1 Update application.yml
  - Set spring.flyway.enabled: true
  - Set spring.sql.init.mode: never
  - Configure flyway.locations with all migration folders
  - Configure flyway.schemas with all 6 schemas plus public
  - Set flyway.baseline-on-migrate: true for existing databases
  - _Requirements: 1.4, 1.5, 5.1, 5.2, 5.3_

- [ ] 4.2 Add Flyway Maven plugin to regtech-app/pom.xml
  - Add flyway-maven-plugin dependency
  - Configure database connection properties
  - Configure schemas list
  - Configure migration locations
  - _Requirements: 4.1, 4.2_

- [ ] 5. Deprecate schema.sql
  - Rename schema.sql to schema.sql.deprecated
  - Add comment explaining it's replaced by Flyway migrations
  - Update any documentation referencing schema.sql
  - _Requirements: 1.4_

- [ ] 6. Test migration process
- [ ] 6.1 Test clean database migration
  - Run mvn flyway:clean to drop all schemas
  - Run mvn flyway:migrate to apply all migrations
  - Verify all schemas created
  - Verify all tables created in correct schemas
  - Verify flyway_schema_history table populated correctly
  - _Requirements: 4.1, 4.2, 4.3_

- [ ]* 6.2 Write integration test for clean-migrate cycle
  - Create FlywayMigrationIntegrationTest class
  - Test that flyway:clean removes all schemas
  - Test that flyway:migrate recreates all schemas and tables
  - Verify migration order in flyway_schema_history
  - **Property 1: Clean-Migrate Idempotence**
  - **Validates: Requirements 4.2**

- [ ] 6.3 Test application startup after migration
  - Run mvn flyway:migrate
  - Start the application
  - Verify no schema-related errors in logs
  - Verify all modules initialize correctly
  - _Requirements: 4.5, 5.5_

- [ ]* 6.4 Write property-based test for migration idempotence
  - Use jqwik to generate random database states
  - For each state: run clean, run migrate, verify schema structure
  - Run 100 iterations to ensure consistency
  - **Property 1: Clean-Migrate Idempotence**
  - **Validates: Requirements 4.2**

- [ ] 7. Update documentation
  - Update README with Flyway migration instructions
  - Document migration folder structure
  - Document version numbering strategy
  - Add troubleshooting guide for common migration issues
  - _Requirements: 1.2, 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7_

- [ ] 8. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.
