# Risk Calculation Application Layer Refactoring - Requirements

## Introduction

This specification addresses the systematic refactoring of the Risk Calculation module's application layer to resolve compilation errors and align with the updated domain model. The domain layer has been successfully implemented with proper value objects, domain services, and repository interfaces. However, the application layer requires updates to work correctly with the new domain structure.

## Glossary

- **Application Layer**: The orchestration layer that coordinates domain objects and services to fulfill use cases
- **Domain Layer**: The core business logic layer containing entities, value objects, and domain services
- **Result API**: The functional error handling pattern using Result<T> with ErrorDetail
- **Record Accessor**: Java record methods that access fields (e.g., `batchId()` instead of `getBatchId()`)
- **Event Publisher**: Component responsible for publishing domain and integration events
- **Repository Interface**: Domain-defined contract for data persistence operations

## Requirements

### Requirement 1: Compilation Error Resolution

**User Story:** As a developer, I want all compilation errors in the application layer resolved, so that the Risk Calculation module can be built and deployed successfully.

#### Acceptance Criteria

1. WHEN the application layer is compiled THEN the system SHALL complete compilation without errors
2. WHEN Maven build is executed THEN the system SHALL complete successfully with `mvn clean compile`
3. WHEN imports are resolved THEN the system SHALL have no missing or unresolved symbols
4. WHEN method signatures are checked THEN the system SHALL have matching signatures between interfaces and implementations

### Requirement 2: Error Handling Consistency

**User Story:** As a developer, I want all Result.failure() calls to use the new ErrorDetail API, so that error handling is consistent across the application.

#### Acceptance Criteria

1. WHEN creating error results THEN the system SHALL use Result.failure(ErrorDetail) instead of Result.failure(String)
2. WHEN ErrorDetail objects are created THEN the system SHALL include proper error codes, types, and messages
3. WHEN errors occur THEN the system SHALL follow established error handling patterns from other modules
4. WHEN error codes are assigned THEN the system SHALL use consistent naming conventions (e.g., "VALIDATION_ERROR", "SYSTEM_ERROR")
5. WHEN error context is provided THEN the system SHALL include localization keys following the pattern "module.operation.error.type"

### Requirement 3: Domain Service Integration

**User Story:** As a developer, I want application services to correctly use domain service interfaces, so that the application layer properly integrates with the domain layer.

#### Acceptance Criteria

1. WHEN calling file storage service THEN the system SHALL use correct method names (retrieveFile vs downloadFileContent)
2. WHEN accessing repositories THEN the system SHALL use correctly implemented repository interfaces
3. WHEN domain events are created THEN the system SHALL properly structure and publish them
4. WHEN domain services are invoked THEN the system SHALL pass correct parameter types

### Requirement 4: Record Accessor Compliance

**User Story:** As a developer, I want code using record classes to call the correct accessor methods, so that data access is consistent with Java record conventions.

#### Acceptance Criteria

1. WHEN accessing RiskCalculationResult fields THEN the system SHALL use record accessors (batchId(), bankInfo(), etc.)
2. WHEN accessing any record field THEN the system SHALL follow Java record naming conventions
3. WHEN compiling record access code THEN the system SHALL have no errors related to missing methods on records
4. WHEN accessing domain object fields THEN the system SHALL use appropriate accessor methods (amount() for records, getAmount() for classes)

### Requirement 5: Event Publishing Correctness

**User Story:** As a developer, I want event publishers to have correct method signatures, so that domain events can be published successfully.

#### Acceptance Criteria

1. WHEN event publisher methods are called THEN the system SHALL accept the correct parameter types
2. WHEN domain events are created THEN the system SHALL populate proper field values
3. WHEN integration events are mapped THEN the system SHALL correctly transform from domain events
4. WHEN events are published THEN the system SHALL successfully deliver them to subscribers

### Requirement 6: Performance Metrics Integration

**User Story:** As a developer, I want the CalculateRiskMetricsCommandHandler to track performance metrics, so that batch processing performance can be monitored and analyzed.

#### Acceptance Criteria

1. WHEN a batch calculation starts THEN the system SHALL call PerformanceMetrics.recordBatchStart()
2. WHEN a batch calculation succeeds THEN the system SHALL call PerformanceMetrics.recordBatchSuccess() with the exposure count
3. WHEN a batch calculation fails THEN the system SHALL call PerformanceMetrics.recordBatchFailure() with the error message
4. WHEN performance metrics are recorded THEN the system SHALL use the correct batch ID for tracking
5. WHEN the command handler is instantiated THEN the system SHALL inject PerformanceMetrics as a dependency

## Technical Constraints

### TC-1: Backward Compatibility
- Existing event contracts with other modules must be maintained
- Integration event structures cannot change without coordination

### TC-2: Clean Architecture
- Application layer must not contain business logic
- Domain boundaries must be respected
- Dependencies must flow inward (application → domain, never domain → application)

### TC-3: Testing Requirements
- All refactored code must have corresponding tests
- Integration tests must pass after refactoring
- No reduction in test coverage

## Success Criteria

1. Zero compilation errors in the application layer
2. Maven build completes successfully in under 2 minutes
3. All unit tests pass with >95% success rate
4. Integration tests demonstrate proper module interaction
5. Event publishing works correctly with other modules
6. Code follows established architectural patterns
