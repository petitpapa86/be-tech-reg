# Value Object Placement Guidelines

## Purpose

This document provides clear guidelines for organizing value objects in the report-generation domain layer. It explains the rationale behind the current organization and provides decision-making frameworks for placing new value objects as the domain evolves.

## Core Principle

**Value objects should be organized by their business relationship to aggregates, not by technical classification.**

Following Domain-Driven Design (DDD) principles:
- **High cohesion within aggregates**: Value objects that are intrinsic to an aggregate's behavior should live with that aggregate
- **Low coupling between aggregates**: Shared concepts should be extracted to prevent direct dependencies between aggregates

## Current Organization

### Aggregate-Specific Value Objects (`domain/generation/`)

These value objects are tightly coupled to the `GeneratedReport` aggregate and represent concepts specific to report generation behavior:

- **CalculatedExposure**: Individual large exposure with regulatory calculations
- **ConcentrationIndices**: Herfindahl-Hirschman indices for risk concentration
- **GeographicBreakdown**: Geographic distribution of exposures
- **SectorBreakdown**: Sector distribution of exposures
- **ValidationResult**: XBRL validation results
- **ValidationError**: Individual XBRL validation errors

**Why here?** These objects are only meaningful in the context of generating reports. They encapsulate domain logic specific to the report generation process and are not reused elsewhere.

### Shared Value Objects (`domain/shared/valueobjects/`)

These value objects are used across multiple aggregates, layers, or bounded contexts:

#### Identity Value Objects
- **ReportId**: Unique identifier for reports (used across all layers)
- **BatchId**: Unique identifier for batches (shared with ingestion and risk-calculation)
- **BankId**: Unique identifier for banks (shared across all modules)

#### Temporal Value Objects
- **ReportingDate**: Business date for reporting (used in queries, events, and multiple aggregates)
- **ProcessingTimestamps**: Audit timestamps (used across all aggregates)

#### Status Value Objects
- **ReportStatus**: Report lifecycle status (used in queries, events, and UI)
- **ComplianceStatus**: Regulatory compliance status (used in reports and quality checks)
- **XbrlValidationStatus**: XBRL validation status (used in metadata and queries)

#### Metadata Value Objects
- **HtmlReportMetadata**: HTML report metadata (used in aggregate and application layer)
- **XbrlReportMetadata**: XBRL report metadata (used in aggregate and application layer)

#### Infrastructure-Related Value Objects
- **S3Uri**: S3 storage location (used across domain and infrastructure)
- **PresignedUrl**: Temporary access URL (used in domain and presentation)
- **FileSize**: File size with validation (used across layers)

#### Quality Value Objects
- **QualityGrade**: Quality assessment grade (shared with data-quality module)
- **QualityDimension**: Quality dimension identifier (shared with data-quality module)

#### Other Shared Value Objects
- **AmountEur**: Monetary amount in EUR (used across multiple aggregates)
- **ReportType**: Type of report (HTML/XBRL) (used in queries and routing)
- **ReportContent**: Report content wrapper (used in storage and retrieval)
- **FailureReason**: Failure reason description (used in error handling)
- **BatchContext**: Batch processing context (shared across modules)

**Why here?** These objects represent cross-cutting domain concepts that need to be consistently understood across multiple parts of the system.

## Decision Tree for New Value Objects

Use this decision tree when creating a new value object:

```
┌─────────────────────────────────────────────────────────────┐
│ Is this value object used by only ONE aggregate?             │
└─────────────────┬───────────────────────────────────────────┘
                  │
        ┌─────────┴─────────┐
        │                   │
       YES                 NO
        │                   │
        ▼                   ▼
┌───────────────────┐  ┌────────────────────────────────────┐
│ Place in          │  │ Is it used across multiple         │
│ aggregate package │  │ aggregates or layers?              │
│                   │  └──────────┬─────────────────────────┘
│ Example:          │             │
│ domain/           │   ┌─────────┴─────────┐
│   generation/     │   │                   │
│     NewValue.java │  YES                 NO
└───────────────────┘   │                   │
                        ▼                   ▼
              ┌──────────────────┐  ┌──────────────────────┐
              │ Place in shared  │  │ Start in aggregate   │
              │ valueobjects/    │  │ package, move when   │
              │                  │  │ second usage appears │
              │ Example:         │  │                      │
              │ domain/shared/   │  │ Rationale: YAGNI     │
              │   valueobjects/  │  │ (You Aren't Gonna    │
              │     NewValue.java│  │  Need It)            │
              └──────────────────┘  └──────────────────────┘
```

## Placement Examples

### Example 1: ReportSummary (Hypothetical)

**Scenario**: You need to create a `ReportSummary` value object that contains key metrics from a generated report.

**Analysis**:
- Used by: Only the `GeneratedReport` aggregate
- Purpose: Encapsulates summary data specific to report generation
- Shared?: No other aggregates need this concept

**Decision**: Place in `domain/generation/ReportSummary.java`

**Rationale**: This is aggregate-specific behavior. The summary is meaningful only in the context of the GeneratedReport aggregate.

### Example 2: RegionCode (Hypothetical)

**Scenario**: You need to create a `RegionCode` value object for geographic regions.

**Analysis**:
- Used by: Report generation, risk calculation, and data quality modules
- Purpose: Standardized region identifier
- Shared?: Yes, multiple bounded contexts need consistent region codes

**Decision**: Place in `domain/shared/valueobjects/RegionCode.java`

**Rationale**: This is a shared kernel concept that needs to be understood consistently across multiple modules.

### Example 3: ReportValidationRule (Hypothetical)

**Scenario**: You need to create a `ReportValidationRule` value object for custom validation rules.

**Analysis**:
- Used by: Currently only report generation
- Purpose: Encapsulates validation logic
- Shared?: Uncertain - might be used by quality checks later

**Decision**: Start in `domain/generation/ReportValidationRule.java`, move to shared if needed

**Rationale**: Follow YAGNI principle. Don't prematurely generalize. Move to shared when a second genuine use case appears.

## When to Move Value Objects Between Packages

### From Aggregate to Shared

**Move when**:
1. A second aggregate needs the same concept
2. The application layer needs direct access (not just passing through)
3. The value object becomes part of integration events shared across bounded contexts
4. Multiple modules need to understand the concept consistently

**Process**:
1. Create the value object in `domain/shared/valueobjects/`
2. Update all imports in the original aggregate
3. Update imports in the new usage location
4. Run all tests to ensure nothing breaks
5. Update package-info.java documentation

**Example**: If `ValidationResult` starts being used by a separate quality-checking aggregate, move it to shared.

### From Shared to Aggregate

**Move when**:
1. Usage analysis shows only one aggregate actually uses it
2. The value object becomes tightly coupled to specific aggregate behavior
3. The concept is no longer needed across multiple contexts

**Process**:
1. Verify no other aggregates or modules use it (search codebase)
2. Move the value object to the aggregate package
3. Update all imports
4. Run all tests
5. Update package-info.java documentation

**Note**: This is rare and usually indicates initial misclassification.

## Anti-Patterns to Avoid

### ❌ Anti-Pattern 1: "All value objects in shared/"

**Problem**: Creates a "god package" with low cohesion. Makes it hard to understand which objects belong to which aggregate.

**Example**: Putting `CalculatedExposure` in shared when it's only used by `GeneratedReport`.

**Why it's wrong**: Violates the DDD principle of high cohesion within aggregates.

### ❌ Anti-Pattern 2: "Organize by technical type"

**Problem**: Grouping all "identifiers" together or all "amounts" together, regardless of business relationship.

**Example**: Creating `domain/identifiers/` and `domain/amounts/` packages.

**Why it's wrong**: Technical classification doesn't reflect business relationships. Makes it harder to understand the domain model.

### ❌ Anti-Pattern 3: "Premature generalization"

**Problem**: Moving value objects to shared before there's a genuine second use case.

**Example**: Moving `ValidationError` to shared "because we might need it elsewhere someday."

**Why it's wrong**: Violates YAGNI principle. Creates unnecessary coupling and makes refactoring harder.

### ❌ Anti-Pattern 4: "Duplicate value objects"

**Problem**: Creating similar value objects in multiple places instead of sharing.

**Example**: Having `ReportId` in generation package and `ReportIdentifier` in shared.

**Why it's wrong**: Creates inconsistency and makes integration difficult.

## Rationale for Current Organization

### Why Not Move Everything to Shared?

The current structure follows the **Aggregate Pattern** from DDD:

1. **Encapsulation**: Aggregate-specific value objects encapsulate domain logic that's only relevant within that aggregate
2. **Cohesion**: Keeping related objects together makes the aggregate easier to understand and maintain
3. **Autonomy**: Aggregates can evolve independently without affecting shared concepts
4. **Clear Boundaries**: The organization makes aggregate boundaries explicit

### Why Not Keep Everything in Aggregate Packages?

Shared value objects serve important purposes:

1. **Consistency**: Identifiers like `ReportId` need to be understood the same way everywhere
2. **Integration**: Events and commands that cross aggregate boundaries need shared vocabulary
3. **Reusability**: Common concepts like `AmountEur` shouldn't be duplicated
4. **Loose Coupling**: Shared objects prevent direct dependencies between aggregates

### The Balance

The current organization strikes a balance:
- **Aggregate packages**: High cohesion for aggregate-specific behavior
- **Shared package**: Low coupling for cross-cutting concerns

This is the **sweet spot** for maintainable domain models.

## Consistency Across Modules

This organization pattern is consistent across all modules in the system:

### regtech-risk-calculation
- Aggregate-specific: `CalculatedExposure`, `ConcentrationIndices` in `calculation/`
- Shared: `AmountEur`, `ProcessingTimestamps` in `shared/valueobjects/`

### regtech-billing
- Aggregate-specific: Invoice details in `invoices/`
- Shared: `Money`, `BillingPeriod` in `shared/valueobjects/`

### regtech-data-quality
- Aggregate-specific: Quality rule details in `rules/`
- Shared: `QualityGrade`, `QualityDimension` in `shared/valueobjects/`

**Key Insight**: All modules follow the same principle: aggregate-specific value objects live with their aggregate, truly shared concepts live in shared/.

## Quick Reference Checklist

When creating a new value object, ask yourself:

- [ ] Is this concept used by only one aggregate? → **Aggregate package**
- [ ] Is this concept used across multiple aggregates? → **Shared package**
- [ ] Is this concept part of integration events? → **Shared package**
- [ ] Is this concept an identifier used in queries? → **Shared package**
- [ ] Is this concept specific to aggregate behavior? → **Aggregate package**
- [ ] Am I uncertain about future usage? → **Start in aggregate, move later if needed**

## Summary

**Golden Rule**: Place value objects based on their business relationship to aggregates, not technical classification.

**Default Strategy**: Start specific (aggregate package), generalize when needed (shared package).

**Key Principle**: High cohesion within aggregates, low coupling between aggregates.

Following these guidelines ensures a maintainable, understandable domain model that accurately reflects the business domain while remaining flexible for future evolution.

---

*For more information about the domain layer organization, see [DOMAIN_LAYER_ORGANIZATION.md](../DOMAIN_LAYER_ORGANIZATION.md)*

*For package-level documentation, see:*
- *[domain/generation/package-info.java](src/main/java/com/bcbs239/regtech/reportgeneration/domain/generation/package-info.java)*
- *[domain/shared/valueobjects/package-info.java](src/main/java/com/bcbs239/regtech/reportgeneration/domain/shared/valueobjects/package-info.java)*
