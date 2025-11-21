# Requirements Document

## Introduction

The report generation module's application layer is currently organized by technical layers (orchestration, aggregation, recommendations, coordination, events) rather than by business capabilities. This violates the Screaming Architecture principle where the structure should communicate what the system does, not how it's built. This reorganization will restructure the application layer to be organized by business capabilities, making the codebase more maintainable and easier to understand.

## Glossary

- **Application Layer**: The layer containing use cases and application services that orchestrate domain logic
- **Business Capability**: A cohesive set of functionality that delivers business value (e.g., report generation, event coordination)
- **Screaming Architecture**: An architectural principle where the folder structure immediately communicates the system's purpose
- **Technical Layer**: Organization by technical concerns (controllers, services, repositories) rather than business capabilities
- **Comprehensive Report**: A report that combines risk calculation results, data quality metrics, and recommendations
- **Report Generation Module**: The bounded context responsible for generating regulatory reports

## Requirements

### Requirement 1

**User Story:** As a developer, I want the application layer organized by business capabilities, so that I can quickly find and understand the code related to specific features.

#### Acceptance Criteria

1. WHEN a developer navigates the application layer THEN the folder structure SHALL reflect business capabilities rather than technical layers
2. WHEN examining package names THEN each package SHALL represent a distinct business capability (e.g., generation, coordination, integration)
3. WHEN looking for report generation logic THEN all related classes SHALL be grouped together in a single capability package
4. WHEN looking for event coordination logic THEN all related classes SHALL be grouped together in a single capability package
5. WHEN examining the structure THEN technical terms like "orchestration", "aggregation", "recommendations" SHALL be replaced with business capability names

### Requirement 2

**User Story:** As a developer, I want clear separation between different business capabilities, so that changes to one capability don't unexpectedly affect others.

#### Acceptance Criteria

1. WHEN modifying report generation logic THEN the changes SHALL be isolated to the generation capability package
2. WHEN modifying event coordination logic THEN the changes SHALL be isolated to the coordination capability package
3. WHEN adding new functionality THEN the system SHALL provide clear guidance on which capability package to use
4. WHEN reviewing dependencies THEN each capability package SHALL have minimal coupling to other capability packages
5. WHEN examining cross-capability communication THEN it SHALL occur through well-defined interfaces or events

### Requirement 3

**User Story:** As a new team member, I want the codebase structure to be self-documenting, so that I can understand the system's capabilities without extensive documentation.

#### Acceptance Criteria

1. WHEN a new developer views the application layer THEN the package names SHALL clearly communicate what the system does
2. WHEN exploring a capability package THEN all related commands, handlers, services, and DTOs SHALL be co-located
3. WHEN looking for integration event handling THEN it SHALL be clearly separated from internal event handling
4. WHEN examining the structure THEN it SHALL follow the same organizational pattern as other modules (risk-calculation, data-quality)
5. WHEN comparing with other modules THEN the organizational approach SHALL be consistent across the codebase

### Requirement 4

**User Story:** As a developer, I want the refactoring to maintain all existing functionality, so that the system continues to work correctly after reorganization.

#### Acceptance Criteria

1. WHEN the reorganization is complete THEN all existing tests SHALL pass without modification
2. WHEN classes are moved THEN all import statements SHALL be updated correctly
3. WHEN Spring beans are relocated THEN the dependency injection SHALL continue to work correctly
4. WHEN the application starts THEN all components SHALL be properly wired and functional
5. WHEN integration events are published THEN they SHALL be received and processed correctly
