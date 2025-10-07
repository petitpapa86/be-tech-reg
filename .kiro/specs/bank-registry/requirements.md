# Bank Registry Context - Requirements Document

## Introduction

The Bank Registry context manages the core bank information, subscription tiers, and capital data using functional programming patterns and pure business functions. It implements value objects as Java records, uses `Result<T, ErrorDetail>` for explicit error handling, and employs closure-based dependency injection for external registry validation.

This context serves as the authoritative source for bank identity and regulatory status while maintaining framework-independent business logic through pure functions and repository closures.

## Requirements

### Requirement 1: Comprehensive Bank Institution Information Management

**User Story:** As a Bank Administrator, I want to configure complete bank institution information including regulatory details, so that the system has all necessary data for BCBS 239 compliance.

#### Acceptance Criteria

1. WHEN bank institution information is configured THEN the system SHALL collect denominazione sociale (legal name), codice ABI (5-digit Italian bank code), codice LEI (20-character Legal Entity Identifier), and sede legale (legal address)
2. WHEN bank classification is set THEN the system SHALL support gruppo bancario (banking group), tipologia banca (bank type: Banca di Credito Cooperativo, Banca Commerciale, etc.), and categoria vigilanza (supervisory category: SSM, LSI)
3. WHEN ABI codes are validated THEN the system SHALL enforce 5-digit numeric format and validate against Banca d'Italia registry
4. WHEN LEI codes are validated THEN the system SHALL enforce 20-character alphanumeric format and validate against GLEIF registry
5. WHEN bank information is complete THEN the system SHALL mark configuration as "Configurazione Valida" and enable BCBS 239 parameter setup

### Requirement 2: Bank Profile Management with Value Objects and Pure Functions

**User Story:** As a System Administrator, I want to manage bank profiles using value objects and pure functions, so that bank registration logic is testable and framework-independent.

#### Acceptance Criteria

1. WHEN banks are registered THEN the system SHALL use pure function `registerBank(RegisterBankCommand, Function<AbiCode, Maybe<Bank>>, Function<Bank, Result<BankId, ErrorDetail>>)` returning `Result<RegisterBankResponse, ErrorDetail>`
2. WHEN value objects are created THEN the system SHALL implement AbiCode and LeiCode as Java records with factory methods like `AbiCode.create(String)` returning `Result<AbiCode, ErrorDetail>` with pattern validation
3. WHEN external validation is needed THEN the system SHALL use closure-based validators like `Function<LeiCode, Result<Boolean, ErrorDetail>>` for GLEIF registry checks
4. WHEN bank lookups occur THEN the system SHALL use repository closure `Function<BankId, Result<Maybe<Bank>, ErrorDetail>>` for framework-free domain access
5. WHEN bank operations complete THEN the system SHALL publish domain events (`BankRegisteredEvent`, `BankStatusChangedEvent`) through internal event handlers to integration events

### Requirement 3: Multi-Bank Configuration within Subscription Limits

**User Story:** As a Customer, I want to configure multiple banks within my subscription tier limits, so that I can manage all my banking entities from one platform.

#### Acceptance Criteria

1. WHEN subscription tiers are enforced THEN the system SHALL support STARTER (1 bank, 1K exposures, 5 reports), PROFESSIONAL (5 banks, 10K exposures, 50 reports), ENTERPRISE (unlimited banks, unlimited usage)
2. WHEN new banks are added THEN the system SHALL validate against subscription tier bank limits before allowing registration
3. WHEN bank limits are approached THEN the system SHALL suggest tier upgrades and provide upgrade options
4. WHEN tier upgrades occur THEN the system SHALL immediately apply new bank limits and pro-rate billing adjustments
5. WHEN banks are configured THEN the system SHALL automatically assign the configuring user as BANK_ADMIN for that specific bank

### Requirement 4: BCBS 239 Parameters Configuration

**User Story:** As a Compliance Officer, I want to configure BCBS 239 parameters and thresholds, so that large exposure analysis follows regulatory requirements.

#### Acceptance Criteria

1. WHEN large exposure limits are configured THEN the system SHALL set limite grande esposizione (default 25% of eligible capital per EU Article 395 CRR) and soglia classificazione (default 10% threshold for classification)
2. WHEN capital base is defined THEN the system SHALL require capitale ammissibile (eligible capital amount in EUR) and metodo calcolo (calculation method: Approccio Standardizzato or Metodo Avanzato)
3. WHEN data quality thresholds are set THEN the system SHALL configure soglia qualità minima (minimum 95% completeness) and validazione file (automatic or custom validation rules)
4. WHEN reporting frequencies are configured THEN the system SHALL set frequenza standard (monthly default), giorni per submission (20 days after reference period), and crisis mode capabilities
5. WHEN parameters are validated THEN the system SHALL ensure all thresholds are within regulatory bounds and mark configuration as complete

### Requirement 5: Capital Data Management

**User Story:** As a Bank Administrator, I want to maintain current capital data, so that risk calculations and large exposure thresholds are accurate.

#### Acceptance Criteria

1. WHEN capital data is entered THEN the system SHALL require eligible_capital_large_exposures amount in EUR
2. WHEN reporting dates are set THEN the system SHALL ensure capital data corresponds to specific reporting periods
3. WHEN capital amounts are updated THEN the system SHALL validate positive values and reasonable ranges
4. WHEN historical capital data is needed THEN the system SHALL maintain time-series data for trend analysis
5. WHEN capital data changes THEN the system SHALL trigger recalculation of exposure percentages in dependent systems

### Requirement 6: Multi-Template Regulatory Configuration Management

**User Story:** As a Bank Administrator, I want to configure multiple regulatory templates for different reporting requirements, so that I can upload files and generate reports according to various regulatory frameworks.

#### Acceptance Criteria

1. WHEN regulatory templates are configured THEN the system SHALL support three template types: "Italian Large Exposures (Circolare 285)", "EU Large Exposures EBA ITS", and "Italian Risk Concentration Supervisory"
2. WHEN template selection occurs THEN the system SHALL allow banks to configure multiple templates simultaneously based on their regulatory obligations
3. WHEN file uploads happen THEN the system SHALL use the selected template for validation, processing, and report generation
4. WHEN template configurations are saved THEN the system SHALL store template-specific parameters including output formats, validation rules, and BCBS 239 compliance settings
5. WHEN templates are activated THEN the system SHALL publish TemplateConfiguredEvent with context section mappings for downstream processing

### Requirement 7: Report Configuration and Template Management

**User Story:** As a Bank Administrator, I want to configure report generation settings and distribution for each regulatory template, so that reports are automatically generated and delivered according to specific regulatory requirements.

#### Acceptance Criteria

1. WHEN output formats are chosen THEN the system SHALL support PDF (recommended), Excel (.xlsx), or both formats with proper localization for each template
2. WHEN distribution is configured THEN the system SHALL allow email compliance team setup, CC recipients, and automatic report delivery upon completion for each template
3. WHEN scheduling is enabled THEN the system SHALL support automatic generation with configurable day of week (default: Monday) and time (default: 09:00) per template
4. WHEN regulatory submission is configured THEN the system SHALL prepare for automatic submission to appropriate authorities (Banca d'Italia, EBA) based on template type
5. WHEN template-specific settings are applied THEN the system SHALL maintain separate configuration for each regulatory framework while sharing common bank parameters

### Requirement 8: External Registry Integration

**User Story:** As a Compliance Officer, I want automatic validation against external registries, so that our bank information remains accurate and compliant.

#### Acceptance Criteria

1. WHEN LEI codes are validated THEN the system SHALL query GLEIF API for real-time verification
2. WHEN ABI codes are checked THEN the system SHALL validate against Italian Banking Registry
3. WHEN external validation fails THEN the system SHALL flag discrepancies and require manual review
4. WHEN registry data is updated THEN the system SHALL periodically refresh validation status (daily)
5. WHEN validation errors occur THEN the system SHALL log issues and notify bank administrators

### Requirement 9: Multi-Tenant Data Isolation

**User Story:** As a Security Administrator, I want strict data isolation between banks, so that each bank can only access their own information.

#### Acceptance Criteria

1. WHEN bank data is accessed THEN the system SHALL enforce tenant isolation using bank_id scoping
2. WHEN queries are executed THEN the system SHALL automatically filter results by authenticated user's bank access
3. WHEN cross-bank operations are needed THEN the system SHALL require SYSTEM_ADMIN privileges
4. WHEN data export occurs THEN the system SHALL include only data the user is authorized to access
5. WHEN audit trails are generated THEN the system SHALL log all cross-tenant access attempts

### Requirement 10: Subscription Billing Integration

**User Story:** As a Billing Administrator, I want seamless integration with billing systems, so that subscription charges are accurate and timely.

#### Acceptance Criteria

1. WHEN subscriptions are created THEN the system SHALL generate Stripe subscription records with proper metadata
2. WHEN billing periods end THEN the system SHALL calculate usage-based charges and overage fees
3. WHEN payment failures occur THEN the system SHALL handle dunning management and service suspension
4. WHEN subscription changes happen THEN the system SHALL handle pro-ration and immediate billing adjustments
5. WHEN invoices are generated THEN the system SHALL include detailed usage breakdowns and compliance with tax requirements

### Requirement 11: Bank Status and Lifecycle Management

**User Story:** As a Platform Administrator, I want to manage bank lifecycle states, so that inactive or non-compliant banks are properly handled.

#### Acceptance Criteria

1. WHEN banks are onboarded THEN the system SHALL guide through setup process including capital data and user assignments
2. WHEN banks become non-compliant THEN the system SHALL support graduated responses (warnings, restrictions, suspension)
3. WHEN subscriptions are cancelled THEN the system SHALL maintain data retention policies while preventing new access
4. WHEN banks are reactivated THEN the system SHALL restore access while maintaining historical compliance records
5. WHEN bank termination occurs THEN the system SHALL provide data export capabilities and secure data deletion