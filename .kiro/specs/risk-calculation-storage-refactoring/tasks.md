# Implementation Plan

- [x] 1. Add calculation_results_uri column to batches table





  - Create Flyway migration to add calculation_results_uri VARCHAR(500) column to batches table
  - Add index on calculation_results_uri for URI lookups
  - _Requirements: 2.1, 2.5_

- [x] 2. Enhance BatchRepository with URI management methods





  - Add updateCalculationResultsUri(String batchId, String uri) method
  - Add getCalculationResultsUri(String batchId) method returning Optional<String>
  - Add markAsCompleted(String batchId, String resultsUri, Instant processedAt) method
  - Update BatchEntity to include calculationResultsUri field
  - Update JpaBatchRepository implementation
  - _Requirements: 2.1, 2.5_

- [ ] 3. Implement CalculationResultsJsonSerializer
  - Create CalculationResultsJsonSerializer class in application layer
  - Implement serialize(RiskCalculationResult) method returning JSON string
  - Include format_version field in JSON output
  - Include batch_id, calculated_at, bank_info in JSON
  - Include summary section with totals and breakdowns
  - Include calculated_exposures array with complete details
  - Implement deserialize(String json) method returning RiskCalculationResult
  - Handle CalculationResultsSerializationException for serialization errors
  - Handle CalculationResultsDeserializationException for deserialization errors
  - _Requirements: 1.1, 1.2, 10.1, 10.2, 10.3, 10.4_

- [ ]* 3.1 Write property test for JSON serialization round-trip
  - **Property 7: File Retrieval Round-Trip**
  - **Validates: Requirements 4.3**

- [ ]* 3.2 Write property test for format version presence
  - **Property 14: Format Version Presence**
  - **Validates: Requirements 10.1**

- [ ] 4. Create ICalculationResultsStorageService interface and implementation
  - Create ICalculationResultsStorageService interface in domain layer
  - Add storeCalculationResults(RiskCalculationResult) method returning Result<String>
  - Add retrieveCalculationResults(String batchId) method returning Result<RiskCalculationResult>
  - Add retrieveCalculationResultsRaw(String batchId) method returning Result<JsonNode>
  - Create CalculationResultsStorageServiceImpl in infrastructure layer
  - Inject CalculationResultsJsonSerializer and IFileStorageService
  - Inject BatchRepository for URI lookup
  - Generate file names as risk_calc_{batchId}_{timestamp}.json
  - _Requirements: 1.3, 1.4, 4.3, 8.3_

- [ ]* 4.1 Write property test for storage URI validity
  - **Property 2: Storage URI Validity**
  - **Validates: Requirements 1.4**

- [ ]* 4.2 Write property test for historical data retrieval
  - **Property 10: Historical Data Retrieval**
  - **Validates: Requirements 8.3**

- [ ] 5. Update CalculateRiskMetricsCommandHandler to use file storage
  - Inject ICalculationResultsStorageService
  - After calculation completes, call storeCalculationResults()
  - Store returned URI in batch metadata via BatchRepository
  - Update batch status to COMPLETED with URI
  - Remove calls to ExposureRepository.saveAll()
  - Remove calls to MitigationRepository.saveAll()
  - _Requirements: 1.3, 1.5, 2.3, 2.4, 2.5_

- [ ]* 5.1 Write property test for batch completion state
  - **Property 5: Batch Completion State**
  - **Validates: Requirements 2.5**

- [ ]* 5.2 Write property test for database exposure exclusion
  - **Property 3: Database Exposure Exclusion**
  - **Validates: Requirements 2.3**

- [ ]* 5.3 Write property test for database mitigation exclusion
  - **Property 4: Database Mitigation Exclusion**
  - **Validates: Requirements 2.4**

- [ ] 6. Update BatchCalculationCompletedEvent to include URI
  - Add calculationResultsUri field to BatchCalculationCompletedEvent
  - Update RiskCalculationEventPublisher to include URI in event
  - _Requirements: 4.1_

- [ ]* 6.1 Write property test for event URI inclusion
  - **Property 6: Event URI Inclusion**
  - **Validates: Requirements 4.1**

- [ ] 7. Deprecate ExposureRepository database persistence methods
  - Mark save() and saveAll() methods as @Deprecated in ExposureRepository
  - Add loadFromJson(String jsonContent) method to ExposureRepository
  - Update JpaExposureRepository to implement loadFromJson()
  - Add deprecation warnings in method documentation
  - _Requirements: 5.1, 5.5_

- [ ] 8. Deprecate MitigationRepository database persistence methods
  - Mark save() and saveAll() methods as @Deprecated in MitigationRepository
  - Add loadFromJson(String jsonContent) method to MitigationRepository
  - Update JpaMitigationRepository to implement loadFromJson()
  - Add deprecation warnings in method documentation
  - _Requirements: 5.2, 5.5_

- [ ] 9. Update ExposureQueryService to use JSON file retrieval
  - Inject ICalculationResultsStorageService
  - Update findByBatchId() to retrieve JSON file and parse exposures
  - Ensure no database queries to exposures table
  - _Requirements: 5.5, 9.3_

- [ ]* 9.1 Write property test for JSON-based exposure retrieval
  - **Property 8: JSON-Based Exposure Retrieval**
  - **Validates: Requirements 5.5**

- [ ]* 9.2 Write property test for detailed query file-based access
  - **Property 13: Detailed Query File-Based**
  - **Validates: Requirements 9.3**

- [ ] 10. Verify status queries use database only
  - Review BatchStatusQueryService implementation
  - Ensure no file I/O operations during status queries
  - Verify queries use only database metadata
  - _Requirements: 9.1_

- [ ]* 10.1 Write property test for status query database-only access
  - **Property 11: Status Query Database-Only**
  - **Validates: Requirements 9.1**

- [ ] 11. Verify summary queries use database only (when enabled)
  - Review PortfolioAnalysisQueryService implementation
  - Ensure summary queries use portfolio_analysis table
  - Verify no file access for summary queries
  - _Requirements: 9.2_

- [ ]* 11.1 Write property test for summary query database-only access
  - **Property 12: Summary Query Database-Only**
  - **Validates: Requirements 9.2**

- [ ] 12. Implement error handling for file storage operations
  - Create CalculationResultsSerializationException
  - Create CalculationResultsDeserializationException
  - Ensure FileStorageException is thrown on upload failures
  - Ensure FileNotFoundException is thrown on download failures
  - Update CalculateRiskMetricsCommandHandler to catch storage errors
  - Publish BatchCalculationFailedEvent on storage errors
  - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5_

- [ ]* 12.1 Write property test for error event publishing
  - **Property 15: Error Event Publishing**
  - **Validates: Requirements 7.5**

- [ ] 13. Implement JSON file immutability checks
  - Update ICalculationResultsStorageService to prevent overwrites
  - Check if file exists before storing
  - Throw exception or create versioned file if batch_id already has results
  - _Requirements: 8.1, 8.4_

- [ ]* 13.1 Write property test for JSON file immutability
  - **Property 9: JSON File Immutability**
  - **Validates: Requirements 8.1, 8.4**

- [ ] 14. Update integration tests for file-first architecture
  - Update IngestionToRiskCalculationIntegrationTest to verify JSON file creation
  - Verify batch metadata includes calculation_results_uri
  - Verify exposures and mitigations tables remain empty
  - Test end-to-end flow with file storage
  - _Requirements: 1.5, 2.3, 2.4, 2.5_

- [ ] 15. Add database migration documentation
  - Document the migration strategy for existing batches
  - Explain how to handle mixed state (some batches in DB, some in files)
  - Document table deprecation timeline
  - Add notes about dropping exposures and mitigations tables
  - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5_

- [ ] 16. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.
