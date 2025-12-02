# Risk Calculation Presentation Layer - API Migration Guide

## Overview

This guide documents the migration from the old presentation layer DTOs and endpoints to the new bounded context-aligned architecture. The refactoring aligns the API with the domain model's bounded contexts: ExposureRecording, Valuation, Protection, Classification, and Analysis.

**Migration Timeline:**
- **Phase 1 (Current)**: New endpoints available alongside old endpoints
- **Phase 2 (Q1 2026)**: Old endpoints marked as deprecated
- **Phase 3 (Q2 2026)**: Old endpoints removed

## Breaking Changes Summary

### Removed DTOs
The following DTOs have been removed and replaced:

| Old DTO | New DTO(s) | Notes |
|---------|-----------|-------|
| `RiskReportDTO` | `PortfolioAnalysisResponseDTO` | Split into focused DTOs |
| `ExposureDTO` | `ClassifiedExposureDTO`, `ProtectedExposureDTO` | Separated by bounded context |
| `CreditRiskMitigationDTO` | `MitigationDTO` | Simplified structure |
| `BankInfoDTO` | Removed | Bank info now in domain layer |

### New Endpoints

All new endpoints follow the pattern: `/api/v1/risk-calculation/{resource}/{batchId}`

#### Portfolio Analysis Endpoints
- `GET /api/v1/risk-calculation/portfolio-analysis/{batchId}`
- `GET /api/v1/risk-calculation/portfolio-analysis/{batchId}/concentrations`
- `GET /api/v1/risk-calculation/portfolio-analysis/{batchId}/breakdowns`

#### Exposure Results Endpoints
- `GET /api/v1/risk-calculation/exposures/{batchId}/classified`
- `GET /api/v1/risk-calculation/exposures/{batchId}/protected`

#### Batch Status Endpoints
- `GET /api/v1/risk-calculation/batches/{batchId}/status`
- `GET /api/v1/risk-calculation/batches/{batchId}/progress`
- `GET /api/v1/risk-calculation/batches/active`


## DTO Migration Mapping

### 1. Portfolio Analysis Response

**Old Structure (RiskReportDTO):**
```json
{
  "batchId": "batch_123",
  "totalExposure": 1000000.00,
  "exposures": [...],
  "mitigations": [...]
}
```

**New Structure (PortfolioAnalysisResponseDTO):**
```json
{
  "batch_id": "batch_123",
  "total_portfolio_eur": 1000000.00,
  "processing_state": {
    "status": "COMPLETED",
    "phase": "ANALYSIS"
  },
  "processing_progress": {
    "total_exposures": 100,
    "processed_exposures": 100,
    "percentage_complete": 100.0
  },
  "concentration_indices": {
    "geographic_hhi": 0.25,
    "sector_hhi": 0.30,
    "geographic_risk_level": "MODERATE",
    "sector_risk_level": "MODERATE"
  },
  "geographic_breakdown": {
    "type": "GEOGRAPHIC",
    "shares": [...]
  },
  "sector_breakdown": {
    "type": "SECTOR",
    "shares": [...]
  },
  "timestamps": {
    "started_at": "2024-12-02T10:00:00Z",
    "completed_at": "2024-12-02T10:05:00Z"
  }
}
```

**Migration Notes:**
- Field names now use snake_case for consistency
- Processing state and progress are now separate nested objects
- Concentration indices are calculated and included
- Breakdowns are structured with type and shares
- Timestamps track processing lifecycle


### 2. Classified Exposure

**Old Structure (ExposureDTO):**
```json
{
  "id": "exp_123",
  "amount": 50000.00,
  "currency": "USD",
  "region": "EU",
  "sector": "RETAIL"
}
```

**New Structure (ClassifiedExposureDTO):**
```json
{
  "exposure_id": "exp_123",
  "net_exposure_eur": 47500.00,
  "geographic_region": "EU",
  "economic_sector": "RETAIL_MORTGAGE",
  "classification": {
    "counterparty_type": "CORPORATE",
    "asset_class": "LOANS",
    "risk_weight": 75
  }
}
```

**Migration Notes:**
- Amounts are now always in EUR (converted from original currency)
- Economic sector uses standardized enum values
- Classification metadata is now included
- Separate endpoint for classified vs protected exposures

### 3. Protected Exposure

**New Structure (ProtectedExposureDTO):**
```json
{
  "exposure_id": "exp_123",
  "gross_exposure_eur": 50000.00,
  "net_exposure_eur": 40000.00,
  "total_mitigation_eur": 10000.00,
  "mitigations": [
    {
      "mitigation_id": "mit_456",
      "type": "COLLATERAL",
      "value_eur": 10000.00,
      "coverage_ratio": 0.20
    }
  ],
  "has_mitigations": true,
  "fully_covered": false
}
```

**Migration Notes:**
- Clearly separates gross vs net exposure
- Includes mitigation details inline
- Provides boolean flags for quick checks
- Coverage ratio calculated automatically


### 4. Batch Status

**New Structure (BatchStatusResponseDTO):**
```json
{
  "batch_id": "batch_123",
  "bank_id": "bank_456",
  "processing_state": "COMPLETED",
  "current_phase": "ANALYSIS",
  "has_portfolio_analysis": true,
  "has_classified_exposures": true,
  "has_protected_exposures": true,
  "total_exposures": 100,
  "error_message": null,
  "started_at": "2024-12-02T10:00:00Z",
  "completed_at": "2024-12-02T10:05:00Z"
}
```

**Migration Notes:**
- Provides clear flags for available results
- Includes error information when applicable
- Tracks processing phases explicitly

## API Endpoint Migration

### Portfolio Analysis

#### Get Complete Portfolio Analysis

**Endpoint:** `GET /api/v1/risk-calculation/portfolio-analysis/{batchId}`

**Example Request:**
```bash
curl -X GET "https://api.example.com/api/v1/risk-calculation/portfolio-analysis/batch_123" \
  -H "Authorization: Bearer {token}" \
  -H "Accept: application/json"
```

**Example Response:**
```json
{
  "batch_id": "batch_123",
  "total_portfolio_eur": 5000000.00,
  "processing_state": {
    "status": "COMPLETED",
    "phase": "ANALYSIS"
  },
  "concentration_indices": {
    "geographic_hhi": 0.25,
    "sector_hhi": 0.30,
    "geographic_risk_level": "MODERATE",
    "sector_risk_level": "MODERATE"
  },
  "timestamps": {
    "started_at": "2024-12-02T10:00:00Z",
    "completed_at": "2024-12-02T10:05:00Z"
  }
}
```

**Status Codes:**
- `200 OK`: Portfolio analysis retrieved successfully
- `404 Not Found`: Batch not found or analysis not available
- `401 Unauthorized`: Missing or invalid authentication
- `403 Forbidden`: Insufficient permissions


#### Get Concentration Indices

**Endpoint:** `GET /api/v1/risk-calculation/portfolio-analysis/{batchId}/concentrations`

**Example Request:**
```bash
curl -X GET "https://api.example.com/api/v1/risk-calculation/portfolio-analysis/batch_123/concentrations" \
  -H "Authorization: Bearer {token}"
```

**Example Response:**
```json
{
  "geographic_hhi": 0.25,
  "sector_hhi": 0.30,
  "geographic_risk_level": "MODERATE",
  "sector_risk_level": "MODERATE"
}
```

#### Get Breakdowns

**Endpoint:** `GET /api/v1/risk-calculation/portfolio-analysis/{batchId}/breakdowns?type={GEOGRAPHIC|SECTOR}`

**Query Parameters:**
- `type` (optional): Filter by breakdown type ("GEOGRAPHIC" or "SECTOR")

**Example Request:**
```bash
curl -X GET "https://api.example.com/api/v1/risk-calculation/portfolio-analysis/batch_123/breakdowns?type=GEOGRAPHIC" \
  -H "Authorization: Bearer {token}"
```

**Example Response:**
```json
{
  "type": "GEOGRAPHIC",
  "shares": [
    {
      "category": "EU",
      "amount_eur": 2500000.00,
      "percentage": 50.0
    },
    {
      "category": "US",
      "amount_eur": 1500000.00,
      "percentage": 30.0
    },
    {
      "category": "ASIA",
      "amount_eur": 1000000.00,
      "percentage": 20.0
    }
  ]
}
```


### Exposure Results

#### Get Classified Exposures (Paginated)

**Endpoint:** `GET /api/v1/risk-calculation/exposures/{batchId}/classified`

**Query Parameters:**
- `sector` (optional): Filter by economic sector (e.g., "RETAIL_MORTGAGE", "CORPORATE", "SOVEREIGN")
- `page` (optional): Page number (0-indexed), default: 0
- `size` (optional): Page size (1-100), default: 20

**Example Request:**
```bash
curl -X GET "https://api.example.com/api/v1/risk-calculation/exposures/batch_123/classified?sector=RETAIL_MORTGAGE&page=0&size=20" \
  -H "Authorization: Bearer {token}"
```

**Example Response:**
```json
{
  "content": [
    {
      "exposure_id": "exp_123",
      "net_exposure_eur": 47500.00,
      "geographic_region": "EU",
      "economic_sector": "RETAIL_MORTGAGE",
      "classification": {
        "counterparty_type": "RETAIL",
        "asset_class": "LOANS",
        "risk_weight": 35
      }
    }
  ],
  "page": 0,
  "size": 20,
  "total_elements": 45,
  "total_pages": 3,
  "is_first": true,
  "is_last": false
}
```

**Status Codes:**
- `200 OK`: Exposures retrieved successfully (may be empty list)
- `400 Bad Request`: Invalid query parameters
- `404 Not Found`: Batch not found
- `401 Unauthorized`: Missing or invalid authentication


#### Get Protected Exposures (Paginated)

**Endpoint:** `GET /api/v1/risk-calculation/exposures/{batchId}/protected`

**Query Parameters:**
- `page` (optional): Page number (0-indexed), default: 0
- `size` (optional): Page size (1-100), default: 20

**Example Request:**
```bash
curl -X GET "https://api.example.com/api/v1/risk-calculation/exposures/batch_123/protected?page=0&size=20" \
  -H "Authorization: Bearer {token}"
```

**Example Response:**
```json
{
  "content": [
    {
      "exposure_id": "exp_123",
      "gross_exposure_eur": 50000.00,
      "net_exposure_eur": 40000.00,
      "total_mitigation_eur": 10000.00,
      "mitigations": [
        {
          "mitigation_id": "mit_456",
          "type": "COLLATERAL",
          "value_eur": 10000.00,
          "coverage_ratio": 0.20
        }
      ],
      "has_mitigations": true,
      "fully_covered": false
    }
  ],
  "page": 0,
  "size": 20,
  "total_elements": 30,
  "total_pages": 2,
  "is_first": true,
  "is_last": false
}
```

### Batch Status

#### Get Batch Status

**Endpoint:** `GET /api/v1/risk-calculation/batches/{batchId}/status`

**Example Request:**
```bash
curl -X GET "https://api.example.com/api/v1/risk-calculation/batches/batch_123/status" \
  -H "Authorization: Bearer {token}"
```

**Example Response:**
```json
{
  "batch_id": "batch_123",
  "bank_id": "bank_456",
  "processing_state": "COMPLETED",
  "current_phase": "ANALYSIS",
  "has_portfolio_analysis": true,
  "has_classified_exposures": true,
  "has_protected_exposures": true,
  "total_exposures": 100,
  "error_message": null,
  "started_at": "2024-12-02T10:00:00Z",
  "completed_at": "2024-12-02T10:05:00Z"
}
```


#### Get Processing Progress

**Endpoint:** `GET /api/v1/risk-calculation/batches/{batchId}/progress`

**Example Request:**
```bash
curl -X GET "https://api.example.com/api/v1/risk-calculation/batches/batch_123/progress" \
  -H "Authorization: Bearer {token}"
```

**Example Response:**
```json
{
  "total_exposures": 100,
  "processed_exposures": 75,
  "percentage_complete": 75.0,
  "current_phase": "CLASSIFICATION",
  "estimated_completion": "2024-12-02T10:08:00Z"
}
```

#### Get Active Batches

**Endpoint:** `GET /api/v1/risk-calculation/batches/active?bankId={bankId}`

**Query Parameters:**
- `bankId` (optional): Filter by bank ID

**Example Request:**
```bash
curl -X GET "https://api.example.com/api/v1/risk-calculation/batches/active?bankId=bank_456" \
  -H "Authorization: Bearer {token}"
```

**Example Response:**
```json
{
  "total_count": 2,
  "bank_id_filter": "bank_456",
  "batches": [
    {
      "batch_id": "batch_123",
      "bank_id": "bank_456",
      "processing_state": "IN_PROGRESS",
      "current_phase": "VALUATION",
      "started_at": "2024-12-02T10:00:00Z"
    },
    {
      "batch_id": "batch_124",
      "bank_id": "bank_456",
      "processing_state": "IN_PROGRESS",
      "current_phase": "CLASSIFICATION",
      "started_at": "2024-12-02T09:45:00Z"
    }
  ]
}
```


## Common Use Cases

### Use Case 1: Check Batch Calculation Status

**Scenario:** You want to check if a batch calculation is complete before retrieving results.

**Steps:**
1. Call the batch status endpoint
2. Check the `processing_state` field
3. If `COMPLETED`, proceed to retrieve results

**Example Code (JavaScript):**
```javascript
async function checkBatchStatus(batchId) {
  const response = await fetch(
    `https://api.example.com/api/v1/risk-calculation/batches/${batchId}/status`,
    {
      headers: {
        'Authorization': `Bearer ${token}`,
        'Accept': 'application/json'
      }
    }
  );
  
  if (!response.ok) {
    throw new Error(`Failed to get batch status: ${response.status}`);
  }
  
  const status = await response.json();
  
  if (status.processing_state === 'COMPLETED') {
    console.log('Batch calculation complete!');
    return true;
  } else if (status.processing_state === 'FAILED') {
    console.error('Batch calculation failed:', status.error_message);
    return false;
  } else {
    console.log(`Batch in progress: ${status.current_phase}`);
    return false;
  }
}
```

### Use Case 2: Retrieve Portfolio Analysis with Concentration Risk

**Scenario:** You want to get the complete portfolio analysis including concentration indices.

**Example Code (Python):**
```python
import requests

def get_portfolio_analysis(batch_id, token):
    url = f"https://api.example.com/api/v1/risk-calculation/portfolio-analysis/{batch_id}"
    headers = {
        "Authorization": f"Bearer {token}",
        "Accept": "application/json"
    }
    
    response = requests.get(url, headers=headers)
    response.raise_for_status()
    
    analysis = response.json()
    
    # Check concentration risk
    geo_hhi = analysis['concentration_indices']['geographic_hhi']
    sector_hhi = analysis['concentration_indices']['sector_hhi']
    
    print(f"Geographic HHI: {geo_hhi} ({analysis['concentration_indices']['geographic_risk_level']})")
    print(f"Sector HHI: {sector_hhi} ({analysis['concentration_indices']['sector_risk_level']})")
    
    return analysis
```


### Use Case 3: Paginate Through Classified Exposures

**Scenario:** You want to retrieve all classified exposures for a batch, handling pagination.

**Example Code (Java):**
```java
public List<ClassifiedExposureDTO> getAllClassifiedExposures(String batchId, String token) {
    List<ClassifiedExposureDTO> allExposures = new ArrayList<>();
    int page = 0;
    int size = 100; // Max page size
    boolean hasMore = true;
    
    while (hasMore) {
        String url = String.format(
            "https://api.example.com/api/v1/risk-calculation/exposures/%s/classified?page=%d&size=%d",
            batchId, page, size
        );
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Authorization", "Bearer " + token)
            .header("Accept", "application/json")
            .GET()
            .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            throw new RuntimeException("Failed to retrieve exposures: " + response.statusCode());
        }
        
        PagedResponse<ClassifiedExposureDTO> pagedResponse = 
            objectMapper.readValue(response.body(), 
                new TypeReference<PagedResponse<ClassifiedExposureDTO>>() {});
        
        allExposures.addAll(pagedResponse.getContent());
        
        hasMore = !pagedResponse.isLast();
        page++;
    }
    
    return allExposures;
}
```

### Use Case 4: Filter Exposures by Economic Sector

**Scenario:** You want to retrieve only retail mortgage exposures.

**Example Code (cURL):**
```bash
# Get first page of retail mortgage exposures
curl -X GET "https://api.example.com/api/v1/risk-calculation/exposures/batch_123/classified?sector=RETAIL_MORTGAGE&page=0&size=50" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Accept: application/json"
```

**Available Sector Values:**
- `RETAIL_MORTGAGE`
- `RETAIL_REVOLVING`
- `RETAIL_OTHER`
- `CORPORATE`
- `SOVEREIGN`
- `BANK`
- `EQUITY`
- `OTHER`


### Use Case 5: Monitor Active Batches for a Bank

**Scenario:** You want to monitor all active calculations for a specific bank.

**Example Code (TypeScript):**
```typescript
interface BatchStatus {
  batch_id: string;
  bank_id: string;
  processing_state: string;
  current_phase: string;
  started_at: string;
}

async function monitorActiveBatches(bankId: string, token: string): Promise<BatchStatus[]> {
  const url = `https://api.example.com/api/v1/risk-calculation/batches/active?bankId=${bankId}`;
  
  const response = await fetch(url, {
    headers: {
      'Authorization': `Bearer ${token}`,
      'Accept': 'application/json'
    }
  });
  
  if (!response.ok) {
    throw new Error(`Failed to get active batches: ${response.status}`);
  }
  
  const data = await response.json();
  
  console.log(`Found ${data.total_count} active batches for bank ${bankId}`);
  
  data.batches.forEach((batch: BatchStatus) => {
    console.log(`Batch ${batch.batch_id}: ${batch.current_phase} (started ${batch.started_at})`);
  });
  
  return data.batches;
}
```

## Error Handling

### Standard Error Response Format

All errors follow a consistent format:

```json
{
  "error": "Error type",
  "message": "Human-readable error message",
  "timestamp": "2024-12-02T10:00:00Z",
  "path": "/api/v1/risk-calculation/portfolio-analysis/batch_123"
}
```

### Common Error Codes

| Status Code | Error Type | Description | Resolution |
|-------------|-----------|-------------|------------|
| 400 | Bad Request | Invalid request parameters | Check query parameters and request body |
| 401 | Unauthorized | Missing or invalid authentication | Provide valid Bearer token |
| 403 | Forbidden | Insufficient permissions | Verify user has RISK_CALCULATION_READ permission |
| 404 | Not Found | Batch or resource not found | Verify batch ID exists and calculation is complete |
| 202 | Accepted | Calculation not yet complete | Poll batch status endpoint until complete |
| 500 | Internal Server Error | Server-side error | Contact support with error details |


### Error Handling Examples

**Example 1: Handling 404 Not Found**
```javascript
async function getPortfolioAnalysis(batchId, token) {
  try {
    const response = await fetch(
      `https://api.example.com/api/v1/risk-calculation/portfolio-analysis/${batchId}`,
      {
        headers: {
          'Authorization': `Bearer ${token}`,
          'Accept': 'application/json'
        }
      }
    );
    
    if (response.status === 404) {
      console.error('Portfolio analysis not found. Batch may not exist or calculation not complete.');
      return null;
    }
    
    if (!response.ok) {
      const error = await response.json();
      throw new Error(`API Error: ${error.message}`);
    }
    
    return await response.json();
  } catch (error) {
    console.error('Failed to retrieve portfolio analysis:', error);
    throw error;
  }
}
```

**Example 2: Handling 202 Accepted (Calculation in Progress)**
```python
import time
import requests

def wait_for_calculation_complete(batch_id, token, max_wait_seconds=300):
    """Poll batch status until calculation is complete or timeout."""
    url = f"https://api.example.com/api/v1/risk-calculation/batches/{batch_id}/status"
    headers = {"Authorization": f"Bearer {token}"}
    
    start_time = time.time()
    
    while time.time() - start_time < max_wait_seconds:
        response = requests.get(url, headers=headers)
        
        if response.status_code == 404:
            raise ValueError(f"Batch {batch_id} not found")
        
        response.raise_for_status()
        status = response.json()
        
        if status['processing_state'] == 'COMPLETED':
            print(f"Calculation complete for batch {batch_id}")
            return True
        elif status['processing_state'] == 'FAILED':
            raise RuntimeError(f"Calculation failed: {status.get('error_message')}")
        
        print(f"Calculation in progress: {status['current_phase']} "
              f"({status.get('percentage_complete', 0)}%)")
        time.sleep(5)  # Poll every 5 seconds
    
    raise TimeoutError(f"Calculation did not complete within {max_wait_seconds} seconds")
```


## Security and Authentication

### Required Permissions

All risk calculation endpoints require the `RISK_CALCULATION_READ` permission.

**Example Authorization Header:**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### Rate Limiting

API endpoints are rate-limited to prevent abuse:
- **Portfolio Analysis endpoints**: 100 requests per minute per user
- **Exposure Results endpoints**: 200 requests per minute per user (due to pagination)
- **Batch Status endpoints**: 300 requests per minute per user

**Rate Limit Headers:**
```
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 95
X-RateLimit-Reset: 1638446400
```

## Performance Considerations

### Response Times

Expected response times under normal load:

| Endpoint Type | Expected Response Time |
|--------------|----------------------|
| Batch Status | < 50ms |
| Portfolio Analysis | < 200ms |
| Concentration Indices | < 100ms |
| Breakdowns | < 150ms |
| Classified Exposures (paginated) | < 300ms |
| Protected Exposures (paginated) | < 300ms |

### Pagination Best Practices

1. **Use appropriate page sizes**: Default is 20, maximum is 100
2. **Cache results**: Results are immutable once calculation is complete
3. **Parallel requests**: You can request multiple pages in parallel for faster retrieval
4. **Filter early**: Use sector filters to reduce result set size

**Example: Parallel Page Retrieval**
```javascript
async function getAllExposuresParallel(batchId, token, totalPages) {
  const pagePromises = [];
  
  for (let page = 0; page < totalPages; page++) {
    pagePromises.push(
      fetch(
        `https://api.example.com/api/v1/risk-calculation/exposures/${batchId}/classified?page=${page}&size=100`,
        {
          headers: {
            'Authorization': `Bearer ${token}`,
            'Accept': 'application/json'
          }
        }
      ).then(r => r.json())
    );
  }
  
  const results = await Promise.all(pagePromises);
  return results.flatMap(r => r.content);
}
```


## Migration Checklist

Use this checklist to ensure a smooth migration:

### Phase 1: Preparation (Current)
- [ ] Review this migration guide
- [ ] Identify all code using old DTOs (`RiskReportDTO`, `ExposureDTO`, etc.)
- [ ] Map old endpoints to new endpoints
- [ ] Update API client libraries to support new endpoints
- [ ] Test new endpoints in development environment
- [ ] Update integration tests

### Phase 2: Implementation (Q1 2026)
- [ ] Update application code to use new DTOs
- [ ] Implement error handling for new error formats
- [ ] Update pagination logic for exposure endpoints
- [ ] Add support for new query parameters (sector filtering, etc.)
- [ ] Update monitoring and logging to track new endpoints
- [ ] Deploy changes to staging environment
- [ ] Conduct end-to-end testing

### Phase 3: Validation (Q1 2026)
- [ ] Verify all functionality works with new endpoints
- [ ] Compare results between old and new endpoints (if still available)
- [ ] Performance test new endpoints under load
- [ ] Update documentation and runbooks
- [ ] Train team on new API structure

### Phase 4: Cutover (Q2 2026)
- [ ] Deploy to production
- [ ] Monitor error rates and performance
- [ ] Remove old endpoint references from code
- [ ] Archive old DTO classes
- [ ] Update API documentation to remove old endpoints

## Support and Resources

### Documentation
- **API Reference**: `/docs/api/risk-calculation`
- **OpenAPI Spec**: `/api/v1/risk-calculation/openapi.json`
- **Postman Collection**: Available in project repository

### Contact
- **Technical Support**: support@example.com
- **API Questions**: api-team@example.com
- **Slack Channel**: #risk-calculation-api

### Changelog

#### Version 2.0.0 (December 2024)
- Complete refactoring of presentation layer
- New bounded context-aligned DTOs
- Improved error handling and validation
- Added pagination support for exposure endpoints
- Added concentration indices and breakdowns
- Enhanced batch status tracking

#### Version 1.0.0 (Previous)
- Initial implementation with `RiskReportDTO`
- Basic exposure and mitigation DTOs
- Simple batch status endpoint

---

**Last Updated**: December 2, 2024  
**Document Version**: 1.0  
**API Version**: 2.0.0
