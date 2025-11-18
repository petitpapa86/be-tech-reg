# Requirements Document

## Introduction

The Risk Calculation Module is a critical component of the RegTech platform that processes financial exposure data to calculate comprehensive risk metrics, geographic and sector concentration analysis, and regulatory compliance indicators. The module operates as part of the event-driven architecture, processing batches of exposure data in parallel with the Data Quality module to provide real-time risk assessment capabilities for regulatory reporting under BCBS 239 standards.

## Glossary

- **Risk_Calculation_Module**: The bounded context responsible for calculating risk metrics, concentration indices, and geographic/sector analysis
- **Exposure_Record**: A financial exposure entry containing client information, amounts, currencies, countries, and sectors
- **Batch_Summary**: Aggregated risk metrics and concentration data for a complete batch of exposures
- **Geographic_Region**: Classification of exposures by geographic location (ITALY, EU_OTHER, NON_EUROPEAN)
- **Sector_Category**: Classification of exposures by economic sector (RETAIL_MORTGAGE, SOVEREIGN, CORPORATE, BANKING, OTHER)
- **Herfindahl_Index**: Mathematical measure of concentration calculated as the sum of squared market shares
- **Currency_Conversion**: Process of converting all exposure amounts to EUR using current exchange rates
- **S3_Storage**: Amazon S3 cloud storage service for storing detailed calculation results in production
- **File_Storage**: Local filesystem storage for detailed calculation results in development profile
- **Integration_Event**: Cross-module event that enables communication between bounded contexts
- **Calculation_Status**: State of risk calculation process (PENDING, DOWNLOADING, CALCULATING, COMPLETED, FAILED)

## Requirements

### Requirement 1

**User Story:** As a risk analyst, I want the system to automatically calculate risk metrics when new exposure data is ingested, so that I can have real-time visibility into portfolio risk concentrations.

#### Acceptance Criteria

1. WHEN a BatchIngestedEvent is received from the ingestion module THEN the Risk_Calculation_Module SHALL initiate risk calculation processing within 5 seconds
2. WHEN risk calculation processing begins THEN the Risk_Calculation_Module SHALL create a batch summary record with PENDING status
3. WHEN multiple BatchIngestedEvents are received simultaneously THEN the Risk_Calculation_Module SHALL process them in parallel without blocking
4. WHEN a BatchIngestedEvent contains invalid data THEN the Risk_Calculation_Module SHALL log the error and skip processing
5. WHEN a batch has already been processed THEN the Risk_Calculation_Module SHALL detect the duplicate and skip reprocessing

### Requirement 2

**User Story:** As a risk analyst, I want all exposure amounts converted to a common currency, so that I can perform accurate aggregation and comparison across different currencies.

#### Acceptance Criteria

1. WHEN processing exposure records THEN the Risk_Calculation_Module SHALL convert all amounts to EUR using current exchange rates
2. WHEN an exposure is already in EUR THEN the Risk_Calculation_Module SHALL preserve the original amount without conversion
3. WHEN converting currencies THEN the Risk_Calculation_Module SHALL store both original amount and converted amount
4. WHEN converting currencies THEN the Risk_Calculation_Module SHALL record the exchange rate used for audit purposes
5. WHEN exchange rate data is unavailable THEN the Risk_Calculation_Module SHALL fail the calculation and log the error

### Requirement 3

**User Story:** As a compliance officer, I want exposures classified by geographic regions, so that I can monitor geographic concentration risk and home country bias.

#### Acceptance Criteria

1. WHEN processing exposure records THEN the Risk_Calculation_Module SHALL classify each exposure into ITALY, EU_OTHER, or NON_EUROPEAN regions
2. WHEN an exposure country matches the bank's home country THEN the Risk_Calculation_Module SHALL classify it as ITALY
3. WHEN an exposure country is in the European Union but not Italy THEN the Risk_Calculation_Module SHALL classify it as EU_OTHER
4. WHEN an exposure country is outside the European Union THEN the Risk_Calculation_Module SHALL classify it as NON_EUROPEAN
5. WHEN country classification is complete THEN the Risk_Calculation_Module SHALL calculate total amounts and percentages for each geographic region

### Requirement 4

**User Story:** As a risk manager, I want exposures classified by economic sectors, so that I can assess sector concentration risk and diversification.

#### Acceptance Criteria

1. WHEN processing exposure records THEN the Risk_Calculation_Module SHALL classify each exposure into RETAIL_MORTGAGE, SOVEREIGN, CORPORATE, BANKING, or OTHER sectors
2. WHEN classifying sectors THEN the Risk_Calculation_Module SHALL map granular sector codes to standardized categories
3. WHEN sector classification is complete THEN the Risk_Calculation_Module SHALL calculate total amounts and percentages for each sector category
4. WHEN calculating percentages THEN the Risk_Calculation_Module SHALL use the total portfolio amount as the denominator
5. WHEN sector percentages are calculated THEN the Risk_Calculation_Module SHALL ensure all percentages sum to 100%

### Requirement 5

**User Story:** As a regulatory compliance officer, I want concentration indices calculated automatically, so that I can monitor portfolio concentration risk against regulatory limits.

#### Acceptance Criteria

1. WHEN aggregating risk metrics THEN the Risk_Calculation_Module SHALL calculate Herfindahl-Hirschman Index for geographic concentration
2. WHEN aggregating risk metrics THEN the Risk_Calculation_Module SHALL calculate Herfindahl-Hirschman Index for sector concentration
3. WHEN calculating Herfindahl indices THEN the Risk_Calculation_Module SHALL use the formula HHI = Σ(share_i)²
4. WHEN Herfindahl index exceeds 0.25 THEN the Risk_Calculation_Module SHALL flag high concentration risk
5. WHEN concentration calculations are complete THEN the Risk_Calculation_Module SHALL store indices with 4 decimal precision

### Requirement 6

**User Story:** As a system administrator, I want calculation results stored efficiently, so that the system can handle large volumes while maintaining query performance.

#### Acceptance Criteria

1. WHEN risk calculations are complete THEN the Risk_Calculation_Module SHALL store one summary record per batch in the database
2. WHEN storing batch summaries THEN the Risk_Calculation_Module SHALL include geographic breakdowns, sector breakdowns, and concentration indices
3. WHEN storing detailed calculations in production THEN the Risk_Calculation_Module SHALL upload individual exposure calculations to S3
4. WHEN storing detailed calculations in development profile THEN the Risk_Calculation_Module SHALL save individual exposure calculations to local filesystem
5. WHEN uploading to S3 THEN the Risk_Calculation_Module SHALL encrypt files using AES-256 encryption
6. WHEN database storage is complete THEN the Risk_Calculation_Module SHALL reference the storage URI in the batch summary record

### Requirement 7

**User Story:** As a downstream system, I want to be notified when risk calculations complete, so that I can trigger report generation and billing processes.

#### Acceptance Criteria

1. WHEN risk calculations complete successfully THEN the Risk_Calculation_Module SHALL publish a BatchCalculationCompletedEvent
2. WHEN publishing completion events THEN the Risk_Calculation_Module SHALL include batch summary data and S3 URI
3. WHEN publishing completion events THEN the Risk_Calculation_Module SHALL include geographic and sector breakdowns
4. WHEN publishing completion events THEN the Risk_Calculation_Module SHALL include concentration indices
5. WHEN calculation fails THEN the Risk_Calculation_Module SHALL publish a BatchCalculationFailedEvent with error details

### Requirement 8

**User Story:** As a system operator, I want comprehensive error handling and recovery, so that temporary failures don't result in lost calculations.

#### Acceptance Criteria

1. WHEN file download fails THEN the Risk_Calculation_Module SHALL retry up to 3 times with exponential backoff
2. WHEN currency conversion fails THEN the Risk_Calculation_Module SHALL log the error and fail the entire batch calculation
3. WHEN database operations fail THEN the Risk_Calculation_Module SHALL rollback the transaction and mark calculation as failed
4. WHEN calculation processing fails THEN the Risk_Calculation_Module SHALL update batch status to FAILED with error message
5. WHEN errors occur THEN the Risk_Calculation_Module SHALL log structured error details for troubleshooting

### Requirement 9

**User Story:** As a performance monitor, I want calculation processing to be efficient and scalable, so that the system can handle increasing data volumes.

#### Acceptance Criteria

1. WHEN processing multiple batches THEN the Risk_Calculation_Module SHALL execute calculations in parallel using thread pools
2. WHEN downloading files THEN the Risk_Calculation_Module SHALL stream parse JSON to minimize memory usage
3. WHEN processing large batches THEN the Risk_Calculation_Module SHALL complete calculations within 30 seconds for batches up to 10,000 exposures
4. WHEN system resources are constrained THEN the Risk_Calculation_Module SHALL queue calculations and process them sequentially
5. WHEN calculations complete THEN the Risk_Calculation_Module SHALL log processing time and throughput metrics