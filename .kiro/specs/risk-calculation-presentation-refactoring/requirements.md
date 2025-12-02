# Risk Calculation Presentation Layer Refactoring - Requirements

## Introduction

The risk calculation module underwent a major refactoring to adopt bounded context architecture (ExposureRecording, Valuation, Protection, Classification, Analysis). However, the presentation layer was not updated and still contains old DTOs and controllers that don't align with the new domain model. This creates technical debt and makes the API inconsistent with the underlying architecture.

## Glossary

- **DTO (Data Transfer Object)**: An object that carries data between processes, specifically between the presentation and application layers
- **Bounded Context**: A central pattern in Domain-Driven Design defining explicit boundaries within which a domain model is defined
- **Presentation Layer**: The layer responsible for handling HTTP requests, routing, and data transformation for external consumers
- **Controller**: A component that handles HTTP requests and delegates to application services
- **Route**: A mapping between HTTP endpoints and controller methods

## Requirements

### Requirement 1: DTO Alignment with New Domain Model

**User Story:** As a developer, I want DTOs that align with the new bounded context architecture, so that the API reflects the current domain model.

#### Acceptance Criteria

1.1. WHEN the presentation layer defines DTOs THEN they SHALL map to the new bounded context domain objects (ExposureRecording, ClassifiedExposure, ProtectedExposure, PortfolioAnalysis)

1.2. WHEN old DTOs exist (RiskReportDTO, ExposureDTO, CreditRiskMitigationDTO, BankInfoDTO) THEN the system SHALL replace them with DTOs aligned to bounded contexts

1.3. WHEN new DTOs are created THEN they SHALL use clear naming that reflects their bounded context (e.g., ExposureRecordingDTO, ClassifiedExposureDTO)

1.4. WHEN DTOs are defined THEN they SHALL include proper Jackson annotations for JSON serialization

1.5. WHEN DTOs contain monetary amounts THEN they SHALL use BigDecimal with proper precision

### Requirement 2: Controller Refactoring

**User Story:** As a developer, I want controllers that use the refactored application layer, so that the presentation layer is consistent with the new architecture.

#### Acceptance Criteria

2.1. WHEN controllers exist THEN they SHALL delegate to the new application layer command handlers (CalculateRiskMetricsCommandHandler)

2.2. WHEN old controller methods exist THEN the system SHALL remove or update them to use new application services

2.3. WHEN controllers handle requests THEN they SHALL transform DTOs to application layer commands

2.4. WHEN controllers return responses THEN they SHALL transform application layer results to DTOs

2.5. WHEN controllers encounter errors THEN they SHALL use proper HTTP status codes and error responses

### Requirement 3: API Endpoint Consistency

**User Story:** As an API consumer, I want consistent API endpoints that reflect the current system capabilities, so that I can integrate reliably.

#### Acceptance Criteria

3.1. WHEN API endpoints are defined THEN they SHALL follow RESTful conventions

3.2. WHEN endpoints accept input THEN they SHALL validate request bodies using the new DTOs

3.3. WHEN endpoints return data THEN they SHALL use the new response DTOs

3.4. WHEN endpoints are versioned THEN they SHALL maintain backward compatibility or provide clear migration paths

3.5. WHEN API documentation exists THEN it SHALL reflect the current endpoint structure

### Requirement 4: Mapper Implementation

**User Story:** As a developer, I want clear mappers between DTOs and domain objects, so that data transformation is explicit and maintainable.

#### Acceptance Criteria

4.1. WHEN DTOs need conversion to domain objects THEN the system SHALL provide dedicated mapper classes

4.2. WHEN domain objects need conversion to DTOs THEN the system SHALL use the same mapper classes

4.3. WHEN mappers perform conversion THEN they SHALL handle null values safely

4.4. WHEN mappers encounter invalid data THEN they SHALL throw descriptive exceptions

4.5. WHEN mappers are implemented THEN they SHALL be stateless and reusable

### Requirement 5: Status and Monitoring Endpoints

**User Story:** As a system administrator, I want status and monitoring endpoints that provide accurate information about risk calculations, so that I can monitor system health.

#### Acceptance Criteria

5.1. WHEN status endpoints exist (BatchSummaryStatusController) THEN they SHALL use the new domain model

5.2. WHEN monitoring endpoints exist (RiskCalculationHealthController) THEN they SHALL check the health of new components

5.3. WHEN metrics are collected (RiskCalculationMetricsCollector) THEN they SHALL reflect the new architecture

5.4. WHEN health checks run THEN they SHALL verify connectivity to required services (database, storage, currency API)

5.5. WHEN metrics are exposed THEN they SHALL include performance data from the new bounded contexts

### Requirement 6: Route Configuration

**User Story:** As a developer, I want clear route configuration that maps endpoints to controllers, so that the API structure is explicit and maintainable.

#### Acceptance Criteria

6.1. WHEN routes are defined THEN they SHALL use the IEndpoint interface pattern

6.2. WHEN routes are registered THEN they SHALL be organized by functional area (calculation, status, monitoring)

6.3. WHEN routes require authentication THEN they SHALL specify security requirements

6.4. WHEN routes are documented THEN they SHALL include OpenAPI/Swagger annotations

6.5. WHEN routes change THEN they SHALL maintain backward compatibility or version appropriately

### Requirement 7: Error Handling

**User Story:** As an API consumer, I want consistent error responses, so that I can handle failures appropriately.

#### Acceptance Criteria

7.1. WHEN validation errors occur THEN the system SHALL return 400 Bad Request with detailed error messages

7.2. WHEN business logic errors occur THEN the system SHALL return appropriate 4xx status codes

7.3. WHEN system errors occur THEN the system SHALL return 500 Internal Server Error without exposing internal details

7.4. WHEN errors are returned THEN they SHALL include error codes, messages, and timestamps

7.5. WHEN errors are logged THEN they SHALL include sufficient context for debugging

### Requirement 8: Testing

**User Story:** As a developer, I want comprehensive tests for the presentation layer, so that I can ensure API reliability.

#### Acceptance Criteria

8.1. WHEN controllers are implemented THEN they SHALL have unit tests

8.2. WHEN DTOs are defined THEN they SHALL have serialization/deserialization tests

8.3. WHEN mappers are implemented THEN they SHALL have conversion tests

8.4. WHEN endpoints are exposed THEN they SHALL have integration tests

8.5. WHEN error scenarios exist THEN they SHALL have dedicated test cases

## Success Criteria

- All old DTOs are replaced with new bounded context-aligned DTOs
- Controllers use the refactored application layer
- API endpoints are consistent and well-documented
- Mappers provide clear transformation between layers
- Status and monitoring endpoints reflect the new architecture
- All tests pass with >80% code coverage
- No compilation errors or warnings

## Non-Functional Requirements

- API response times should remain under 200ms for status endpoints
- Error responses should be consistent across all endpoints
- DTOs should be backward compatible where possible
- Code should follow existing presentation layer patterns from other modules
