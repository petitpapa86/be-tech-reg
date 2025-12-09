# Report Generation Domain Layer Organization

## Overview

The report-generation domain layer is organized by **business capabilities** following Domain-Driven Design (DDD) principles. This structure ensures that the domain model clearly expresses the business concepts and rules of report generation, making the codebase more maintainable and aligned with business needs.

## Current Structure ✅

```
domain/
├── generation/              # Report Generation Aggregate
│   ├── events/             # Domain events for report lifecycle
│   ├── GeneratedReport.java           # Aggregate root
│   ├── HtmlReportGenerator.java       # Domain service
│   ├── XbrlReportGenerator.java       # Domain service
│   ├── XbrlValidator.java             # Domain service
│   ├── IGeneratedReportRepository.java # Repository interface
│   ├── CalculationResults.java        # Entity
│   ├── QualityResults.java            # Entity
│   ├── ReportMetadata.java            # Entity
│   ├── CalculatedExposure.java        # Value object
│   ├── ConcentrationIndices.java      # Value object
│   ├── GeographicBreakdown.java       # Value object
│   ├── SectorBreakdown.java           # Value object
│   ├── ValidationResult.java          # Value object
│   ├── ValidationError.java           # Value object
│   ├── HtmlGenerationException.java   # Domain exception
│   ├── XbrlGenerationException.java   # Domain exception
│   └── XbrlValidationException.java   # Domain exception
│
├── storage/                 # Storage Domain Service
│   └── IReportStorageService.java     # Domain service interface
│
├── shared/                  # Shared Kernel
│   └── valueobjects/       # Cross-cutting value objects
│       ├── AmountEur.java
│       ├── BankId.java
│       ├── BatchContext.java
│       ├── BatchId.java
│       ├── ComplianceStatus.java
│       ├── FailureReason.java
│       ├── FileSize.java
│       ├── HtmlReportMetadata.java
│       ├── PresignedUrl.java
│       ├── ProcessingTimestamps.java
│       ├── QualityDimension.java
│       ├── QualityGrade.java
│       ├── ReportContent.java
│       ├── ReportId.java
│       ├── ReportingDate.java
│       ├── ReportStatus.java
│       ├── ReportType.java
│       ├── S3Uri.java
│       ├── XbrlReportMetadata.java
│       └── XbrlValidationStatus.java
│
└── coordination/            # (Empty - coordination is application concern)
```

## Design Principles

### 1. Business Capability Organization

The domain layer is organized around the core business capability: **Report Generation**

- **generation/** - Contains the complete aggregate for generating regulatory reports (HTML and XBRL)
- **storage/** - Abstracts storage concerns as a domain service interface
- **shared/** - Contains value objects used across the domain

### 2. DDD Tactical Patterns

#### Aggregates
- **GeneratedReport** - The aggregate root representing a generated regulatory report
  - Enforces invariants around report lifecycle
  - Manages report metadata, content, and status
  - Publishes domain events (ReportGeneratedEvent, ReportGenerationFailedEvent)

#### Domain Services
- **HtmlReportGenerator** - Encapsulates HTML report generation logic
- **XbrlReportGenerator** - Encapsulates XBRL report generation logic
- **XbrlValidator** - Validates XBRL reports against regulatory schemas
- **IReportStorageService** - Interface for storage operations

#### Value Objects
All value objects are immutable and enforce business rules:
- **AmountEur** - Monetary amounts in EUR with validation
- **BankId** - Bank identifier with format validation
- **BatchId** - Batch identifier
- **ReportingDate** - Date for which report is generated
- **ReportStatus** - Report lifecycle status (PENDING, COMPLETED, FAILED)
- **S3Uri** - S3 storage location
- And many more...

#### Domain Events
- **ReportGeneratedEvent** - Published when report generation succeeds
- **ReportGenerationFailedEvent** - Published when report generation fails
- **ReportGenerationStartedEvent** - Published when report generation begins

#### Repository Interfaces
- **IGeneratedReportRepository** - Persistence abstraction for GeneratedReport aggregate

### 3. Value Object Organization Philosophy

Value objects in the domain layer follow a clear placement strategy based on their usage patterns and business relationships. This organization ensures high cohesion within aggregates while maintaining a shared kernel for cross-cutting concepts.

#### Placement Rules

**Rule 1: Aggregate-Specific Value Objects Stay With Their Aggregate**

Value objects that are used exclusively by a single aggregate should be co-located with that aggregate. This maximizes cohesion and makes the aggregate's complete behavior immediately visible.

**Examples in generation/ package:**
- **CalculatedExposure** - Represents a single calculated large exposure with regulatory data, used only by GeneratedReport
- **ConcentrationIndices** - Herfindahl-Hirschman indices for risk concentration, used only by GeneratedReport
- **GeographicBreakdown** - Geographic distribution of exposures, used only by GeneratedReport
- **SectorBreakdown** - Sector distribution of exposures, used only by GeneratedReport
- **ValidationResult** - XBRL validation results, used only in report generation context
- **ValidationError** - Individual XBRL validation errors, used only in report generation context

**Rule 2: Shared Value Objects Belong in shared/valueobjects/**

Value objects used across multiple aggregates, layers, or contexts should be placed in the shared package. These represent cross-cutting domain concepts that form the "shared kernel" of the bounded context.

**Examples in shared/valueobjects/ package:**

*Identity Value Objects:*
- **ReportId**, **BatchId**, **BankId** - Used across all layers and contexts

*Temporal Value Objects:*
- **ReportingDate**, **ProcessingTimestamps** - Used across multiple aggregates

*Status Value Objects:*
- **ReportStatus**, **ComplianceStatus**, **XbrlValidationStatus** - Used in queries and events

*Metadata Value Objects:*
- **HtmlReportMetadata**, **XbrlReportMetadata** - Used in aggregate and application layer

*Infrastructure-Related Value Objects:*
- **S3Uri**, **PresignedUrl**, **FileSize** - Used across domain and infrastructure

*Quality Value Objects:*
- **QualityGrade**, **QualityDimension** - Used across quality and report generation

*Other Shared Value Objects:*
- **AmountEur**, **ReportType**, **ReportContent**, **FailureReason**, **BatchContext**

#### Rationale: Why This Organization?

**1. High Cohesion Within Aggregates**

When value objects are tightly coupled to an aggregate's behavior, co-locating them makes the aggregate's complete model immediately visible. A developer examining the `generation/` package can see all the concepts that make up report generation without navigating to distant packages.

**2. Low Coupling Between Aggregates**

Shared value objects prevent direct dependencies between aggregates. Instead of Aggregate A depending on Aggregate B's value objects, both depend on shared concepts in the shared kernel.

**3. Follows DDD Principles**

This organization follows Eric Evans' guidance in Domain-Driven Design:
- "Aggregate-specific concepts should be encapsulated within the aggregate"
- "Shared concepts should be explicitly identified and placed in a shared kernel"

**4. Prevents Premature Generalization**

Starting with value objects in the aggregate package and moving them to shared only when a second usage appears follows the YAGNI (You Aren't Gonna Need It) principle. This avoids creating unnecessary abstractions.

#### Decision Tree for New Value Objects

When creating a new value object, follow this decision tree:

```
Is the value object used by only one aggregate?
├─ YES → Place in aggregate package (e.g., domain/generation/)
│         Example: A new ReportSummary used only by GeneratedReport
│
└─ NO → Is it used across multiple aggregates or layers?
         ├─ YES → Place in shared/valueobjects/
         │         Example: A new RegionCode used by multiple modules
         │
         └─ UNCERTAIN → Start in aggregate package, move to shared when second usage appears
                        Rationale: Avoid premature generalization (YAGNI principle)
```

#### When to Move Value Objects

**From Aggregate to Shared:**
- When a second aggregate needs the same concept
- When the application layer needs direct access
- When it becomes part of integration events
- When it's used in queries across multiple aggregates

**From Shared to Aggregate:**
- When usage analysis shows only one aggregate uses it (rare)
- When it becomes tightly coupled to aggregate behavior
- Usually indicates initial misclassification

#### Detailed Guidelines

For comprehensive guidelines on value object placement, including examples and edge cases, see [VALUE_OBJECT_GUIDELINES.md](domain/VALUE_OBJECT_GUIDELINES.md).

#### Consistency Across Modules

This value object organization is consistent with other modules:

**regtech-risk-calculation:**
- Aggregate-specific: CalculatedExposure, ConcentrationIndices in `calculation/`
- Shared: AmountEur, ProcessingTimestamps in `shared/valueobjects/`

**regtech-billing:**
- Aggregate-specific: Invoice details in `invoices/`
- Shared: Money, BillingPeriod in `shared/valueobjects/`

All modules follow the same principle: **aggregate-specific value objects live with their aggregate, truly shared concepts live in shared/**.

### 4. Ubiquitous Language

The domain model uses terminology from the regulatory reporting domain:
- **GeneratedReport** (not "Document" or "File")
- **XBRL** (eXtensible Business Reporting Language)
- **ReportingDate** (not "Date" or "Timestamp")
- **ComplianceStatus** (regulatory compliance)
- **QualityGrade** (data quality assessment)

### 5. Separation of Concerns

#### What Belongs in Domain Layer ✅
- Business rules and invariants
- Domain entities and value objects
- Domain services (pure business logic)
- Domain events
- Repository interfaces (not implementations)

#### What Does NOT Belong in Domain Layer ❌
- Application orchestration (belongs in application layer)
- Infrastructure concerns (belongs in infrastructure layer)
- External system integration (belongs in application/integration)
- UI concerns (belongs in presentation layer)

## Comparison with Application Layer

The domain layer focuses on **what** the business does, while the application layer focuses on **how** to orchestrate it:

| Concern | Domain Layer | Application Layer |
|---------|-------------|-------------------|
| Report Generation Logic | ✅ HtmlReportGenerator, XbrlReportGenerator | ❌ |
| Orchestrating Generation | ❌ | ✅ ComprehensiveReportOrchestrator |
| Event Coordination | ❌ | ✅ ReportCoordinator |
| Integration Events | ❌ | ✅ ReportEventListener |
| Data Aggregation | ❌ | ✅ ComprehensiveReportDataAggregator |

## Why This Structure Works

### 1. Screaming Architecture
Looking at the domain structure immediately tells you this system is about **report generation**

### 2. High Cohesion
All report generation domain logic is co-located in the `generation/` package

### 3. Low Coupling
- Domain layer has no dependencies on application, infrastructure, or presentation layers
- Uses interfaces (IReportStorageService, IGeneratedReportRepository) for external concerns
- Value objects are self-contained with no external dependencies

### 4. Testability
- Pure domain logic can be tested without infrastructure
- Value objects enforce invariants at construction
- Domain services have clear inputs and outputs

### 5. Maintainability
- Changes to report generation logic are isolated to the generation package
- New report types can be added without affecting existing code
- Business rules are explicit and centralized

## Consistency with Other Modules

This organization is consistent with other modules in the system:

- **regtech-risk-calculation/domain** - Organized by calculation, classification, aggregation
- **regtech-data-quality/domain** - Organized by validation, rules, quality assessment
- **regtech-billing/domain** - Organized by accounts, subscriptions, payments, invoices

All follow the same principle: **organize by business capability, not technical layers**.

## Conclusion

The report-generation domain layer is **already properly organized** by business capabilities following DDD principles. No reorganization is needed. The structure clearly communicates the business purpose (report generation), maintains high cohesion within aggregates, and achieves low coupling through well-defined interfaces.

The application layer reorganization (completed in task 8) complemented this domain structure by organizing application concerns (generation orchestration, event coordination, integration) separately from pure domain logic.

---

**Status**: ✅ Domain layer structure verified and documented  
**Date**: November 21, 2025  
**Related**: Application layer reorganization completed (see tasks.md)
