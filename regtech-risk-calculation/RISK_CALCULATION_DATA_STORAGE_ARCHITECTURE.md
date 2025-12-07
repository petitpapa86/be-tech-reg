# Risk Calculation Data Storage Architecture

## Overview

The Risk Calculation Module uses a **dual storage strategy**: structured data in PostgreSQL for querying and API access, plus JSON files in S3/filesystem for report generation. This document clarifies the data storage strategy and how the Report Generation Module consumes the results.

## Storage Strategy

### 1. Database Storage (Minimal - For Batch Tracking Only)

Only essential batch metadata is persisted to PostgreSQL:

1. **batches** - Batch metadata, processing status, and file URI
2. **portfolio_analysis** (optional) - Summary metrics for quick API access

**Purpose**: Track batch processing status and provide quick summary access

**What is NOT stored in database**:
- ❌ Individual exposures
- ❌ Mitigations
- ❌ Detailed exposure classifications

**Rationale**: Since complete results are in JSON files, storing individual exposures in the database is redundant. The JSON file serves as the single source of truth for detailed data.

### 2. File Storage (Secondary - For Report Generation)

Calculation results are also serialized to JSON and stored in S3/filesystem:

**Production**: `s3://risk-analysis-production/calculated/calc_batch_{batchId}.json`  
**Development**: `/data/calculated/calc_batch_{batchId}.json`

**Purpose**: Provide complete calculation results to Report Generation Module

**File storage provides**:
- Complete snapshot of calculation results
- Decoupled integration between modules
- Audit trail and historical analysis
- Backup and disaster recovery

## Data Flow Architecture

```
┌─────────────────────┐
│ Ingestion Module    │
│ (Stores raw JSON)   │
└──────────┬──────────┘
           │ BatchIngestedEvent
           ▼
┌─────────────────────┐
│ Risk Calculation    │
│ Module              │
│                     │
│ 1. Calculate        │
│ 2. Store in DB ✓    │
│    - batches        │
│    - exposures      │
│    - mitigations    │
│    - portfolio_     │
│      analysis       │
│ 3. Serialize to     │
│    JSON ✓           │
│ 4. Upload to S3/    │
│    filesystem ✓     │
└──────────┬──────────┘
           │ BatchCalculationCompletedEvent
           │ (includes S3 URI)
           ▼
┌─────────────────────┐
│ Report Generation   │
│ Module              │
│                     │
│ 1. Download JSON ✓  │
│    from S3/         │
│    filesystem       │
│ 2. Parse results    │
│ 3. Generate reports │
│ 4. Store HTML/XBRL  │
│    to S3/filesystem │
└─────────────────────┘
```

## JSON File Format

The calculation results JSON file contains complete exposure details and summary metrics:

```json
{
  "batch_id": "batch_20240331_001",
  "calculated_at": "2024-03-31T14:23:47Z",
  
  "summary": {
    "total_exposures": 8,
    "total_amount_eur": 2758075000.00,
    "geographic_breakdown": {
      "ITALY": { "amount": 1850000000.00, "percentage": 67.08 },
      "EU_OTHER": { "amount": 830000000.00, "percentage": 30.08 },
      "NON_EUROPEAN": { "amount": 78075000.00, "percentage": 2.84 }
    },
    "sector_breakdown": {
      "RETAIL_MORTGAGE": { "amount": 950000000.00, "percentage": 34.45 },
      "SOVEREIGN": { "amount": 578242500.00, "percentage": 20.97 },
      "CORPORATE": { "amount": 1150000000.00, "percentage": 41.70 },
      "BANKING": { "amount": 79832500.00, "percentage": 2.89 }
    },
    "concentration_indices": {
      "geographic_hhi": 0.5234,
      "geographic_concentration_level": "HIGH",
      "sector_hhi": 0.3891,
      "sector_concentration_level": "HIGH"
    }
  },
  
  "exposures": [
    {
      "exposure_id": "EXP_001",
      "client_name": "MUTUI CASA TRENTINO",
      "original_amount": 950000000.00,
      "original_currency": "EUR",
      "amount_eur": 950000000.00,
      "exchange_rate_used": null,
      "country": "IT",
      "geographic_region": "ITALY",
      "sector": "RETAIL_MORTGAGE",
      "sector_category": "RETAIL_MORTGAGE",
      "percentage_of_total": 34.45
    },
    {
      "exposure_id": "EXP_007",
      "client_name": "US TREASURY BONDS",
      "original_amount": 85000000.00,
      "original_currency": "USD",
      "amount_eur": 78242500.00,
      "exchange_rate_used": 0.9205,
      "country": "US",
      "geographic_region": "NON_EUROPEAN",
      "sector": "SOVEREIGN",
      "sector_category": "SOVEREIGN",
      "percentage_of_total": 2.84
    }
    // ... more exposures
  ]
}
```

## Report Generation Data Access

The Report Generation Module accesses risk calculation results through **file download**:

### 1. Event Reception

Receives `BatchCalculationCompletedEvent` with S3 URI:

```java
public record BatchCalculationCompletedEvent(
    String batchId,
    String calculationResultsUri,  // S3 URI or filesystem path
    BankInfo bankInfo,
    Instant completedAt
) {}
```

### 2. File Download

Downloads JSON file from storage:

```java
// Production: Download from S3
String json = fileStorageService.downloadFile(
    "s3://risk-analysis-production/calculated/calc_batch_" + batchId + ".json"
);

// Development: Read from local filesystem
String json = Files.readString(
    Path.of("/data/calculated/calc_batch_" + batchId + ".json")
);
```

### 3. JSON Parsing

Parses JSON to domain objects:

```java
CalculationResults results = objectMapper.readValue(
    json, 
    CalculationResults.class
);
```

### 4. Report Generation

Uses parsed data to generate HTML and XBRL reports

## Risk Calculation Module Implementation

### After Calculation Completes

The Risk Calculation Module performs these steps:

1. **Serialize to JSON** (complete results)
   ```java
   CalculationResults results = new CalculationResults(
       batchId,
       Instant.now(),
       summary,
       exposures  // All exposures with full details
   );
   
   String json = calculationResultsSerializer.serialize(results);
   ```

2. **Upload to Storage**
   ```java
   String uri = calculationResultsStorageService.store(
       batchId,
       json
   );
   // Returns: s3://risk-analysis-production/calculated/calc_batch_{id}.json
   ```

3. **Save Minimal Metadata to Database** (for tracking only)
   ```java
   // Only save batch metadata with file URI
   Batch batch = new Batch(
       batchId,
       bankInfo,
       reportDate,
       totalExposures,
       BatchStatus.COMPLETED,
       uri  // Store S3 URI for reference
   );
   batchRepository.save(batch);
   
   // Optionally save summary for quick API access
   portfolioAnalysisRepository.save(analysis);
   ```

4. **Publish Event with URI**
   ```java
   eventPublisher.publish(new BatchCalculationCompletedEvent(
       batchId,
       uri,  // Include S3 URI in event
       bankInfo,
       Instant.now()
   ));
   ```

## Simplified Database Schema

### batches Table (Required)

```sql
CREATE TABLE batches (
    batch_id VARCHAR(100) PRIMARY KEY,
    bank_name VARCHAR(255) NOT NULL,
    abi_code VARCHAR(10) NOT NULL,
    lei_code VARCHAR(20) NOT NULL,
    report_date DATE NOT NULL,
    total_exposures INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL,
    calculation_results_uri VARCHAR(500),  -- S3 URI to JSON file
    ingested_at TIMESTAMP NOT NULL,
    processed_at TIMESTAMP,
    
    INDEX idx_report_date (report_date),
    INDEX idx_status (status)
);
```

### portfolio_analysis Table (Optional - For Quick API Access)

```sql
CREATE TABLE portfolio_analysis (
    batch_id VARCHAR(100) PRIMARY KEY,
    total_portfolio_eur DECIMAL(20, 2) NOT NULL,
    
    -- Summary metrics only (full details in JSON)
    geographic_hhi DECIMAL(6, 4) NOT NULL,
    geographic_concentration_level VARCHAR(20) NOT NULL,
    sector_hhi DECIMAL(6, 4) NOT NULL,
    sector_concentration_level VARCHAR(20) NOT NULL,
    
    analyzed_at TIMESTAMP NOT NULL,
    
    FOREIGN KEY (batch_id) REFERENCES batches(batch_id)
);
```

### Tables NOT Needed

- ❌ **exposures** table - All exposure details are in JSON file
- ❌ **mitigations** table - All mitigation details are in JSON file

**Benefit**: Dramatically simplified database schema with no redundant data storage

## Event-Driven Integration

### BatchCalculationCompletedEvent

When risk calculation completes, the module publishes an event with the S3 URI:

```java
public record BatchCalculationCompletedEvent(
    String batchId,
    String calculationResultsUri,  // S3 URI to JSON file
    BankInfo bankInfo,
    EurAmount totalPortfolio,
    Instant completedAt
) {}
```

**Example Event**:
```json
{
  "batchId": "batch_20240331_001",
  "calculationResultsUri": "s3://risk-analysis-production/calculated/calc_batch_20240331_001.json",
  "bankInfo": {
    "bankName": "Banca Esempio",
    "abiCode": "12345",
    "leiCode": "EXAMPLE123456789012"
  },
  "totalPortfolio": 2758075000.00,
  "completedAt": "2024-03-31T14:23:47Z"
}
```

**Note**: The event includes the **S3 URI** so the Report Generation Module knows where to download the complete calculation results.

## Benefits of File-First Storage Strategy

### Why JSON Files are Primary Storage

1. **Single Source of Truth**: Complete calculation results in one place
2. **No Data Duplication**: Avoid storing same data in both DB and files
3. **Simplified Schema**: Minimal database tables needed
4. **Module Decoupling**: Report module gets everything from JSON
5. **Audit Trail**: Immutable files provide complete historical records
6. **Flexibility**: Easy to add new fields without schema migrations

### Why Minimal Database Storage

1. **Batch Tracking**: Quick status checks without parsing JSON
2. **Summary Access**: Fast API responses for dashboard metrics
3. **Query Performance**: Index on batch_id, status, report_date
4. **Operational Monitoring**: Track processing status and failures

### What This Eliminates

- ❌ Duplicate storage of exposure data
- ❌ Complex JPA entity mappings for exposures
- ❌ Database migrations for exposure schema changes
- ❌ Synchronization issues between DB and files
- ❌ Large database tables with millions of exposure records

## Comparison with Other Modules

| Module | Input Storage | Processing | Output Storage |
|--------|--------------|------------|----------------|
| Ingestion | S3/Filesystem (raw JSON) | Parse & validate | Database + S3/Filesystem (processed JSON) |
| Risk Calculation | Database (exposures) | Calculate metrics | Database + S3/Filesystem (calc JSON) |
| Data Quality | Database (exposures) | Validate rules | Database + S3/Filesystem (quality JSON) |
| Report Generation | S3/Filesystem (calc + quality JSON) | Generate reports | S3/Filesystem (HTML/XBRL) |

## Access Patterns

### Database is for:
- Batch status queries (`GET /api/risk-calculation/batches/{id}/status`)
- Summary metrics (`GET /api/risk-calculation/batches/{id}/summary`)
- Listing batches (`GET /api/risk-calculation/batches?status=COMPLETED`)
- Dashboard widgets (total batches, recent completions)

### File Storage is for:
- Report generation (download complete JSON)
- Detailed exposure queries (parse JSON on-demand)
- Audit and compliance (immutable records)
- Data export and archival
- Historical analysis

### When to Query JSON Files

If you need detailed exposure data:
```java
// Download JSON file
String json = fileStorageService.downloadFile(batch.getCalculationResultsUri());

// Parse to get exposures
CalculationResults results = objectMapper.readValue(json, CalculationResults.class);

// Filter/query in memory
List<Exposure> italianExposures = results.getExposures().stream()
    .filter(e -> "IT".equals(e.getCountry()))
    .toList();
```

**Note**: For frequent queries on exposure details, consider adding a caching layer or search index (e.g., Elasticsearch) rather than storing in relational database.

## Conclusion

The Risk Calculation Module follows a **file-first storage strategy**:

1. **JSON files** are the primary storage for complete calculation results
2. **Database** stores only minimal metadata for batch tracking and summary access

This architectural decision:

- **Eliminates data duplication** - no need to store exposures in both DB and files
- **Simplifies database schema** - only 1-2 tables instead of 4+
- **Reduces storage costs** - avoid millions of exposure records in PostgreSQL
- **Improves maintainability** - schema changes only affect JSON format
- **Enables module decoupling** - report module gets everything from JSON
- **Provides audit trails** - immutable JSON files serve as records

**The JSON file is the single source of truth** for detailed calculation results. The database is only for operational metadata and quick summary access.

### Migration Note

If you currently have `exposures` and `mitigations` tables, they can be **deprecated and removed** since all that data is now in the JSON files. This will:
- Free up significant database storage
- Simplify JPA entity management
- Reduce database migration complexity
- Speed up batch processing (fewer DB writes)

---

**Last Updated**: December 2024  
**Related Documents**:
- `.kiro/specs/risk-calculation-module/requirements.md`
- `.kiro/specs/risk-calculation-module/design.md`
- `.kiro/specs/report-generation-module/requirements.md`
