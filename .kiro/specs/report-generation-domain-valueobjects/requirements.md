# Requirements Document: Report Generation Domain Value Objects Reorganization

## Introduction

The report-generation domain layer currently has value objects scattered across two locations: some within the `generation/` aggregate package and others in a separate `shared/valueobjects/` package. This creates ambiguity about where value objects belong and violates DDD principles of organizing by business capability. This specification addresses reorganizing value objects to follow consistent DDD patterns while maintaining the existing business logic.

## Glossary

- **Value Object**: An immutable object defined by its attributes rather than identity, encapsulating business rules and validation
- **Aggregate**: A cluster of domain objects treated as a single unit for data changes, with a root entity
- **Shared Kernel**: Domain concepts used across multiple aggregates within the same bounded context
- **Generation Aggregate**: The report generation aggregate root (GeneratedReport) and its closely related objects
- **Domain Layer**: The layer containing pure business logic without infrastructure dependencies

## Requirements

### Requirement 1

**User Story:** As a developer, I want value objects organized by their business relationship to aggregates, so that I can quickly understand which objects belong together and maintain high cohesion.

#### Acceptance Criteria

1. WHEN a value object is used exclusively by a single aggregate THEN the system SHALL place that value object within the aggregate's package
2. WHEN a value object is used by multiple aggregates THEN the system SHALL place that value object in the shared package
3. WHEN examining the generation package THEN the system SHALL contain only value objects directly related to report generation behavior
4. WHEN examining the shared package THEN the system SHALL contain only value objects representing cross-cutting domain concepts
5. WHERE value objects exist in the generation package THEN the system SHALL ensure they are not duplicated in the shared package

### Requirement 2

**User Story:** As a developer, I want aggregate-specific value objects co-located with their aggregate, so that I can understand the aggregate's complete behavior without navigating to distant packages.

#### Acceptance Criteria

1. WHEN CalculatedExposure is used only by GeneratedReport THEN the system SHALL keep CalculatedExposure in the generation package
2. WHEN ConcentrationIndices is used only by GeneratedReport THEN the system SHALL keep ConcentrationIndices in the generation package
3. WHEN GeographicBreakdown is used only by GeneratedReport THEN the system SHALL keep GeographicBreakdown in the generation package
4. WHEN SectorBreakdown is used only by GeneratedReport THEN the system SHALL keep SectorBreakdown in the generation package
5. WHEN ValidationResult is used only by report generation THEN the system SHALL keep ValidationResult in the generation package

### Requirement 3

**User Story:** As a developer, I want truly shared value objects in a dedicated shared package, so that I can reuse common domain concepts across aggregates without creating dependencies between aggregates.

#### Acceptance Criteria

1. WHEN ReportId is used across multiple contexts THEN the system SHALL keep ReportId in the shared valueobjects package
2. WHEN BatchId is used across multiple contexts THEN the system SHALL keep BatchId in the shared valueobjects package
3. WHEN BankId is used across multiple contexts THEN the system SHALL keep BankId in the shared valueobjects package
4. WHEN ReportingDate is used across multiple contexts THEN the system SHALL keep ReportingDate in the shared valueobjects package
5. WHEN ReportStatus is used across multiple contexts THEN the system SHALL keep ReportStatus in the shared valueobjects package

### Requirement 4

**User Story:** As a developer, I want clear guidelines for placing new value objects, so that I can maintain consistency as the domain evolves.

#### Acceptance Criteria

1. WHEN creating a new value object THEN the system SHALL provide documentation explaining placement rules
2. WHEN a value object is used by only one aggregate THEN the documentation SHALL specify placing it in the aggregate package
3. WHEN a value object is used by multiple aggregates THEN the documentation SHALL specify placing it in shared/valueobjects
4. WHEN a value object's usage changes THEN the documentation SHALL specify when to move it between packages
5. WHEN examining the domain structure THEN the documentation SHALL explain the rationale for the organization

### Requirement 5

**User Story:** As a developer, I want the reorganization to maintain all existing business logic and validation rules, so that the refactoring is safe and doesn't introduce bugs.

#### Acceptance Criteria

1. WHEN moving value objects THEN the system SHALL preserve all validation logic
2. WHEN moving value objects THEN the system SHALL preserve all business methods
3. WHEN moving value objects THEN the system SHALL preserve all immutability guarantees
4. WHEN moving value objects THEN the system SHALL update all import statements correctly
5. WHEN the reorganization is complete THEN the system SHALL compile without errors
