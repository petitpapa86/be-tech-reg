# Requirements Document: Data Format Alignment

## Introduction

This specification addresses the need to align the data format across the Ingestion, Risk Calculation, and Data Quality modules. Currently, these modules have duplicate DTOs and expect different JSON structures when reading batch data files, causing integration issues. The goal is to create shared DTOs in the Core module and standardize on a single, consistent format that includes bank information, exposures, and credit risk mitigation data.

## Glossary

- **Ingestion Module**: The system component responsible for receiving and parsing uploaded financial data files
- **Risk Calculation Module**: The system component that performs risk analysis on exposure data
- **Data Quality Module**: The system component that validates data quality against business rules
- **Core Module**: The shared module containing common domain models and DTOs used across all modules
- **Batch Data File**: JSON file stored in S3 or local filesystem containing exposure data for a batch
- **Bank Info**: Metadata about the financial institution including bank name, ABI code, LEI code, and report date
- **DTO (Data Transfer Object)**: A simple object used for transferring data between modules or layers

## Requirements

### Requirement 1

**User Story:** As a developer, I want shared DTOs in the Core module for batch data interchange, so that all modules use consistent data structures without duplication.

#### Acceptance Criteria

1. WHEN defining batch data DTOs THEN the system SHALL create them in the Core module under a shared package
2. WHEN creating a BankInfoDTO THEN the system SHALL include fields for bankName, abiCode, leiCode, reportDate, and totalExposures
3. WHEN creating an ExposureDTO THEN the system SHALL include all fields needed by Risk Calculation and Data Quality modules
4. WHEN creating a CreditRiskMitigationDTO THEN the system SHALL include fields for exposureId, mitigationType, value, and currency
5. WHEN creating a BatchDataDTO THEN the system SHALL compose BankInfoDTO, list of ExposureDTO, and list of CreditRiskMitigationDTO

### Requirement 2

**User Story:** As a system integrator, I want all modules to use the shared DTOs from Core, so that data format changes are managed in one place.

#### Acceptance Criteria

1. WHEN the Ingestion module serializes batch data THEN the system SHALL use BatchDataDTO from Core module
2. WHEN the Risk Calculation module reads batch data THEN the system SHALL use BatchDataDTO from Core module
3. WHEN the Data Quality module reads batch data THEN the system SHALL use BatchDataDTO from Core module
4. WHEN a module needs to convert between domain models and DTOs THEN the system SHALL provide mapper classes
5. WHEN the Core module updates DTO definitions THEN the system SHALL ensure all dependent modules are updated

### Requirement 3

**User Story:** As a developer, I want the Ingestion module to use shared DTOs when writing batch files, so that the output format matches what downstream modules expect.

#### Acceptance Criteria

1. WHEN the Ingestion module creates ParsedFileData THEN the system SHALL include a BankInfo value object
2. WHEN the Ingestion module serializes batch data to JSON THEN the system SHALL convert ParsedFileData to BatchDataDTO
3. WHEN the Ingestion module writes exposures THEN the system SHALL map LoanExposure domain objects to ExposureDTO
4. WHEN the Ingestion module writes mitigations THEN the system SHALL map CreditRiskMitigation domain objects to CreditRiskMitigationDTO
5. WHEN the Ingestion module completes serialization THEN the system SHALL produce JSON matching the BatchDataDTO structure

### Requirement 4

**User Story:** As a developer, I want the Risk Calculation module to use shared DTOs when reading batch files, so that it can process data from the Ingestion module without custom parsing.

#### Acceptance Criteria

1. WHEN the Risk Calculation module reads a batch file THEN the system SHALL deserialize JSON to BatchDataDTO from Core
2. WHEN the Risk Calculation module processes batch data THEN the system SHALL convert BatchDataDTO to domain models
3. WHEN the Risk Calculation module removes duplicate DTOs THEN the system SHALL delete the infrastructure/dto package
4. WHEN the Risk Calculation module updates imports THEN the system SHALL reference Core module DTOs
5. WHEN the Risk Calculation module validates batch data THEN the system SHALL use the BankInfoDTO from Core

### Requirement 5

**User Story:** As a developer, I want the Data Quality module to use shared DTOs when reading batch files, so that it can validate data from the Ingestion module without custom parsing.

#### Acceptance Criteria

1. WHEN the Data Quality module downloads batch data THEN the system SHALL deserialize JSON to BatchDataDTO from Core
2. WHEN the Data Quality module parses exposures THEN the system SHALL convert ExposureDTO to ExposureRecord domain model
3. WHEN the Data Quality module encounters the old "loan_portfolio" format THEN the system SHALL continue to support it for backward compatibility
4. WHEN the Data Quality module updates parsing logic THEN the system SHALL handle the new "exposures" field at the top level
5. WHEN the Data Quality module completes parsing THEN the system SHALL log the number of exposures successfully parsed

### Requirement 6

**User Story:** As a developer, I want consistent JSON field naming across all modules, so that serialization and deserialization work correctly.

#### Acceptance Criteria

1. WHEN serializing DTOs to JSON THEN the system SHALL use snake_case for all field names
2. WHEN deserializing JSON to DTOs THEN the system SHALL support both snake_case and camelCase for backward compatibility
3. WHEN defining DTO fields THEN the system SHALL use Jackson annotations to specify JSON property names
4. WHEN the Core module defines DTOs THEN the system SHALL document the expected JSON structure
5. WHEN modules serialize or deserialize THEN the system SHALL use the shared Jackson configuration from Core

### Requirement 7

**User Story:** As a quality assurance engineer, I want integration tests that verify data format compatibility, so that format changes don't break the integration between modules.

#### Acceptance Criteria

1. WHEN running integration tests THEN the system SHALL verify that Ingestion output can be deserialized to BatchDataDTO
2. WHEN running integration tests THEN the system SHALL verify that Risk Calculation can read BatchDataDTO from JSON
3. WHEN running integration tests THEN the system SHALL verify that Data Quality can read BatchDataDTO from JSON
4. WHEN running integration tests THEN the system SHALL verify round-trip serialization preserves all data
5. WHEN running integration tests THEN the system SHALL verify that all required fields are present and correctly typed
