# Portfolio Analysis Reconstitution Fix

## Issue
The application was throwing an `UnsupportedOperationException` when trying to load `PortfolioAnalysis` entities from the database:

```
java.lang.UnsupportedOperationException: PortfolioAnalysis reconstitution from entity not yet implemented. 
Domain model needs a reconstitute factory method.
    at com.bcbs239.regtech.riskcalculation.infrastructure.database.mappers.PortfolioAnalysisMapper.toDomain(PortfolioAnalysisMapper.java:180)
```

This occurred in the `BatchIngestedEventListener` when it tried to find an existing portfolio analysis by batch ID.

## Root Cause
The `PortfolioAnalysisMapper.toDomain()` method was incomplete. While it could convert domain objects to entities (for saving), it couldn't reconstitute domain objects from entities (for loading from database).

The domain model only had an `analyze()` factory method for creating new analyses from raw exposure data, but lacked a `reconstitute()` method for recreating domain objects from persisted data.

## Solution

### 1. Added Reconstitute Factory Method
Added a new static factory method to `PortfolioAnalysis` domain model:

```java
public static PortfolioAnalysis reconstitute(
    String batchId,
    EurAmount totalPortfolio,
    Breakdown geographicBreakdown,
    Breakdown sectorBreakdown,
    HHI geographicHHI,
    HHI sectorHHI,
    Instant analyzedAt
)
```

This method allows the infrastructure layer to recreate domain objects from persisted data without recalculating metrics.

### 2. Implemented Mapper toDomain Method
Updated `PortfolioAnalysisMapper.toDomain()` to:
- Reconstruct geographic breakdown from entity fields
- Reconstruct sector breakdown from entity fields
- Reconstruct HHI values with concentration levels
- Call the new `reconstitute()` factory method

## Files Modified
1. `regtech-risk-calculation/domain/src/main/java/com/bcbs239/regtech/riskcalculation/domain/analysis/PortfolioAnalysis.java`
   - Added `reconstitute()` static factory method

2. `regtech-risk-calculation/infrastructure/src/main/java/com/bcbs239/regtech/riskcalculation/infrastructure/database/mappers/PortfolioAnalysisMapper.java`
   - Completed `toDomain()` implementation
   - Removed placeholder exception

## Testing
- Build successful: `mvn clean compile` passed
- No compilation errors
- Ready for integration testing

## Impact
This fix enables:
- Loading existing portfolio analyses from the database
- Resuming risk calculation workflows
- Querying historical analysis results
- Event listeners to check for existing analyses before creating new ones
