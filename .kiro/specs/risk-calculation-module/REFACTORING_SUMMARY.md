# Risk Calculation Module - Refactoring Summary

## Overview

The risk-calculation-module spec has been completely refactored to align with the new domain model that separates concerns into distinct bounded contexts. This refactoring transforms the module from a monolithic risk calculation service into a well-structured system following Domain-Driven Design principles.

## Key Changes

### 1. Bounded Context Separation

**Before**: Single monolithic risk calculation module mixing all concerns
**After**: Five distinct bounded contexts with clear responsibilities:

1. **Exposure Recording** - Captures immutable facts about financial instruments
2. **Valuation Engine** - Handles currency conversion to EUR
3. **Credit Protection** - Calculates net exposures after mitigations
4. **Classification Service** - Performs geographic and sector classification
5. **Portfolio Analysis** - Calculates concentration metrics and HHI

### 2. Generic Financial Instrument Support

**Before**: Loan-specific model (LoanId, LoanClassification, loan_type)
**After**: Generic exposure model supporting multiple instrument types:

- **InstrumentType enum**: LOAN, BOND, DERIVATIVE, GUARANTEE, CREDIT_LINE, REPO, SECURITY, INTERBANK, OTHER
- **InstrumentId**: Generic identifier (loan ID, bond ISIN, derivative contract ID, etc.)
- **ExposureClassification**: Generic classification with productType field

### 3. Data Flow Architecture

**Clear separation of concerns with explicit data flow**:

```
Exposure Recording → Valuation Engine → Credit Protection → Classification → Portfolio Analysis
```

Each context has a single responsibility:
- **Recording**: Store raw facts
- **Valuation**: Convert currencies
- **Protection**: Apply mitigations
- **Classification**: Categorize exposures
- **Analysis**: Calculate metrics

### 4. Database Schema Updates

**New tables aligned with bounded contexts**:

- **batches**: Batch metadata (bank info, status, timestamps)
- **exposures**: Generic exposure records (instrument_id, instrument_type, product_type)
- **mitigations**: Credit risk mitigation data
- **portfolio_analysis**: Aggregated concentration metrics

**Key schema changes**:
- `loan_id` → `instrument_id` (generic)
- `loan_type` → `product_type` (generic)
- Added `instrument_type` field (LOAN, BOND, DERIVATIVE, etc.)
- `loan_portfolio` → `exposures` (in JSON)

### 5. Requirements Restructuring

**Updated requirements to reflect bounded contexts**:

- **Requirement 1**: Exposure ingestion and validation (generic instruments)
- **Requirement 2**: Valuation Engine currency conversion
- **Requirement 3**: Credit Protection net exposure calculation
- **Requirement 4**: Geographic classification
- **Requirement 5**: Sector classification
- **Requirement 6**: Portfolio Analysis concentration metrics
- **Requirement 7**: Database persistence (batches, exposures, mitigations)
- **Requirement 8**: Portfolio analysis persistence
- **Requirement 9**: Error handling and validation

### 6. Domain Model Improvements

**Value Objects using Records**:
- Immutable by default
- Built-in validation in constructors
- Clean, concise syntax

**Aggregate Roots with Factory Methods**:
- `ExposureValuation.convert()` - Creates valuation with exchange rate
- `ProtectedExposure.calculate()` - Calculates net exposure
- `PortfolioAnalysis.analyze()` - Performs concentration analysis

**Domain Services**:
- `ExposureClassifier` - Handles geographic and sector classification
- `ExchangeRateProvider` - Interface for exchange rate data

### 7. Event-Driven Architecture Preserved

**Event-driven integration remains intact**:

The module continues to integrate with other modules through events:

**Inbound Events**:
- `BatchIngestedEvent` - Triggers risk calculation when ingestion completes
- Listens via `BatchIngestedEventListener`

**Outbound Events**:
- `BatchCalculationCompletedEvent` - Notifies downstream modules (Report Generation)
- `BatchCalculationFailedEvent` - Signals calculation failures
- Published via `RiskCalculationEventPublisher`

**Event Flow**:
```
Ingestion Module → BatchIngestedEvent → Risk Calculation Module
Risk Calculation Module → BatchCalculationCompletedEvent → Report Generation Module
```

**Application Layer Structure**:
- **RiskReportIngestionService**: Orchestrates ingestion and validation
- **RiskCalculationService**: Orchestrates bounded context flow
- **BatchIngestedEventListener**: Handles incoming events
- **RiskCalculationEventPublisher**: Publishes outgoing events
- **RiskReportMapper**: Maps DTOs to domain objects

### 8. Simplified Infrastructure

**Streamlined components while keeping event-driven architecture**:

**Kept**:
- Event-driven integration (BatchIngestedEvent, BatchCalculationCompletedEvent)
- Database persistence (PostgreSQL)
- Exchange rate provider integration
- Async event processing

**Removed**:
- S3/filesystem storage for detailed calculation results
- Complex file streaming and processing
- Retry mechanisms and exponential backoff (rely on event bus retry)

**Rationale**: The new design focuses on core domain logic with bounded contexts while maintaining event-driven integration with other modules. Database persistence replaces file storage for simplicity.

## Migration Path

### For Existing Code

1. **Update domain models**: Replace loan-specific classes with generic exposure classes
2. **Refactor repositories**: Update to use new schema (instrument_id, product_type)
3. **Update DTOs**: Change `loan_portfolio` to `exposures`, add `instrument_type`
4. **Implement bounded contexts**: Create separate services for Valuation, Protection, Classification
5. **Update database**: Run migration scripts to rename columns and add new fields

### For New Implementations

Follow the new spec structure:
1. Start with Exposure Recording context
2. Implement Valuation Engine
3. Add Credit Protection
4. Implement Classification Service
5. Complete with Portfolio Analysis

## Benefits

1. **Clear Separation of Concerns**: Each bounded context has a single responsibility
2. **Generic Instrument Support**: Handles loans, bonds, derivatives, and more
3. **Maintainability**: Changes to one context don't affect others
4. **Testability**: Each context can be tested independently
5. **Extensibility**: Easy to add new instrument types or classification rules
6. **Domain-Driven Design**: Follows DDD principles with proper aggregate boundaries

## Next Steps

1. Review the updated requirements.md
2. Review the updated design.md
3. Update tasks.md to reflect the new architecture
4. Begin implementation following the bounded context approach
