# Design Updates for Comprehensive Report Generation

## Key Changes from Original Design

### 1. Report Type Change
- **OLD**: Separate LARGE_EXPOSURES and DATA_QUALITY reports
- **NEW**: Single COMPREHENSIVE report combining both analyses

### 2. Event Coordination
- **NEW**: Dual event coordination - wait for BOTH events before generation
- **NEW**: BatchEventTracker component to track pending events
- **NEW**: ReportCoordinator domain service

### 3. Quality Recommendations
- **NEW**: QualityRecommendationsGenerator domain service
- **NEW**: Dynamic recommendations based on actual error patterns
- **NEW**: Contextual guidance instead of generic advice

### 4. HTML Report Structure
- **NEW**: Combined report with two main sections:
  - Large Exposures Analysis (existing)
  - Data Quality Analysis (new)
- **NEW**: Overall quality score badge in header
- **NEW**: Compliance status alongside bank information

### 5. Data Aggregation
- **NEW**: ComprehensiveReportDataAggregator
- **NEW**: Fetches BOTH calculation and quality JSON files
- **NEW**: Validates data consistency between sources

## Implementation Status

The updated design document content was provided in the context transfer.
The design is complete and ready for task creation.

