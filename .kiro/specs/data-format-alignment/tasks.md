# Implementation Plan: Data Format Alignment

- [x] 1. Create shared DTOs in Core module





  - Create package `com.bcbs239.regtech.core.domain.shared.dto` in Core domain module
  - Create `BankInfoDTO` record with Jackson annotations for snake_case JSON fields
  - Create `ExposureDTO` record with all 11 fields and Jackson annotations
  - Create `CreditRiskMitigationDTO` record with Jackson annotations
  - Create `BatchDataDTO` record composing the three DTOs above
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_

- [ ]* 1.1 Write unit tests for DTO serialization
  - Test that BankInfoDTO serializes to JSON with snake_case field names
  - Test that ExposureDTO serializes with all 11 fields
  - Test that CreditRiskMitigationDTO serializes correctly
  - Test that BatchDataDTO produces correct JSON structure
  - _Requirements: 1.2, 1.3, 1.4, 1.5_

- [ ]* 1.2 Write property test for JSON field naming
  - **Property 4: JSON Serialization Uses Snake Case**
  - **Validates: Requirements 6.1**

- [ ]* 1.3 Write property test for deserialization flexibility
  - **Property 5: JSON Deserialization Supports Both Naming Conventions**
  - **Validates: Requirements 6.2**

- [x] 2. Add conversion methods to Ingestion domain models





  - Add `BankInfo` value object to Ingestion domain if not exists
  - Add `toDTO()` method to `BankInfo` returning `BankInfoDTO`
  - Add `fromDTO(BankInfoDTO)` static factory method to `BankInfo`
  - Add `toDTO()` method to `LoanExposure` returning `ExposureDTO`
  - Add `fromDTO(ExposureDTO)` static factory method to `LoanExposure`
  - Add `toDTO()` method to `CreditRiskMitigation` returning `CreditRiskMitigationDTO`
  - Add `fromDTO(CreditRiskMitigationDTO)` static factory method to `CreditRiskMitigation`
  - _Requirements: 3.1, 3.3, 3.4_

- [x] 3. Update ParsedFileData with conversion methods
  **Status: COMPLETED**
  
  **Implementation Summary:**
  - ✅ Updated `ParsedFileData` record to include `BankInfoModel` as first field
  - ✅ Added `toDTO()` method to `ParsedFileData` returning `BatchDataDTO`
  - ✅ Added `fromDTO(BatchDataDTO)` static factory method to `ParsedFileData`
  - ✅ Updated `DefaultFileParsingService.parseJsonFile()` to pass `bankInfo` from parsing result
  - ✅ Updated `DefaultFileParsingService.parseExcelFile()` to pass `null` for bankInfo (Excel doesn't have bank_info)
  - ✅ All code compiles successfully
  - _Requirements: 3.1, 3.2_

- [ ]* 3.1 Write property test for exposure mapping
  - **Property 2: Exposure Mapping Preserves All Fields**
  - **Validates: Requirements 3.3, 4.2, 5.2**

- [ ]* 3.2 Write property test for mitigation mapping
  - **Property 3: Mitigation Mapping Preserves All Fields**
  - **Validates: Requirements 3.4**

- [ ]* 3.3 Write property test for Ingestion serialization round trip
  - **Property 1: Serialization Round Trip Preserves Data** (Ingestion variant)
  - **Validates: Requirements 3.2, 7.4**

- [x] 4. Update Ingestion file writing to use toDTO




  - Update file storage service to call `parsedFileData.toDTO()` before serialization
  - Ensure JSON output matches expected structure
  - Update logging to include bank information
  - _Requirements: 3.5_

- [ ]* 4.1 Write property test for JSON structure validation
  - **Property 6: Batch Data Structure Completeness**
  - **Validates: Requirements 3.5, 7.5**i

- [x] 5. Remove duplicate DTOs from Risk Calculation module





  - Delete `regtech-risk-calculation/infrastructure/src/main/java/com/bcbs239/regtech/riskcalculation/infrastructure/dto/` package
  - Update all imports to use Core module DTOs
  - Update pom.xml to ensure Core domain dependency exists
  - _Requirements: 4.3, 4.4_

- [x] 6. Add conversion methods to Risk Calculation domain models





  - Add `fromDTO(BankInfoDTO)` static factory method to `BankInfo` value object
  - Add `fromDTO(ExposureDTO)` static factory method to `ExposureRecording`
  - Add `fromDTO(CreditRiskMitigationDTO)` static factory method to `Mitigation`
  - Update domain models to construct themselves from DTOs
  - _Requirements: 4.2_

- [ ]* 6.1 Write property test for Risk Calculation deserialization
  - **Property 1: Serialization Round Trip Preserves Data** (Risk Calculation variant)
  - **Validates: Requirements 4.1, 4.2, 7.4**

- [-] 7. Update Risk Calculation file reading to use fromDTO


  - Update file storage service to deserialize to `BatchDataDTO`
  - Call `ExposureRecording.fromDTO()` for each exposure
  - Call `BankInfo.fromDTO()` for bank information
  - Update error handling for deserialization failures
  - _Requirements: 4.1, 4.5_

- [ ] 8. Add conversion method to Data Quality ExposureRecord
  - Add `fromDTO(ExposureDTO)` static factory method to `ExposureRecord`
  - Map all fields from ExposureDTO to ExposureRecord
  - Handle field name variations (snake_case and camelCase)
  - _Requirements: 5.2_

- [ ]* 8.1 Write property test for Data Quality deserialization
  - **Property 1: Serialization Round Trip Preserves Data** (Data Quality variant)
  - **Validates: Requirements 5.1, 5.2, 7.4**

- [ ] 9. Update Data Quality parsing logic to use BatchDataDTO
  - Update `S3StorageServiceImpl.downloadAndParseStreaming` to check for "exposures" and "bank_info" fields
  - Deserialize to `BatchDataDTO` when new format detected
  - Call `ExposureRecord.fromDTO()` for each exposure in BatchDataDTO
  - Keep backward compatibility for "loan_portfolio" field
  - Keep backward compatibility for direct array format
  - Add clear error message for unsupported formats
  - Update logging to include bank information if available
  - _Requirements: 5.1, 5.3, 5.4, 5.5_

- [ ]* 9.1 Write property test for new format parsing
  - **Property 7: New Format Parsing**
  - **Validates: Requirements 5.4**

- [ ]* 9.2 Write unit tests for backward compatibility
  - Test that old "loan_portfolio" format still parses
  - Test that direct array format still parses
  - Test that new "exposures" format parses
  - _Requirements: 5.3_

- [ ] 10. Create integration test for Ingestion → Risk Calculation
  - Create test that calls `parsedFileData.toDTO()` to get BatchDataDTO
  - Serialize BatchDataDTO to JSON
  - Deserialize JSON back to BatchDataDTO
  - Call `ExposureRecording.fromDTO()` for each exposure
  - Verify all data is preserved
  - _Requirements: 7.1, 7.2_

- [ ]* 10.1 Write property test for integration compatibility
  - **Property 8: Integration Compatibility** (Ingestion → Risk Calculation)
  - **Validates: Requirements 7.1, 7.2, 7.3**

- [ ] 11. Create integration test for Ingestion → Data Quality
  - Create test that calls `parsedFileData.toDTO()` to get BatchDataDTO
  - Serialize BatchDataDTO to JSON
  - Deserialize JSON using Data Quality parsing logic
  - Call `ExposureRecord.fromDTO()` for each exposure
  - Verify all exposures are parsed correctly
  - _Requirements: 7.1, 7.3_

- [ ] 12. Update sample data files
  - Update test data files in `data/raw/` to use new format
  - Ensure `bank_info` is at top level
  - Ensure `exposures` and `credit_risk_mitigation` are at top level
  - Update any test fixtures in test resources
  - _Requirements: 3.5_

- [ ] 13. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 14. Update documentation
  - Document the BatchDataDTO structure in Core module README
  - Document the DDD approach: objects know how to convert themselves
  - Update Ingestion module documentation with new format and toDTO/fromDTO methods
  - Update Risk Calculation module documentation with fromDTO methods
  - Update Data Quality module documentation with fromDTO methods
  - Add migration guide for old format to new format
  - _Requirements: 6.4_
