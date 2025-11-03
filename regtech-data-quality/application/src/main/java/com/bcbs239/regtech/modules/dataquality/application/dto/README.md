# Data Quality Module DTOs and Mapping

This directory contains all the Data Transfer Objects (DTOs) and mapping functionality for the Data Quality Module, implementing task 4.3 requirements.

## Implemented DTOs

### 1. QualityScoresDto
- **Purpose**: Serializable representation of quality scores with all six dimensions
- **Features**:
  - Complete score breakdown (completeness, accuracy, consistency, timeliness, uniqueness, validity)
  - Overall score and quality grade
  - Compliance status and attention level indicators
  - Bidirectional mapping (fromDomain/toDomain)
  - Utility methods for dimension analysis

### 2. ValidationSummaryDto
- **Purpose**: Serializable representation of validation summary with error statistics
- **Features**:
  - Total/valid/invalid exposure counts
  - Error breakdown by dimension, severity, and code
  - Validation rate calculations
  - Top error codes (limited to 10 for performance)
  - Quality threshold checking

### 3. QualityReportDto
- **Purpose**: Complete quality report with score breakdown for API responses
- **Features**:
  - Full report metadata (ID, batch, bank, status, timestamps)
  - Embedded quality scores and validation summary
  - S3 details reference
  - Processing duration calculations
  - Status checking methods (completed, failed, compliant, etc.)
  - Human-readable summaries

### 4. QualityReportSummaryDto
- **Purpose**: Lightweight version for lists and trends (performance optimized)
- **Features**:
  - Essential report information without detailed validation results
  - Optimized for bulk operations and trend analysis
  - Brief summary generation

### 5. QualityTrendsDto
- **Purpose**: Quality trends analysis over time periods
- **Features**:
  - Aggregated statistics for all six dimensions
  - Compliance rate calculations
  - Trend direction analysis (improving/declining/stable)
  - Time period analysis
  - Builder pattern for flexible construction

## Mapping Methods

All DTOs implement comprehensive mapping functionality:

### Domain to DTO Mapping
- `fromDomain(domainObject)` - Static factory methods
- Null-safe conversions
- Enum to string conversions for serialization
- Nested object mapping

### DTO to Domain Mapping
- `toDomain()` - Instance methods where applicable
- String to enum conversions
- Validation of converted objects

### Utility Methods
- Empty/default object creation
- Status checking methods
- Calculation helpers (rates, percentages, durations)
- Human-readable formatting

## Requirements Coverage

This implementation covers all requirements from 4.1-4.7:

- **4.1**: Individual dimension scores (completeness, accuracy, consistency, timeliness, uniqueness, validity)
- **4.2**: Weighted overall score calculation
- **4.3**: Quality grade assignment (A+, A, B, C, F)
- **4.4**: Score thresholds and compliance checking
- **4.5**: Rule pass/fail tracking per dimension
- **4.6**: Error statistics and categorization
- **4.7**: Trending quality issue identification

## Design Patterns

- **Record Pattern**: Immutable DTOs using Java records
- **Factory Pattern**: Static factory methods for domain conversion
- **Builder Pattern**: Complex object construction (QualityTrendsDto)
- **Null Object Pattern**: Empty/default DTOs for safe handling

## Performance Considerations

- Lightweight summary DTOs for bulk operations
- Limited error code collections (top 10) to prevent memory bloat
- Lazy calculation of derived values
- Efficient enum conversions

## Testing

Comprehensive test coverage is provided in `DtoMappingTest.java` covering:
- Bidirectional mapping accuracy
- Null safety
- Utility method functionality
- Edge cases and boundary conditions