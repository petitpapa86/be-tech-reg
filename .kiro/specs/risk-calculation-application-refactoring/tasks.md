# Risk Calculation Application Layer Refactoring - Implementation Tasks

## Phase 1: Critical Compilation Fixes

- [x] 1. Integrate PerformanceMetrics in CalculateRiskMetricsCommandHandler





  - Add PerformanceMetrics as constructor dependency
  - Call recordBatchStart() at method entry
  - Call recordBatchSuccess() before returning success
  - Call recordBatchFailure() in catch block
  - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5_

- [x] 2. Fix Result API usage across application layer





  - Update CalculateRiskMetricsCommandHandler Result.failure() calls
  - Update BatchIngestedEventListener error handling
  - Update CalculationResultsJsonSerializer error cases
  - Update PerformanceMonitoringScheduler error handling
  - Ensure all ErrorDetail objects have proper error codes, types, and context keys
  - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5_

- [x] 3. Fix record accessor usage in CalculationResultsJsonSerializer





  - Replace result.getBatchId() with result.batchId()
  - Replace result.getBankInfo() with result.bankInfo()
  - Replace result.getAnalysis() with result.analysis()
  - Fix domain object accessor calls (Share, Breakdown, etc.)
  - _Requirements: 4.1, 4.2, 4.3, 4.4_



- [x] 4. Fix repository method calls in BatchIngestedEventListener





  - Replace existsByBatchId() with existsById()
  - Wrap String batchId in BatchId value object
  - Update any other repository usage
  - _Requirements: 3.2_

- [x] 5. Verify compilation success





  - Run `mvn clean compile` on application layer
  - Fix any remaining compilation errors
  - Verify all imports are resolved
  - _Requirements: 1.1, 1.2, 1.3, 1.4_

## Phase 2: Event System and Domain Integration

- [x] 6. Verify event publisher method signatures




  - Confirm publishBatchCalculationCompleted() accepts correct parameters
  - Confirm publishBatchCalculationFailed() accepts correct parameters
  - Verify domain event creation logic
  - _Requirements: 5.1, 5.2_

- [ ] 7. Verify domain service integration


  - Confirm IFileStorageService.retrieveFile() is called correctly
  - Verify FileStorageUri parameter usage
  - Check domain event publishing flow
  - _Requirements: 3.1, 3.3, 3.4_

## Phase 3: Testing and Validation

- [ ]* 8. Update unit tests for CalculateRiskMetricsCommandHandler
  - Add PerformanceMetrics mock
  - Verify recordBatchStart() is called
  - Verify recordBatchSuccess() is called with correct exposure count
  - Verify recordBatchFailure() is called on errors
  - Update Result.failure() assertions to check ErrorDetail
  - _Requirements: 2.1, 2.2, 6.1, 6.2, 6.3_

- [ ]* 9. Update unit tests for CalculationResultsJsonSerializer
  - Fix record accessor usage in test code
  - Update test assertions for record accessors
  - Verify JSON serialization works correctly
  - _Requirements: 4.1, 4.2, 4.3_

- [ ]* 10. Update unit tests for BatchIngestedEventListener
  - Update repository mock method signatures
  - Verify BatchId value object usage
  - Test error handling with ErrorDetail
  - _Requirements: 3.2, 4.4_

- [ ]* 11. Run integration tests
  - Execute CalculateRiskMetricsCommandHandlerIntegrationTest
  - Verify performance metrics are recorded
  - Verify end-to-end calculation flow
  - Check event publishing works correctly
  - _Requirements: 1.1, 5.3, 5.4, 6.2_

- [ ] 12. Final verification
  - Run full Maven build: `mvn clean install`
  - Verify all tests pass
  - Check build completes in under 2 minutes
  - Confirm zero compilation errors
  - _Requirements: 1.1, 1.2_

## Task Dependencies

```
Task 1 (PerformanceMetrics) ──┐
Task 2 (Result API)          ──┼──> Task 5 (Compilation) ──> Task 8-10 (Unit Tests) ──> Task 11 (Integration) ──> Task 12 (Final)
Task 3 (Record Accessors)    ──┤                                                                                              
Task 4 (Repository)          ──┘                                                                              
                                └──> Task 6-7 (Event/Domain) ────────────────────────────────────────────────┘
```

## Success Metrics

### Completion Criteria
- [ ] Zero compilation errors in application layer
- [ ] All unit tests pass (>95% success rate)
- [ ] Integration tests demonstrate proper module interaction
- [ ] Maven build completes in <2 minutes
- [ ] Performance metrics correctly track batch processing

### Quality Gates
1. **Compilation Gate**: All code compiles without errors
2. **Test Gate**: All tests pass with >95% success rate
3. **Integration Gate**: End-to-end scenarios work correctly
4. **Performance Gate**: Metrics tracking works as expected

## Notes

- Focus on one task at a time
- Verify compilation after each major change
- Run tests frequently to catch issues early
- Keep changes minimal and focused on fixing specific issues
- Maintain clean architecture boundaries throughout
