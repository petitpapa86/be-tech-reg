# Design Document: Report Generation Domain Value Objects Reorganization

## Overview

This design addresses the organization of value objects in the report-generation domain layer. Currently, value objects are split between the `generation/` aggregate package and a separate `shared/valueobjects/` package, creating ambiguity about placement and violating DDD cohesion principles. This design establishes clear rules for value object placement based on their usage patterns and business relationships.

## Architecture

### Current State Analysis

**Current Structure:**
```
domain/
├── generation/
│   ├── CalculatedExposure.java        # Value object
│   ├── ConcentrationIndices.java      # Value object
│   ├── GeographicBreakdown.java       # Value object
│   ├── SectorBreakdown.java           # Value object
│   ├── ValidationResult.java          # Value object
│   ├── ValidationError.java           # Value object
│   ├── CalculationResults.java        # Entity
│   ├── QualityResults.java            # Entity
│   ├── ReportMetadata.java            # Entity
│   └── GeneratedReport.java           # Aggregate root
│
└── shared/
    └── valueobjects/
        ├── AmountEur.java
        ├── BankId.java
        ├── BatchContext.java
        ├── BatchId.java
        ├── ComplianceStatus.java
        ├── FailureReason.java
        ├── FileSize.java
        ├── HtmlReportMetadata.java
        ├── PresignedUrl.java
        ├── ProcessingTimestamps.java
        ├── QualityDimension.java
        ├── QualityGrade.java
        ├── ReportContent.java
        ├── ReportId.java
        ├── ReportingDate.java
        ├── ReportStatus.java
        ├── ReportType.java
        ├── S3Uri.java
        ├── XbrlReportMetadata.java
        └── XbrlValidationStatus.java
```

**Problem:** The current structure is actually CORRECT according to DDD principles, but the documentation doesn't explain WHY this organization exists.

### Design Decision: Keep Current Structure

After analysis, the current structure follows proper DDD patterns:

1. **Aggregate-Specific Value Objects in generation/**
   - CalculatedExposure, ConcentrationIndices, GeographicBreakdown, SectorBreakdown
   - These are tightly coupled to the GeneratedReport aggregate
   - They represent concepts specific to report generation behavior

2. **Shared Value Objects in shared/valueobjects/**
   - ReportId, BatchId, BankId, ReportingDate, ReportStatus, etc.
   - These are used across multiple contexts (application, domain, infrastructure)
   - They represent cross-cutting domain concepts

**Rationale:** This follows the DDD principle of "high cohesion within aggregates, low coupling between aggregates." Value objects that are part of an aggregate's behavior should live with that aggregate.

## Components and Interfaces

### Value Object Classification

#### Category 1: Aggregate-Specific Value Objects (Stay in generation/)

**CalculatedExposure**
- Purpose: Represents a single calculated large exposure with regulatory data
- Usage: Only used within GeneratedReport aggregate and report generation logic
- Location: `domain/generation/CalculatedExposure.java` ✅ CORRECT

**ConcentrationIndices**
- Purpose: Herfindahl-Hirschman indices for risk concentration assessment
- Usage: Only used within GeneratedReport aggregate
- Location: `domain/generation/ConcentrationIndices.java` ✅ CORRECT

**GeographicBreakdown**
- Purpose: Geographic distribution of exposures
- Usage: Only used within GeneratedReport aggregate
- Location: `domain/generation/GeographicBreakdown.java` ✅ CORRECT

**SectorBreakdown**
- Purpose: Sector distribution of exposures
- Usage: Only used within GeneratedReport aggregate
- Location: `domain/generation/SectorBreakdown.java` ✅ CORRECT

**ValidationResult**
- Purpose: XBRL validation results
- Usage: Only used within report generation context
- Location: `domain/generation/ValidationResult.java` ✅ CORRECT

**ValidationError**
- Purpose: Individual XBRL validation error
- Usage: Only used within report generation context
- Location: `domain/generation/ValidationError.java` ✅ CORRECT

#### Category 2: Shared Value Objects (Stay in shared/valueobjects/)

**Identity Value Objects:**
- ReportId, BatchId, BankId - Used across all layers and contexts
- Location: `domain/shared/valueobjects/` ✅ CORRECT

**Temporal Value Objects:**
- ReportingDate, ProcessingTimestamps - Used across multiple aggregates
- Location: `domain/shared/valueobjects/` ✅ CORRECT

**Status Value Objects:**
- ReportStatus, ComplianceStatus, XbrlValidationStatus - Used in queries and events
- Location: `domain/shared/valueobjects/` ✅ CORRECT

**Metadata Value Objects:**
- HtmlReportMetadata, XbrlReportMetadata - Used in aggregate and application layer
- Location: `domain/shared/valueobjects/` ✅ CORRECT

**Infrastructure-Related Value Objects:**
- S3Uri, PresignedUrl, FileSize - Used across domain and infrastructure
- Location: `domain/shared/valueobjects/` ✅ CORRECT

**Quality Value Objects:**
- QualityGrade, QualityDimension - Used across quality and report generation
- Location: `domain/shared/valueobjects/` ✅ CORRECT

**Other Shared Value Objects:**
- AmountEur, ReportType, ReportContent, FailureReason, BatchContext
- Location: `domain/shared/valueobjects/` ✅ CORRECT

## Data Models

No changes to data models - this is purely an organizational concern with documentation.

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system-essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property 1: Aggregate-specific value objects remain co-located
*For any* value object used exclusively by a single aggregate, that value object should reside in the same package as its aggregate
**Validates: Requirements 1.1, 2.1, 2.2, 2.3, 2.4, 2.5**

### Property 2: Shared value objects remain in shared package
*For any* value object used by multiple aggregates or across layers, that value object should reside in the shared/valueobjects package
**Validates: Requirements 1.2, 3.1, 3.2, 3.3, 3.4, 3.5**

### Property 3: No duplicate value objects
*For any* value object concept, there should exist exactly one implementation in the domain layer
**Validates: Requirements 1.5**

### Property 4: Package structure reflects usage patterns
*For any* value object, its package location should accurately reflect whether it's aggregate-specific or shared
**Validates: Requirements 1.3, 1.4**

### Property 5: Documentation completeness
*For any* developer examining the domain structure, documentation should clearly explain the placement rationale for value objects
**Validates: Requirements 4.1, 4.2, 4.3, 4.4, 4.5**

## Error Handling

No error handling changes - this is a documentation and organizational improvement.

## Testing Strategy

### Unit Testing
- No new tests required - existing tests remain valid
- All existing value object tests continue to work without modification
- Import statements in tests will not change since package structure is not changing

### Property-Based Testing
Not applicable for this organizational change.

### Documentation Testing
- Verify that documentation accurately describes the current structure
- Ensure examples in documentation match actual code organization
- Validate that placement guidelines are clear and actionable

## Implementation Notes

### What Changes:
1. **Documentation Only** - Create comprehensive documentation explaining:
   - Why value objects are organized this way
   - Rules for placing new value objects
   - Examples of correct placement decisions
   - Rationale for the current structure

2. **Add Package Documentation** - Create package-info.java files:
   - `domain/generation/package-info.java` - Explain aggregate-specific value objects
   - `domain/shared/valueobjects/package-info.java` - Explain shared value objects

### What Doesn't Change:
1. **No Code Movement** - All value objects stay in their current locations
2. **No Refactoring** - No changes to value object implementations
3. **No Import Changes** - No updates to import statements
4. **No Test Changes** - All tests remain unchanged

### Placement Guidelines for Future Development:

**Decision Tree for New Value Objects:**

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

**When to Move a Value Object:**

1. **From Aggregate to Shared:**
   - When a second aggregate needs the same concept
   - When application layer needs direct access
   - When it becomes part of integration events

2. **From Shared to Aggregate:**
   - When usage analysis shows only one aggregate uses it
   - When it becomes tightly coupled to aggregate behavior
   - Rare - usually indicates initial misclassification

## Comparison with Other Modules

This organization is consistent with other modules:

**regtech-risk-calculation:**
- Aggregate-specific: CalculatedExposure, ConcentrationIndices in `calculation/`
- Shared: AmountEur, ProcessingTimestamps in `shared/valueobjects/`

**regtech-billing:**
- Aggregate-specific: Invoice details in `invoices/`
- Shared: Money, BillingPeriod in `shared/valueobjects/`

All modules follow the same principle: **aggregate-specific value objects live with their aggregate, truly shared concepts live in shared/**.

## Conclusion

The current value object organization in report-generation domain is **already correct** according to DDD principles. The issue was not the structure itself, but the lack of documentation explaining the rationale. This design provides that documentation and establishes clear guidelines for future development.

The key insight: **Not all value objects belong in a shared package.** Value objects that are intrinsic to an aggregate's behavior should be co-located with that aggregate for maximum cohesion.
