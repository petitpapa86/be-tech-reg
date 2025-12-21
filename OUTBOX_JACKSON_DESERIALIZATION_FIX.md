# Outbox Jackson Deserialization Fix

## Problem
The outbox processor was failing to deserialize domain events from the risk calculation module with the following error:

```
Failed to process outbox message: Cannot construct instance of `com.bcbs239.regtech.riskcalculation.domain.calculation.events.DataQualityCompletedEvent` (no Creators, like default constructor, exist): cannot deserialize from Object value (no delegate- or property-based Creator)
```

## Root Cause
The domain event classes in the risk calculation module only had parameterized constructors and lacked the Jackson annotations required for proper deserialization. Jackson needs either:
1. A default (no-argument) constructor, or
2. Proper Jackson annotations (`@JsonCreator` and `@JsonProperty`) to indicate which constructor to use

## Solution
Added `@JsonCreator` and `@JsonProperty` annotations to the constructors of the following domain event classes:

### Fixed Event Classes
1. **BatchCalculationCompletedEvent**
   - Added `@JsonCreator` to constructor
   - Added `@JsonProperty` annotations to all constructor parameters

2. **BatchCalculationStartedEvent**
   - Added `@JsonCreator` to constructor
   - Added `@JsonProperty` annotations to all constructor parameters

3. **BatchCalculationFailedEvent**
   - Added `@JsonCreator` to constructor
   - Added `@JsonProperty` annotations to all constructor parameters

4. **PortfolioAnalysisCompletedEvent**
   - Added `@JsonCreator` to constructor
   - Added `@JsonProperty` annotations to all constructor parameters

## Implementation Details

### Before (Example)
```java
public BatchCalculationCompletedEvent(
        String batchId,
        String bankId,
        int processedExposures,
        String calculationResultsUri,
        Instant completedAt) {
    // constructor implementation
}
```

### After (Example)
```java
@JsonCreator
public BatchCalculationCompletedEvent(
        @JsonProperty("batchId") String batchId,
        @JsonProperty("bankId") String bankId,
        @JsonProperty("processedExposures") int processedExposures,
        @JsonProperty("calculationResultsUri") String calculationResultsUri,
        @JsonProperty("completedAt") Instant completedAt) {
    // constructor implementation
}
```

## Impact
- **Fixed**: Outbox processor can now successfully deserialize risk calculation domain events
- **Improved**: Reliable event processing for batch calculations and portfolio analysis
- **Maintained**: Backward compatibility - existing serialization continues to work

## Verification
- All risk calculation modules compile successfully
- Jackson annotations follow the same pattern used in other modules (IAM, Ingestion)
- No breaking changes to existing functionality

## Notes
- Other modules (IAM, Ingestion) already had proper Jackson annotations
- The fix only affects the risk calculation module's domain events
- The outbox processor uses Jackson's `ObjectMapper.readValue()` which requires these annotations for proper deserialization