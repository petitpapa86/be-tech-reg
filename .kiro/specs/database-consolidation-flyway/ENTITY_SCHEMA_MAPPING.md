# Entity-to-Schema Mapping Document

## Overview

This document provides a comprehensive mapping of all JPA entities in the RegTech application to their corresponding database schemas. This mapping is essential for the Flyway migration consolidation effort to ensure all tables are created in the correct schemas.

**Date Created:** 2024-12-02  
**Purpose:** Support database consolidation under Flyway migration management

---

## Schema Organization Summary

The application uses 7 schemas:
- **public** - Core/shared infrastructure (outbox, inbox, sagas, event processing)
- **iam** - Identity and Access Management
- **billing** - Billing and subscription management
- **ingestion** - Data ingestion and batch processing
- **dataquality** - Data quality validation and rules engine
- **riskcalculation** - Risk calculation and portfolio analysis
- **reportgeneration** - Report generation and storage

---

## 1. IAM Schema (`iam`)

### Entities with Correct Schema Annotation

| Entity Class | Table Name | Schema | Location |
|-------------|------------|--------|----------|
| `UserEntity` | `users` | `iam` | `regtech-iam/infrastructure/.../entities/UserEntity.java` |
| `RoleEntity` | `roles` | `iam` | `regtech-iam/infrastructure/.../entities/RoleEntity.java` |
| `RolePermissionEntity` | `role_permissions` | `iam` | `regtech-iam/infrastructure/.../entities/RolePermissionEntity.java` |
| `UserRoleEntity` | `user_roles` | `iam` | `regtech-iam/infrastructure/.../entities/UserRoleEntity.java` |
| `RefreshTokenEntity` | `refresh_tokens` | `iam` | `regtech-iam/infrastructure/.../entities/RefreshTokenEntity.java` |
| `BankEntity` | `banks` | `iam` | `regtech-iam/infrastructure/.../entities/BankEntity.java` |
| `UserBankAssignmentEntity` | `user_bank_assignments` | `iam` | `regtech-iam/infrastructure/.../entities/UserBankAssignmentEntity.java` |
| `InboxEventEntity` | `iam_inbox_events` | `iam` | `regtech-iam/infrastructure/.../entities/InboxEventEntity.java` |

**Status:** ✅ All IAM entities have correct `@Table(schema = "iam")` annotations

---

## 2. Billing Schema (`billing`)

### Entities with Correct Schema Annotation

| Entity Class | Table Name | Schema | Location |
|-------------|------------|--------|----------|
| `BillingAccountEntity` | `billing_accounts` | `billing` | `regtech-billing/infrastructure/.../entities/BillingAccountEntity.java` |
| `SubscriptionEntity` | `subscriptions` | `billing` | `regtech-billing/infrastructure/.../entities/SubscriptionEntity.java` |
| `InvoiceEntity` | `invoices` | `billing` | `regtech-billing/infrastructure/.../entities/InvoiceEntity.java` |
| `InvoiceLineItemEntity` | `invoice_line_items` | `billing` | `regtech-billing/infrastructure/.../entities/InvoiceLineItemEntity.java` |
| `DunningCaseEntity` | `dunning_cases` | `billing` | `regtech-billing/infrastructure/.../entities/DunningCaseEntity.java` |
| `DunningActionEntity` | `dunning_actions` | `billing` | `regtech-billing/infrastructure/.../entities/DunningActionEntity.java` |
| `ProcessedWebhookEventEntity` | `processed_webhook_events` | `billing` | `regtech-billing/infrastructure/.../entities/ProcessedWebhookEventEntity.java` |
| `SagaAuditLogEntity` | `saga_audit_log` | `billing` | `regtech-billing/infrastructure/.../entities/SagaAuditLogEntity.java` |
| `BillingDomainEventEntity` | `billing_domain_events` | `billing` | `regtech-billing/infrastructure/.../entities/BillingDomainEventEntity.java` |

**Status:** ✅ All Billing entities have correct `@Table(schema = "billing")` annotations

---

## 3. Ingestion Schema (`ingestion`)

### Entities with Correct Schema Annotation

| Entity Class | Table Name | Schema | Location |
|-------------|------------|--------|----------|
| `IngestionBatchEntity` | `ingestion_batches` | `ingestion` | `regtech-ingestion/infrastructure/.../persistence/IngestionBatchEntity.java` |
| `BankInfoEntity` | `bank_info` | `ingestion` | `regtech-ingestion/infrastructure/.../persistence/BankInfoEntity.java` |

**Status:** ✅ All Ingestion entities have correct `@Table(schema = "ingestion")` annotations

---

## 4. Data Quality Schema (`dataquality`)

### Entities with Correct Schema Annotation

| Entity Class | Table Name | Schema | Location |
|-------------|------------|--------|----------|
| `QualityReportEntity` | `quality_reports` | `dataquality` | `regtech-data-quality/infrastructure/.../reporting/QualityReportEntity.java` |
| `QualityErrorSummaryEntity` | `quality_error_summaries` | `dataquality` | `regtech-data-quality/infrastructure/.../reporting/QualityErrorSummaryEntity.java` |
| `BusinessRule` | `business_rules` | `dataquality` | `regtech-data-quality/domain/.../rulesengine/domain/BusinessRule.java` |
| `RuleParameter` | `rule_parameters` | `dataquality` | `regtech-data-quality/domain/.../rulesengine/domain/RuleParameter.java` |
| `RuleExemption` | `rule_exemptions` | `dataquality` | `regtech-data-quality/domain/.../rulesengine/domain/RuleExemption.java` |
| `RuleExecutionLog` | `rule_execution_log` | `dataquality` | `regtech-data-quality/domain/.../rulesengine/domain/RuleExecutionLog.java` |
| `RuleViolation` | `rule_violations` | `dataquality` | `regtech-data-quality/domain/.../rulesengine/domain/RuleViolation.java` |

**Status:** ✅ All Data Quality entities have correct `@Table(schema = "dataquality")` annotations

**Note:** Rules engine entities are located in the domain layer (not infrastructure), which is acceptable for this bounded context.

---

## 5. Risk Calculation Schema (`riskcalculation`)

### Entities with Correct Schema Annotation

| Entity Class | Table Name | Schema | Location |
|-------------|------------|--------|----------|
| `BatchEntity` | `batches` | `riskcalculation` | `regtech-risk-calculation/infrastructure/.../entities/BatchEntity.java` |
| `ExposureEntity` | `exposures` | `riskcalculation` | `regtech-risk-calculation/infrastructure/.../entities/ExposureEntity.java` |
| `MitigationEntity` | `mitigations` | `riskcalculation` | `regtech-risk-calculation/infrastructure/.../entities/MitigationEntity.java` |
| `PortfolioAnalysisEntity` | `portfolio_analysis` | `riskcalculation` | `regtech-risk-calculation/infrastructure/.../entities/PortfolioAnalysisEntity.java` |
| `ChunkMetadataEntity` | `chunk_metadata` | `riskcalculation` | `regtech-risk-calculation/infrastructure/.../entities/ChunkMetadataEntity.java` |

**Status:** ✅ All Risk Calculation entities have correct `@Table(schema = "riskcalculation")` annotations

---

## 6. Report Generation Schema (`reportgeneration`)

### Entities with Correct Schema Annotation

| Entity Class | Table Name | Schema | Location |
|-------------|------------|--------|----------|
| `GeneratedReportEntity` | `generated_reports` | `reportgeneration` | `regtech-report-generation/infrastructure/.../entities/GeneratedReportEntity.java` |

**Status:** ✅ All Report Generation entities have correct `@Table(schema = "reportgeneration")` annotations

---

## 7. Public/Core Schema (`public`)

### Entities with Correct Schema Annotation (No explicit schema = public)

| Entity Class | Table Name | Schema | Location |
|-------------|------------|--------|----------|
| `OutboxMessageEntity` | `outbox_messages` | `public` (default) | `regtech-core/infrastructure/.../eventprocessing/OutboxMessageEntity.java` |
| `InboxMessageEntity` | `inbox_messages` | `public` (default) | `regtech-core/infrastructure/.../eventprocessing/InboxMessageEntity.java` |
| `EventProcessingFailureEntity` | `event_processing_failures` | `public` (default) | `regtech-core/infrastructure/.../eventprocessing/EventProcessingFailureEntity.java` |
| `SagaEntity` | `sagas` | `public` (default) | `regtech-core/infrastructure/.../saga/SagaEntity.java` |

**Status:** ✅ All Core entities correctly use default `public` schema (no explicit schema annotation)

---

## Summary Statistics

| Schema | Entity Count | Status |
|--------|--------------|--------|
| `iam` | 8 | ✅ Complete |
| `billing` | 9 | ✅ Complete |
| `ingestion` | 2 | ✅ Complete |
| `dataquality` | 7 | ✅ Complete |
| `riskcalculation` | 5 | ✅ Complete |
| `reportgeneration` | 1 | ✅ Complete |
| `public` | 4 | ✅ Complete |
| **Total** | **36** | **✅ All Correct** |

---

## Findings

### ✅ Positive Findings

1. **All entities have correct schema annotations** - Every entity class reviewed has the appropriate `@Table(schema = "...")` annotation matching its bounded context
2. **Consistent naming conventions** - Table names follow snake_case convention consistently
3. **Proper schema separation** - Each module's entities are correctly isolated to their respective schemas
4. **Core infrastructure properly in public schema** - Shared components (outbox, inbox, sagas) correctly use the default public schema

### 📋 No Issues Found

- **No missing @Table annotations** - All entities have proper table definitions
- **No missing schema specifications** - All module-specific entities explicitly declare their schema
- **No schema mismatches** - All entities are in the correct schema for their bounded context

---

## Recommendations for Flyway Migration

Based on this mapping, the following migration structure is recommended:

### V1__init_schemas.sql
Create all 6 schemas:
```sql
CREATE SCHEMA IF NOT EXISTS iam;
CREATE SCHEMA IF NOT EXISTS billing;
CREATE SCHEMA IF NOT EXISTS ingestion;
CREATE SCHEMA IF NOT EXISTS dataquality;
CREATE SCHEMA IF NOT EXISTS riskcalculation;
CREATE SCHEMA IF NOT EXISTS reportgeneration;
```

### Migration Folder Organization
```
db/migration/
├── V1__init_schemas.sql
├── common/          # V2-V9: public schema tables
├── iam/             # V10-V19: IAM tables
├── billing/         # V20-V29: Billing tables
├── ingestion/       # V30-V39: Ingestion tables
├── dataquality/     # V40-V49: Data Quality tables
├── riskcalculation/ # V50-V59: Risk Calculation tables
└── reportgeneration/# V60-V69: Report Generation tables
```

---

## Verification Checklist

- [x] All IAM entities reviewed and documented
- [x] All Billing entities reviewed and documented
- [x] All Ingestion entities reviewed and documented
- [x] All Data Quality entities reviewed and documented
- [x] All Risk Calculation entities reviewed and documented
- [x] All Report Generation entities reviewed and documented
- [x] All Core infrastructure entities reviewed and documented
- [x] Schema annotations verified for all entities
- [x] No missing or incorrect schema assignments found

---

## Next Steps

1. ✅ **Entity mapping complete** - All 36 entities mapped to correct schemas
2. ⏭️ **Create V1__init_schemas.sql** - Create initial schema creation migration
3. ⏭️ **Organize existing migrations** - Move existing migrations to schema-specific folders
4. ⏭️ **Create missing table migrations** - Generate migrations for tables not yet covered
5. ⏭️ **Configure Flyway** - Update application.yml and pom.xml with Flyway configuration

---

**Document Status:** ✅ Complete  
**Last Updated:** 2024-12-02  
**Reviewed By:** Kiro AI Agent
