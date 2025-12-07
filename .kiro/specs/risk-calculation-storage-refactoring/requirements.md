# Requirements Document

## Introduction

The Risk Calculation Storage Refactoring addresses the need to simplify data storage architecture by adopting a file-first strategy. Currently, the Risk Calculation Module stores detailed exposure and mitigation data in PostgreSQL tables, creating redundancy since complete calculation results are also serialized to JSON files. This refactoring eliminates duplicate storage, simplifies the database schema, and establishes JSON files as the single source of truth for detailed calculation results while maintaining minimal database metadata for operational queries.

## Glossary

- **Risk_Calculation_Module**: The bounded context responsible for calculating risk metrics and concentration analysis
- **File_Storage_Service**: Service interface for storing and retrieving JSON files (S3 or local filesystem)
- **Calculation_Results_JSON**: Complete calculation results serialized to JSON format including all exposures, mitigations, and analysis
- **Batch_Metadata**: Minimal information stored in database for batch tracking (status, URI, timestamps)
- **Portfolio_Analysis_Summary**: Optional database record containing summary metrics for quick API access
- **Single_Source_of_Truth**: The JSON file containing complete, authoritative calculation results
- **Calculation_Results_URI**: S3 URI or filesystem path pointing to the JSON file containing results
- **Database_Schema**: PostgreSQL tables for storing batch metadata and optional summaries
- **Exposure_Repository**: Repository interface for accessing exposure data (to be refactored)
- **Mitigation_Repository**: Repository interface for accessing mitigation data (to be refactored)
- **Batch_Repository**: Repository interface for batch metadata persistence
- **Portfolio_Analysis_Repository**: Repository interface for summary metrics persistence

## Requirements

### Requirement 1

**User Story:** As a system architect, I want JSON files to be the single source of truth for calculation results, so that I can eliminate data duplication and simplify the database schema.

#### Acceptance Criteria

1. WHEN risk calculation completes THEN the Risk_Calculation_Module SHALL serialize complete results to JSON format
2. WHEN serializing results THEN the Risk_Calculation_Module SHALL include batch metadata, all exposures with full details, all mitigations, and portfolio analysis
3. WHEN serializing results THEN the Risk_Calculation_Module SHALL use the File_Storage_Service to store the JSON file
4. WHEN storing JSON files THEN the File_Storage_Service SHALL return a Calculation_Results_URI (S3 URI or filesystem path)
5. WHEN JSON storage succeeds THEN the Risk_Calculation_Module SHALL proceed with database metadata persistence

### Requirement 2

**User Story:** As a database administrator, I want to store only minimal batch metadata in the database, so that I can reduce storage costs and simplify schema maintenance.

#### Acceptance Criteria

1. WHEN persisting batch metadata THEN the Risk_Calculation_Module SHALL store batch_id, bank_info, report_date, total_exposures, status, and calculation_results_uri
2. WHEN persisting batch metadata THEN the Risk_Calculation_Module SHALL store ingested_at and processed_at timestamps
3. WHEN persisting batch metadata THEN the Risk_Calculation_Module SHALL NOT store individual exposure records in the database
4. WHEN persisting batch metadata THEN the Risk_Calculation_Module SHALL NOT store individual mitigation records in the database
5. WHEN batch processing completes THEN the Risk_Calculation_Module SHALL update batch status to COMPLETED and store the calculation_results_uri

### Requirement 3

**User Story:** As a system operator, I want optional portfolio analysis summaries in the database, so that I can provide fast API responses for dashboard metrics without parsing JSON files.

#### Acceptance Criteria

1. WHEN portfolio analysis completes THEN the Risk_Calculation_Module SHALL optionally persist summary metrics to portfolio_analysis table
2. WHEN persisting portfolio analysis THEN the Risk_Calculation_Module SHALL store batch_id, total_portfolio_eur, and analyzed_at timestamp
3. WHEN persisting portfolio analysis THEN the Risk_Calculation_Module SHALL store geographic HHI and concentration level
4. WHEN persisting portfolio analysis THEN the Risk_Calculation_Module SHALL store sector HHI and concentration level
5. WHEN persisting portfolio analysis THEN the Risk_Calculation_Module SHALL NOT store detailed breakdown amounts and percentages

### Requirement 4

**User Story:** As a report generation module, I want to receive calculation results via JSON files, so that I can access complete exposure details without database queries.

#### Acceptance Criteria

1. WHEN BatchCalculationCompletedEvent is published THEN the Risk_Calculation_Module SHALL include the calculation_results_uri in the event
2. WHEN receiving the event THEN downstream modules SHALL download the JSON file using the calculation_results_uri
3. WHEN downloading JSON files THEN the File_Storage_Service SHALL return the complete calculation results
4. WHEN parsing JSON files THEN downstream modules SHALL deserialize to domain objects
5. WHEN JSON file is unavailable THEN the File_Storage_Service SHALL throw FileNotFoundException

### Requirement 5

**User Story:** As a developer, I want to refactor repository interfaces to work with JSON files, so that the codebase reflects the file-first architecture.

#### Acceptance Criteria

1. WHEN refactoring repositories THEN the Risk_Calculation_Module SHALL remove ExposureRepository database persistence methods
2. WHEN refactoring repositories THEN the Risk_Calculation_Module SHALL remove MitigationRepository database persistence methods
3. WHEN refactoring repositories THEN the Risk_Calculation_Module SHALL keep BatchRepository for metadata persistence
4. WHEN refactoring repositories THEN the Risk_Calculation_Module SHALL keep PortfolioAnalysisRepository for optional summary persistence
5. WHEN accessing exposure details THEN the Risk_Calculation_Module SHALL provide methods to download and parse JSON files

### Requirement 6

**User Story:** As a database administrator, I want to deprecate and remove unused database tables, so that I can free up storage and simplify maintenance.

#### Acceptance Criteria

1. WHEN migration is complete THEN the Risk_Calculation_Module SHALL mark exposures table as deprecated
2. WHEN migration is complete THEN the Risk_Calculation_Module SHALL mark mitigations table as deprecated
3. WHEN all batches use JSON files THEN the Risk_Calculation_Module SHALL support dropping the exposures table
4. WHEN all batches use JSON files THEN the Risk_Calculation_Module SHALL support dropping the mitigations table
5. WHEN tables are dropped THEN the Risk_Calculation_Module SHALL continue functioning with JSON-only storage

### Requirement 7

**User Story:** As a system operator, I want comprehensive error handling for file storage operations, so that failures are detected and logged appropriately.

#### Acceptance Criteria

1. WHEN JSON serialization fails THEN the Risk_Calculation_Module SHALL throw CalculationResultsSerializationException
2. WHEN file upload fails THEN the File_Storage_Service SHALL throw FileStorageException
3. WHEN file download fails THEN the File_Storage_Service SHALL throw FileNotFoundException
4. WHEN JSON deserialization fails THEN the Risk_Calculation_Module SHALL throw CalculationResultsDeserializationException
5. WHEN file storage errors occur THEN the Risk_Calculation_Module SHALL publish BatchCalculationFailedEvent with error details

### Requirement 8

**User Story:** As a compliance officer, I want JSON files to serve as immutable audit records, so that I can review historical calculation results without database dependencies.

#### Acceptance Criteria

1. WHEN JSON files are created THEN the Risk_Calculation_Module SHALL ensure they are immutable (write-once)
2. WHEN JSON files are stored THEN the File_Storage_Service SHALL preserve them for the configured retention period
3. WHEN accessing historical data THEN the Risk_Calculation_Module SHALL retrieve JSON files by batch_id
4. WHEN JSON files exist THEN the Risk_Calculation_Module SHALL NOT modify or overwrite them
5. WHEN audit queries are performed THEN the Risk_Calculation_Module SHALL parse JSON files to extract required data

### Requirement 9

**User Story:** As a developer, I want clear separation between operational metadata (database) and detailed results (files), so that I can query batch status quickly without parsing large JSON files.

#### Acceptance Criteria

1. WHEN querying batch status THEN the Risk_Calculation_Module SHALL use database queries (no file access)
2. WHEN querying summary metrics THEN the Risk_Calculation_Module SHALL use portfolio_analysis table (no file access)
3. WHEN querying detailed exposures THEN the Risk_Calculation_Module SHALL download and parse JSON files
4. WHEN listing batches THEN the Risk_Calculation_Module SHALL use database queries with filters on status and report_date
5. WHEN providing dashboard metrics THEN the Risk_Calculation_Module SHALL use database queries for counts and aggregates

### Requirement 10

**User Story:** As a system architect, I want the JSON file format to be well-defined and versioned, so that downstream modules can reliably parse calculation results.

#### Acceptance Criteria

1. WHEN serializing to JSON THEN the Risk_Calculation_Module SHALL include a format_version field
2. WHEN serializing to JSON THEN the Risk_Calculation_Module SHALL include batch_id and calculated_at timestamp
3. WHEN serializing to JSON THEN the Risk_Calculation_Module SHALL include summary section with totals and breakdowns
4. WHEN serializing to JSON THEN the Risk_Calculation_Module SHALL include exposures array with complete exposure details
5. WHEN format changes THEN the Risk_Calculation_Module SHALL increment the format_version and maintain backward compatibility
