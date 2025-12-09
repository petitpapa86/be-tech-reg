# Old Class Cleanup Status

## Summary
Removed old architecture files from the risk-calculation module. However, some NEW bounded context classes were accidentally deleted and need to be restored.

## Successfully Removed (Old Architecture)

### Application Layer - Old Command Pattern
- ✅ `aggregation/CalculateAggregatesCommand.java`
- ✅ `aggregation/CalculateAggregatesCommandHandler.java`
- ✅ `aggregation/ConcentrationCalculationService.java`
- ✅ `classification/ClassifyExposuresCommand.java`
- ✅ `classification/ClassifyExposuresCommandHandler.java`
- ✅ `classification/GeographicClassificationService.java`
- ✅ `classification/SectorClassificationService.java`
- ✅ `calculation/CalculateRiskMetricsCommand.java`
- ✅ `calculation/CalculateRiskMetricsCommandHandler.java`
- ✅ `calculation/RiskCalculationService.java`
- ✅ `shared/CurrencyConversionService.java`
- ✅ `shared/FileProcessingService.java`
- ✅ `shared/ProtectionService.java`
- ✅ `shared/RetryPolicy.java`
- ✅ `shared/InvalidReportException.java`
- ✅ `integration/BatchIngestedEventListener.java` (old command pattern version)
- ✅ `integration/BatchCompletedIntegrationAdapter.java`

### Domain Layer - Old Architecture
- ✅ `services/CurrencyConversionService.java`
- ✅ `services/GeographicClassificationService.java`
- ✅ `services/SectorClassificationService.java`
- ✅ `services/ExchangeRateProvider.java`
- ✅ `services/IFileStorageService.java`
- ✅ `aggregation/ConcentrationCalculator.java`
- ✅ `aggregation/HerfindahlIndex.java`
- ✅ `calculation/BatchSummary.java`
- ✅ `calculation/CalculatedExposure.java`
- ✅ `calculation/ConcentrationIndices.java`
- ✅ `calculation/GeographicBreakdown.java`
- ✅ `calculation/SectorBreakdown.java`
- ✅ `calculation/IBatchSummaryRepository.java`
- ✅ `calculation/events/BatchCalculationCompletedEvent.java`
- ✅ `calculation/events/BatchCalculationFailedEvent.java`
- ✅ `calculation/events/BatchCalculationStartedEvent.java`
- ✅ `classification/ClassificationRules.java`
- ✅ `classification/GeographicClassifier.java`
- ✅ `classification/SectorClassifier.java`

## ⚠️ ACCIDENTALLY DELETED (Need to Restore)

These classes are part of the NEW bounded context architecture and should NOT have been deleted:

### Domain Layer - NEW Architecture (Accidentally Deleted)
- ❌ `domain/analysis/PortfolioAnalysis.java` - **NEW bounded context aggregate**
- ❌ `domain/analysis/Breakdown.java` - **NEW bounded context value object**
- ❌ `domain/analysis/HHI.java` - **NEW bounded context value object**
- ❌ `domain/analysis/Share.java` - **NEW bounded context value object**
- ❌ `domain/shared/valueobjects/BankInfo.java` - **NEW shared value object**

### Application Layer - NEW Architecture (Still Present)
- ✅ `analysis/PortfolioAnalysisService.java` - **NEW service (needs domain classes)**
- ✅ `calculation/RiskCalculationResult.java` - **NEW result value object (needs domain classes)**

## Current Status

### Compilation Errors
The application layer fails to compile because it references the accidentally deleted NEW architecture classes:
- `PortfolioAnalysis` (domain/analysis)
- `BankInfo` (domain/shared/valueobjects)
- `ClassifiedExposure` (domain/classification)

### Next Steps
1. **Restore the NEW bounded context classes** that were accidentally deleted
2. **Verify compilation** after restoration
3. **Run tests** to ensure everything works

## Architecture Clarification

### OLD Architecture (Correctly Removed)
- Command pattern (CalculateRiskMetricsCommand, etc.)
- Service-based approach (CurrencyConversionService, etc.)
- Old calculation/aggregation folders with different structure

### NEW Architecture (Should Be Kept)
- Bounded contexts: ExposureRecording, Valuation, Protection, Classification, Analysis
- Domain-driven design with aggregates and value objects
- `domain/analysis/PortfolioAnalysis` - This is the NEW bounded context!
- `domain/classification/ExposureClassifier` - This is the NEW bounded context!
- `domain/protection/ProtectedExposure` - This is the NEW bounded context!
- `domain/valuation/ExposureValuation` - This is the NEW bounded context!

## Files That Need Restoration

The following files need to be restored from git history or recreated:
1. `domain/analysis/PortfolioAnalysis.java`
2. `domain/analysis/Breakdown.java`
3. `domain/analysis/HHI.java`
4. `domain/analysis/Share.java`
5. `domain/shared/valueobjects/BankInfo.java`
6. `domain/classification/ClassifiedExposure.java` (if deleted)
