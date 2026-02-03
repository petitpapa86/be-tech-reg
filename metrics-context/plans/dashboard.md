## Header
All requests  include in the header: bank identifier:
```
X-Bank-Id: <bank_id>
```

## Standard Response Wrapper
the controller extends C:\Users\alseny\Desktop\react projects\regtech\regtech-core\presentation\src\main\java\com\bcbs239\regtech\core\presentation\controllers\BaseController.java

All API responses follow this standard format:

```json
{
  "success": boolean,
  "message": string,
  "messageKey": string (optional),
  "data": <response_data>,
  "type": string (optional),
  "meta": {
    "apiVersion": string,
    "version": string,
    "timestamp": string (ISO 8601)
  }
}
```

### Success Response Example
```json
{
  "success": true,
  "message": "Dashboard data retrieved successfully",
  "data": { /* actual data */ },
  "meta": {
    "apiVersion": "1.0.0",
    "version": "2024.1",
    "timestamp": "2026-02-01T10:30:00Z"
  }
}
```

### Error Response Example
```json
{
  "success": false,
  "message": "Failed to retrieve dashboard data",
  "messageKey": "DASHBOARD_ERROR",
  "meta": {
    "apiVersion": "1.0.0",
    "version": "2024.1",
    "timestamp": "2026-02-01T10:30:00Z"
  }
}
```

## Endpoints

### 1. Get Dashboard Data

**Endpoint:** `GET /metrics/dashboard`

**Description:** Retrieves comprehensive dashboard data including summary statistics, recent files, compliance metrics, reports, and last batch violations.

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `bankId` | string | Yes | Bank identifier |
| `size` | integer | No | Number of items per page (default: 10) |
| `page` | integer | No | Page number (default: 0) |

**Request Example:**
```http
GET /metrics/dashboard?bankId=BANK-XYZ&size=10&page=0
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
X-Bank-Id: BANK-XYZ
```

**Response Schema:**
```typescript
{
  success: boolean;
  message: string;
  data: {
    summary: {
      filesProcessed: number;      // Total number of files processed
      avgScore: number;             // Average quality score (0-100)
      violations: number;           // Total violations found
      reports: number;              // Total reports generated
    };
    files: [
      {
        id: string;                 // Unique file identifier
        filename: string;           // Name of the file
        date: string;               // Upload date (ISO 8601)
        score: number;              // Quality score (0-100)
        status: string;             // Status: "completed" | "processing" | "error"
        reportId: string | null;    // Associated report ID (if generated)
      }
    ];
    compliance: {
      overall: number;              // Overall compliance score (0-100)
      dataQuality: number;          // Data quality score (0-100)
      bcbs: number;                 // BCBS 239 compliance score (0-100)
      completeness: number;         // Completeness score (0-100)
    };
    reports: [
      {
        filename: string;           // Report filename
        status: string;             // Report status
        details: string;            // Report details/description
      }
    ];
    lastBatchViolations: {
      count: number;                // Number of violations in last batch
    };
  };
  meta: {
    apiVersion: string;
    version: string;
    timestamp: string;
  };
}
```

**Response Example:**
```json
{
  "success": true,
  "message": "Dashboard data retrieved successfully",
  "data": {
    "summary": {
      "filesProcessed": 247,
      "avgScore": 87.5,
      "violations": 23,
      "reports": 156
    },
    "files": [
      {
        "id": "file-2024-001",
        "filename": "exposures_january_2024.xlsx",
        "date": "2024-01-15T10:30:00Z",
        "score": 92.3,
        "status": "completed",
        "reportId": "report-2024-001"
      },
      {
        "id": "file-2024-002",
        "filename": "exposures_february_2024.xlsx",
        "date": "2024-02-01T14:20:00Z",
        "score": 88.7,
        "status": "processing",
        "reportId": null
      }
    ],
    "compliance": {
      "overall": 89.2,
      "dataQuality": 91.5,
      "bcbs": 87.8,
      "completeness": 93.1
    },
    "reports": [
      {
        "filename": "report_january_2024.pdf",
        "status": "COMPLETED",
        "details": "Monthly compliance report for January 2024"
      },
      {
        "filename": "report_december_2023.pdf",
        "status": "SENT",
        "details": "Monthly compliance report for December 2023"
      }
    ],
    "lastBatchViolations": {
      "count": 5
    }
  },
  "meta": {
    "apiVersion": "1.0.0",
    "version": "2024.1",
    "timestamp": "2026-02-01T10:30:00Z"
  }
}
```

---

entities involes 
reports => C:\Users\alseny\Desktop\react projects\regtech\metrics-context\metrics-infrastructure\src\main\java\com\bcbs239\regtech\metrics\infrastructure\entity\ComplianceReportEntity.java

compliance => C:\Users\alseny\Desktop\react projects\regtech\metrics-context\metrics-infrastructure\src\main\java\com\bcbs239\regtech\metrics\infrastructure\entity\DashboardMetricsEntity.java

files => C:\Users\alseny\Desktop\react projects\regtech\metrics-context\metrics-infrastructure\src\main\java\com\bcbs239\regtech\metrics\infrastructure\entity\FileEntity.java

repositories implementation are here C:\Users\alseny\Desktop\react projects\regtech\metrics-context\metrics-infrastructure\src\main\java\com\bcbs239\regtech\metrics\infrastructure\repository