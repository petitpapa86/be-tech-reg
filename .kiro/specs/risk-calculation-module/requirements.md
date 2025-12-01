# Requirements Document

## Introduction

The Risk Calculation Module is a critical component of the RegTech platform that processes financial exposure data through distinct bounded contexts to calculate comprehensive risk metrics, geographic and sector concentration analysis, and regulatory compliance indicators. The module follows Domain-Driven Design principles with clear separation of concerns across Exposure Recording, Valuation, Credit Protection, Classification, and Portfolio Analysis contexts. It operates as part of the event-driven architecture, processing batches of exposure data to provide real-time risk assessment capabilities for regulatory reporting under BCBS 239 standards.

## Glossary

- **Risk_Calculation_Module**: The bounded context responsible for orchestrating risk calculations across multiple sub-contexts
- **Exposure_Recording**: Bounded context for capturing immutable facts about financial instruments (loans, bonds, derivatives, etc.)
- **Valuation_Engine**: Bounded context responsible for currency conversion to EUR
- **Credit_Protection**: Bounded context for calculating net exposures after applying credit risk mitigations
- **Classification_Service**: Bounded context for geographic and sector classification
- **Portfolio_Analysis**: Bounded context for calculating concentration metrics and HHI indices
- **Exposure_Record**: A generic financial exposure entry (loan, bond, derivative, guarantee, etc.) containing instrument information, counterparty details, amounts, and classification
- **Instrument_Type**: Type of financial instrument (LOAN, BOND, DERIVATIVE, GUARANTEE, CREDIT_LINE, REPO, SECURITY, INTERBANK, OTHER)
- **Batch_Summary**: Aggregated risk metrics and concentration data for a complete batch of exposures
- **Geographic_Region**: Classification of exposures by geographic location (ITALY, EU_OTHER, NON_EUROPEAN)
- **Economic_Sector**: Classification of exposures by economic sector (RETAIL_MORTGAGE, SOVEREIGN, CORPORATE, BANKING, OTHER)
- **Herfindahl_Index**: Mathematical measure of concentration calculated as the sum of squared market shares
- **Exchange_Rate_Provider**: External service providing currency exchange rates
- **Database_Storage**: PostgreSQL database for storing batch summaries and portfolio analysis
- **Integration_Event**: Cross-module event that enables communication between bounded contexts
- **Batch_Status**: State of batch processing (INGESTED, PROCESSING, COMPLETED, FAILED)

## Requirements

### Requirement 1

**User Story:** As a risk analyst, I want the system to automatically ingest and validate exposure data from JSON reports, so that I can process financial exposures from various instrument types (loans, bonds, derivatives, etc.).

#### Acceptance Criteria

1. WHEN a JSON risk report is received THEN the Risk_Calculation_Module SHALL parse and validate the bank information
2. WHEN parsing exposures THEN the Risk_Calculation_Module SHALL support multiple instrument types (LOAN, BOND, DERIVATIVE, GUARANTEE, CREDIT_LINE, REPO, SECURITY, INTERBANK, OTHER)
3. WHEN validating exposures THEN the Risk_Calculation_Module SHALL verify that all exposure IDs are unique within the batch
4. WHEN validating mitigations THEN the Risk_Calculation_Module SHALL verify that all mitigation references point to existing exposures
5. WHEN validation fails THEN the Risk_Calculation_Module SHALL throw an InvalidReportException with descriptive error message

### Requirement 2

**User Story:** As a risk analyst, I want all exposure amounts converted to EUR through the Valuation Engine, so that I can perform accurate aggregation and comparison across different currencies.

#### Acceptance Criteria

1. WHEN the Valuation_Engine processes an exposure THEN it SHALL convert the exposure amount to EUR using current exchange rates
2. WHEN an exposure is already in EUR THEN the Valuation_Engine SHALL create an identity exchange rate (EUR to EUR = 1.0)
3. WHEN converting currencies THEN the Valuation_Engine SHALL preserve the original monetary amount and currency code
4. WHEN converting currencies THEN the Valuation_Engine SHALL record the exchange rate used with effective date for audit purposes
5. WHEN exchange rate data is unavailable THEN the Valuation_Engine SHALL fail and the ExchangeRateProvider SHALL return an error

### Requirement 3

**User Story:** As a compliance officer, I want the Credit Protection context to calculate net exposures after applying mitigations, so that I can assess actual risk exposure after collateral and guarantees.

#### Acceptance Criteria

1. WHEN the Credit_Protection context processes an exposure THEN it SHALL calculate gross exposure in EUR from the Valuation_Engine
2. WHEN mitigations exist for an exposure THEN the Credit_Protection SHALL convert each mitigation value to EUR
3. WHEN calculating net exposure THEN the Credit_Protection SHALL subtract total mitigation value from gross exposure
4. WHEN net exposure calculation results in negative value THEN the Credit_Protection SHALL set net exposure to zero
5. WHEN no mitigations exist THEN the Credit_Protection SHALL set net exposure equal to gross exposure

### Requirement 4

**User Story:** As a compliance officer, I want the Classification Service to classify exposures by geographic regions, so that I can monitor geographic concentration risk and home country bias.

#### Acceptance Criteria

1. WHEN the Classification_Service processes an exposure THEN it SHALL classify the country code into ITALY, EU_OTHER, or NON_EUROPEAN regions
2. WHEN an exposure country is "IT" THEN the Classification_Service SHALL classify it as ITALY
3. WHEN an exposure country is in the EU_COUNTRIES set but not Italy THEN the Classification_Service SHALL classify it as EU_OTHER
4. WHEN an exposure country is not in the EU_COUNTRIES set THEN the Classification_Service SHALL classify it as NON_EUROPEAN
5. WHEN classification is complete THEN the Classification_Service SHALL return a ClassifiedExposure with region and sector

### Requirement 5

**User Story:** As a risk manager, I want the Classification Service to classify exposures by economic sectors, so that I can assess sector concentration risk and diversification.

#### Acceptance Criteria

1. WHEN the Classification_Service processes a product type THEN it SHALL classify it into RETAIL_MORTGAGE, SOVEREIGN, CORPORATE, BANKING, or OTHER sectors
2. WHEN product type contains "MORTGAGE" THEN the Classification_Service SHALL classify it as RETAIL_MORTGAGE
3. WHEN product type contains "GOVERNMENT" or "TREASURY" THEN the Classification_Service SHALL classify it as SOVEREIGN
4. WHEN product type contains "INTERBANK" THEN the Classification_Service SHALL classify it as BANKING
5. WHEN product type contains "BUSINESS", "EQUIPMENT", or "CREDIT LINE" THEN the Classification_Service SHALL classify it as CORPORATE
6. WHEN product type does not match any pattern THEN the Classification_Service SHALL classify it as OTHER

### Requirement 6

**User Story:** As a regulatory compliance officer, I want the Portfolio Analysis context to calculate concentration indices automatically, so that I can monitor portfolio concentration risk against regulatory limits.

#### Acceptance Criteria

1. WHEN the Portfolio_Analysis context aggregates exposures THEN it SHALL calculate total portfolio amount in EUR
2. WHEN calculating geographic breakdown THEN the Portfolio_Analysis SHALL group net exposures by GeographicRegion and calculate amounts and percentages
3. WHEN calculating sector breakdown THEN the Portfolio_Analysis SHALL group net exposures by EconomicSector and calculate amounts and percentages
4. WHEN calculating Herfindahl-Hirschman Index THEN the Portfolio_Analysis SHALL use the formula HHI = Σ(share_i)²
5. WHEN HHI is less than 0.15 THEN the Portfolio_Analysis SHALL classify concentration level as LOW
6. WHEN HHI is between 0.15 and 0.25 THEN the Portfolio_Analysis SHALL classify concentration level as MODERATE
7. WHEN HHI exceeds 0.25 THEN the Portfolio_Analysis SHALL classify concentration level as HIGH

### Requirement 7

**User Story:** As a system administrator, I want batch and exposure data persisted to the database, so that the system can maintain audit trails and support queries.

#### Acceptance Criteria

1. WHEN ingestion completes THEN the Risk_Calculation_Module SHALL persist batch metadata to the batches table
2. WHEN persisting batches THEN the Risk_Calculation_Module SHALL store bank_name, abi_code, lei_code, report_date, total_exposures, status, and timestamps
3. WHEN ingestion completes THEN the Risk_Calculation_Module SHALL persist all exposures to the exposures table
4. WHEN persisting exposures THEN the Risk_Calculation_Module SHALL store exposure_id, batch_id, instrument_id, counterparty details, amounts, currency, classification, and timestamps
5. WHEN ingestion completes THEN the Risk_Calculation_Module SHALL persist all mitigations to the mitigations table
6. WHEN persisting mitigations THEN the Risk_Calculation_Module SHALL store exposure_id, batch_id, mitigation_type, value, currency, and timestamps

### Requirement 8

**User Story:** As a system administrator, I want portfolio analysis results persisted to the database, so that I can query concentration metrics and risk indicators.

#### Acceptance Criteria

1. WHEN portfolio analysis completes THEN the Risk_Calculation_Module SHALL persist results to the portfolio_analysis table
2. WHEN persisting portfolio analysis THEN the Risk_Calculation_Module SHALL store batch_id, total_portfolio_eur, and analyzed_at timestamp
3. WHEN persisting geographic breakdown THEN the Risk_Calculation_Module SHALL store amounts and percentages for italy, eu_other, and non_european regions
4. WHEN persisting sector breakdown THEN the Risk_Calculation_Module SHALL store amounts and percentages for retail_mortgage, sovereign, corporate, banking, and other sectors
5. WHEN persisting concentration indices THEN the Risk_Calculation_Module SHALL store geographic_hhi, geographic_concentration_level, sector_hhi, and sector_concentration_level

### Requirement 9

**User Story:** As a system operator, I want comprehensive error handling and validation, so that invalid data is rejected with clear error messages.

#### Acceptance Criteria

1. WHEN bank information is missing THEN the Risk_Calculation_Module SHALL throw InvalidReportException with message "Bank info is required"
2. WHEN exposure portfolio is empty THEN the Risk_Calculation_Module SHALL throw InvalidReportException with message "Loan portfolio cannot be empty"
3. WHEN credit risk mitigation data is missing THEN the Risk_Calculation_Module SHALL throw InvalidReportException with message "Credit risk mitigation is required"
4. WHEN duplicate exposure IDs are detected THEN the Risk_Calculation_Module SHALL throw InvalidReportException with message "Duplicate exposure IDs detected"
5. WHEN mitigations reference non-existent exposures THEN the Risk_Calculation_Module SHALL throw InvalidReportException with message "Some mitigations reference non-existent exposures"