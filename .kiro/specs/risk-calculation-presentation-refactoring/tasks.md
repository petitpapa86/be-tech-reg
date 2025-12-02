# Risk Calculation Presentation Layer Refactoring - Implementation Plan

## Overview
This implementation plan refactors the risk calculation presentation layer to align with the new bounded context architecture. The plan focuses on creating new DTOs, controllers, mappers, and routes that reflect the domain model while maintaining API consistency.

---

## Phase 1: DTO Layer Implementation

- [x] 1. Create new response DTOs aligned with bounded contexts





  - Create PortfolioAnalysisResponseDTO with processing state, concentration indices, breakdowns, and timestamps
  - Create ClassifiedExposureDTO with exposure details, EUR amounts, and economic sector classification
  - Create ProtectedExposureDTO with gross/net exposure and applied mitigations
  - Create ConcentrationIndicesDTO with HHI values and risk levels
  - Create BreakdownDTO for geographic and sector breakdowns
  - Create ProcessingStateDTO for calculation status tracking
  - Create ProcessingProgressDTO for detailed progress information
  - Create ProcessingTimestampsDTO for temporal tracking
  - Create MitigationDTO for credit risk mitigation details
  - Create ExposureClassificationDTO for classification metadata
  - Add Jackson annotations for JSON serialization
  - Use BigDecimal for all monetary amounts
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_

- [x] 2. Create status response DTOs




  - Create BatchStatusResponseDTO with processing state and available results
  - Create PagedResponse<T> generic DTO for paginated results
  - Add proper validation annotations
  - _Requirements: 1.1, 1.3, 1.4_

- [ ]* 2.1 Write unit tests for DTO serialization
  - Test JSON serialization/deserialization for all DTOs
  - Test null handling in DTOs
  - Test BigDecimal precision in monetary fields
  - _Requirements: 8.2_

---

## Phase 2: Mapper Layer Implementation

- [x] 3. Implement domain-to-DTO mappers





  - Create PortfolioAnalysisMapper with toResponseDTO() and helper methods
  - Create ExposureMapper with toClassifiedDTO() and toProtectedDTO()
  - Create StatusMapper for batch status transformations
  - Implement null-safe conversion logic
  - Add descriptive exception handling for invalid data
  - Make all mappers stateless with @Component annotation
  - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5_

- [ ]* 3.1 Write unit tests for mappers
  - Test domain-to-DTO conversions
  - Test null value handling
  - Test invalid data scenarios
  - Test edge cases (empty lists, zero values, etc.)
  - _Requirements: 8.3_

---

## Phase 3: Query Service Layer

- [x] 4. Implement query services for data retrieval





  - Create PortfolioAnalysisQueryService with methods for portfolio analysis, concentration indices, and breakdowns
  - Create ExposureQueryService with paginated methods for classified and protected exposures
  - Create BatchStatusQueryService for status and progress queries
  - Use @Transactional(readOnly = true) for all query methods
  - Inject required repositories and mappers
  - _Requirements: 2.1, 2.3, 2.4_

- [ ]* 4.1 Write unit tests for query services
  - Test service methods with mocked repositories
  - Test pagination logic
  - Test filtering logic (e.g., by economic sector)
  - Test empty result scenarios
  - _Requirements: 8.1_

---

## Phase 4: Controller Implementation

- [x] 5. Create PortfolioAnalysisController





  - Implement GET /{batchId} endpoint for full portfolio analysis
  - Implement GET /{batchId}/concentrations for concentration indices
  - Implement GET /{batchId}/breakdowns with optional type filter
  - Extend BaseController for consistent error handling
  - Add @RestController and @RequestMapping annotations
  - Inject PortfolioAnalysisQueryService
  - _Requirements: 2.1, 2.2, 2.3, 2.4, 3.1, 3.2, 3.3_

- [x] 6. Create ExposureResultsController





  - Implement GET /{batchId}/classified with pagination and sector filtering
  - Implement GET /{batchId}/protected with pagination
  - Use PagedResponse<T> for results
  - Add request parameter validation
  - _Requirements: 2.1, 2.2, 2.3, 2.4, 3.1, 3.2, 3.3_

- [x] 7. Enhance BatchStatusController





  - Implement GET /{batchId}/status for batch status
  - Implement GET /{batchId}/progress for processing progress
  - Implement GET /active for active batches with optional bank filter
  - Update to use new DTOs and query services
  - _Requirements: 2.1, 2.2, 5.1_

- [ ]* 7.1 Write unit tests for controllers
  - Test controller methods with mocked services
  - Test request parameter validation
  - Test response transformation
  - Test pagination parameters
  - _Requirements: 8.1_

---

## Phase 5: Route Configuration

- [x] 8. Implement route configurations




  - Create PortfolioAnalysisRoutes implementing IEndpoint
  - Create ExposureResultsRoutes implementing IEndpoint
  - Update BatchStatusRoutes if needed
  - Configure security requirements (RISK_CALCULATION_READ permission)
  - Register routes in RouterFunction.Builder
  - _Requirements: 6.1, 6.2, 6.3_

---

## Phase 6: Error Handling

- [x] 10. Implement custom exceptions and error handlers





  - Create BatchNotFoundException for missing batches
  - Create CalculationNotCompletedException for incomplete calculations
  - Create RiskCalculationErrorHandler with @ControllerAdvice
  - Map exceptions to appropriate HTTP status codes (404, 202, 400, 500)
  - Include error codes, messages, and timestamps in responses
  - Add logging with sufficient context
  - _Requirements: 2.5, 7.1, 7.2, 7.3, 7.4, 7.5_

- [ ]* 10.1 Write unit tests for error handling
  - Test exception mapping to HTTP status codes
  - Test error response format
  - Test logging behavior
  - _Requirements: 8.5_

---

## Phase 7: Monitoring and Health Checks

- [x] 11. Update health checker and metrics collector




  - Update RiskCalculationHealthChecker to verify new repositories and services
  - Update RiskCalculationMetricsCollector to track new bounded context metrics
  - Add metrics for portfolio analysis, exposures processed, and calculation times
  - Verify connectivity to database, storage, and currency API
  - _Requirements: 5.2, 5.3, 5.4, 5.5_

- [ ]* 11.1 Write unit tests for monitoring components
  - Test health check logic
  - Test metrics collection
  - Test failure scenarios
  - _Requirements: 8.1_

---

## Phase 8: Integration Testing

- [ ]* 12. Write integration tests for API endpoints
  - Test end-to-end flows for portfolio analysis retrieval
  - Test end-to-end flows for exposure queries
  - Test end-to-end flows for batch status
  - Test pagination and filtering
  - Test error scenarios (not found, incomplete, etc.)
  - Test security/authorization
  - _Requirements: 8.4_

---

## Phase 9: Cleanup and Documentation

- [x] 13. Remove old DTOs





  - Delete RiskReportDTO, ExposureDTO, CreditRiskMitigationDTO, BankInfoDTO
  - Update any remaining usages to new DTOs
  - Remove any controllers or methods that depend on old DTOs
  - _Requirements: 1.2_

- [x] 14. Create migration documentation





  - Document API changes and new endpoints
  - Provide mapping from old to new DTOs
  - Include code examples for common use cases
  - Document breaking changes and migration timeline
  - _Requirements: 3.4, 3.5_

---

## Phase 10: Final Validation

- [ ] 15. Final checkpoint - Ensure all tests pass
  - Run all unit tests
  - Run all integration tests
  - Verify code coverage >80%
  - Check for compilation errors or warnings
  - Validate API response times <200ms for status endpoints
  - _Requirements: All_

