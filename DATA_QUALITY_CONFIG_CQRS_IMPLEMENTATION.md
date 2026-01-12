# Data Quality Configuration - CQRS Implementation

## Overview

This document describes the complete implementation of the Data Quality Configuration API using the **CQRS (Command Query Responsibility Segregation)** pattern. Each operation has its own dedicated handler following Clean Architecture principles.

## Architecture

### CQRS Pattern
```
┌─────────────────────────────────────────────────────────────┐
│                    Presentation Layer                        │
│  DataQualityConfigController (Functional Endpoints)          │
│                                                              │
│  GET    /api/v1/data-quality/config                         │
│  PUT    /api/v1/data-quality/config                         │
│  POST   /api/v1/data-quality/config/reset                   │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ↓
┌─────────────────────────────────────────────────────────────┐
│                   Application Layer                          │
│                                                              │
│  ┌────────────────────────────────────────────────┐         │
│  │  QUERY HANDLER (READ)                          │         │
│  │  GetConfigurationQueryHandler                  │         │
│  │  - Fetches from repositories                   │         │
│  │  - Combines threshold + rule data              │         │
│  │  - Returns immutable ConfigurationDto          │         │
│  └────────────────────────────────────────────────┘         │
│                                                              │
│  ┌────────────────────────────────────────────────┐         │
│  │  COMMAND HANDLERS (WRITE)                      │         │
│  │                                                 │         │
│  │  UpdateConfigurationCommandHandler             │         │
│  │  - Validates thresholds (0.0 - 1.0 range)      │         │
│  │  - Updates QualityThreshold entity             │         │
│  │  - Persists to repository                      │         │
│  │  - Delegates to query handler for response     │         │
│  │                                                 │         │
│  │  ResetConfigurationCommandHandler              │         │
│  │  - Creates default thresholds                  │         │
│  │  - Persists to repository                      │         │
│  │  - Delegates to query handler for response     │         │
│  └────────────────────────────────────────────────┘         │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ↓
┌─────────────────────────────────────────────────────────────┐
│                  Infrastructure Layer                        │
│  QualityThresholdRepositoryAdapter (JPA)                    │
│  IBusinessRuleRepository                                     │
└─────────────────────────────────────────────────────────────┘
```

## File Structure

```
regtech-data-quality/
├── presentation/
│   └── config/
│       ├── DataQualityConfigController.java    # REST API boundary
│       └── DataQualityConfigRoutes.java        # Router configuration
│
├── application/
│   └── config/
│       ├── ConfigurationDto.java               # Strong DTOs
│       │   ├── ThresholdsDto
│       │   ├── ValidationDto
│       │   ├── ErrorHandlingDto
│       │   └── ConfigurationStatusDto
│       │
│       ├── GetConfigurationQuery.java          # Query record
│       ├── GetConfigurationQueryHandler.java   # READ handler
│       │
│       ├── UpdateConfigurationCommand.java     # Command record
│       ├── UpdateConfigurationCommandHandler.java  # WRITE handler
│       │
│       ├── ResetConfigurationCommand.java      # Command record
│       └── ResetConfigurationCommandHandler.java   # WRITE handler
│
└── domain/
    └── rulesengine/
        └── QualityThreshold.java               # Domain entity
```

## API Endpoints

### 1. GET Configuration
**Endpoint**: `GET /api/v1/data-quality/config?bankId=xxx`

**Handler**: `GetConfigurationQueryHandler`

**Response**:
```json
{
  "thresholds": {
    "completeness": 0.95,
    "accuracy": 0.98,
    "timeliness": 0.90,
    "consistency": 0.92
  },
  "validation": {
    "enabledRules": [
      "COMPLETENESS_CHECK",
      "ACCURACY_VALIDATION",
      "TIMELINESS_THRESHOLD",
      "CONSISTENCY_VALIDATION"
    ],
    "ruleCount": 4,
    "criticalRules": [
      "COMPLETENESS_CHECK"
    ]
  },
  "errorHandling": {
    "stopOnCriticalError": true,
    "maxErrorsBeforeStop": 100,
    "alertThreshold": 50
  },
  "status": {
    "enabled": true,
    "lastModified": "2024-01-12T22:30:00Z",
    "modifiedBy": "system",
    "configVersion": "1.0"
  }
}
```

### 2. UPDATE Configuration
**Endpoint**: `PUT /api/v1/data-quality/config?bankId=xxx`

**Handler**: `UpdateConfigurationCommandHandler`

**Request Body**:
```json
{
  "thresholds": {
    "completeness": 0.97,
    "accuracy": 0.99,
    "timeliness": 0.92,
    "consistency": 0.94
  }
}
```

**Validation**:
- All thresholds must be between 0.0 and 1.0 (inclusive)
- Invalid values return `BAD_REQUEST` with error message

**Response**: Same as GET Configuration (updated values)

### 3. RESET Configuration
**Endpoint**: `POST /api/v1/data-quality/config/reset?bankId=xxx`

**Handler**: `ResetConfigurationCommandHandler`

**Response**: Same as GET Configuration (default values)

**Default Thresholds**:
- `completeness`: 0.95 (95%)
- `accuracy`: 0.98 (98%)
- `timeliness`: 0.90 (90%)
- `consistency`: 0.92 (92%)

## Permission Requirements

All endpoints require the `DATA_QUALITY_CONFIGURE` permission:

```java
// In DataQualityConfigRoutes.java
RouterAttributes.withAttributes()
    .tag(Tags.CONFIGURATION)
    .permission("DATA_QUALITY_CONFIGURE")
```

## CQRS Handler Details

### Query Handler (READ)
```java
@Component
@Observed(name = "data-quality.config.query")
public class GetConfigurationQueryHandler {
    
    private final QualityThresholdRepository thresholdRepository;
    private final IBusinessRuleRepository businessRuleRepository;
    
    public Result<ConfigurationDto> handle(GetConfigurationQuery query) {
        // 1. Fetch thresholds from repository (or use defaults)
        // 2. Fetch business rules metadata
        // 3. Build ConfigurationDto
        // 4. Return Result.success(dto)
    }
}
```

**Characteristics**:
- ✅ Read-only operations
- ✅ No side effects
- ✅ Returns immutable ConfigurationDto
- ✅ Fetches from multiple repositories
- ✅ Can be cached (future optimization)

### Command Handler (WRITE - Update)
```java
@Component
@Observed(name = "data-quality.config.update")
public class UpdateConfigurationCommandHandler {
    
    private final QualityThresholdRepository thresholdRepository;
    private final GetConfigurationQueryHandler queryHandler;
    
    public Result<ConfigurationDto> handle(UpdateConfigurationCommand command) {
        // 1. Validate threshold ranges (0.0 - 1.0)
        // 2. Create/update QualityThreshold entity
        // 3. Persist to repository
        // 4. Delegate to query handler for response
    }
}
```

**Characteristics**:
- ✅ Write operation (persists changes)
- ✅ Domain validation (threshold range checks)
- ✅ Uses Result pattern for error handling
- ✅ Delegates to query handler for consistent response
- ✅ Idempotent (can be called multiple times with same result)

### Command Handler (WRITE - Reset)
```java
@Component
@Observed(name = "data-quality.config.reset")
public class ResetConfigurationCommandHandler {
    
    private final QualityThresholdRepository thresholdRepository;
    private final GetConfigurationQueryHandler queryHandler;
    
    public Result<ConfigurationDto> handle(ResetConfigurationCommand command) {
        // 1. Create default QualityThreshold entity
        // 2. Persist to repository
        // 3. Delegate to query handler for response
    }
}
```

**Characteristics**:
- ✅ Write operation (persists defaults)
- ✅ No validation needed (defaults are always valid)
- ✅ Delegates to query handler for consistent response
- ✅ Idempotent

## Benefits of CQRS Pattern

### Separation of Concerns
- **Query Handler**: Focused on efficient data retrieval
- **Command Handlers**: Focused on business logic and validation

### Testability
Each handler can be unit tested independently:
```java
@Test
void testGetConfiguration() {
    // Mock repositories
    // Call GetConfigurationQueryHandler.handle()
    // Assert ConfigurationDto structure
}

@Test
void testUpdateConfiguration_InvalidThreshold() {
    // Create command with invalid threshold (1.5)
    // Call UpdateConfigurationCommandHandler.handle()
    // Assert Result.isFailure()
}
```

### Scalability
- Query handlers can be cached
- Command handlers can be queued for async processing
- Separate read/write databases (future optimization)

### Maintainability
- Each operation has single responsibility
- Easy to add new operations (new handler + route)
- Clear boundaries between read and write operations

## Testing

### Manual Testing with curl

```bash
# 1. Get current configuration
curl -X GET "http://localhost:8080/api/v1/data-quality/config?bankId=bank-001" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"

# 2. Update configuration
curl -X PUT "http://localhost:8080/api/v1/data-quality/config?bankId=bank-001" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "thresholds": {
      "completeness": 0.97,
      "accuracy": 0.99,
      "timeliness": 0.92,
      "consistency": 0.94
    }
  }'

# 3. Reset to defaults
curl -X POST "http://localhost:8080/api/v1/data-quality/config/reset?bankId=bank-001" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"

# 4. Test invalid threshold (should fail)
curl -X PUT "http://localhost:8080/api/v1/data-quality/config?bankId=bank-001" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "thresholds": {
      "completeness": 1.5
    }
  }'
```

### Integration Test Example
```java
@SpringBootTest
@AutoConfigureMockMvc
class DataQualityConfigControllerIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    void testGetConfiguration() throws Exception {
        mockMvc.perform(get("/api/v1/data-quality/config")
                .param("bankId", "test-bank"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.thresholds.completeness").value(0.95));
    }
    
    @Test
    void testUpdateConfiguration() throws Exception {
        String json = """
            {
                "thresholds": {
                    "completeness": 0.97,
                    "accuracy": 0.99,
                    "timeliness": 0.92,
                    "consistency": 0.94
                }
            }
            """;
        
        mockMvc.perform(put("/api/v1/data-quality/config")
                .param("bankId", "test-bank")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.thresholds.completeness").value(0.97));
    }
}
```

## Evolution Path

### Phase 1: Fake Data (Completed ✅)
- Controller with Map<String, Object>
- No persistence
- Immediate API validation

### Phase 2: Service Layer (Completed ✅)
- Strong DTOs (ConfigurationDto)
- ConfigurationService with repository integration
- Real persistence with QualityThresholdRepository

### Phase 3: CQRS Refactoring (Completed ✅)
- Separate Query/Command handlers
- Individual handlers per operation
- Strict read/write separation

### Phase 4: Future Enhancements (Planned)
- [ ] Caching for query handler
- [ ] Async command processing
- [ ] Event sourcing for audit trail
- [ ] Optimistic locking for concurrent updates
- [ ] Notification on configuration changes

## Compilation Status

✅ **SUCCESS** - Module compiles without errors:
```
[INFO] BUILD SUCCESS
[INFO] Total time:  1.219 s
[INFO] Finished at: 2026-01-12T23:29:03+01:00
```

## Next Steps

1. ✅ Update controller to use handlers (COMPLETED)
2. ✅ Compile module (SUCCESS)
3. ⏳ Start application and test endpoints
4. ⏳ Add integration tests
5. ⏳ Add caching for query handler
6. ⏳ Document API in Swagger/OpenAPI

## Related Documentation

- [Clean Architecture Guide](CLEAN_ARCH_GUIDE.md)
- [Data Quality Rules Engine Guide](DATA_QUALITY_RULES_ENGINE_GUIDE.md)
- [Authentication Guide](AUTHENTICATION_GUIDE.md)
- [API Endpoints](API_ENDPOINTS.md)

---

**Status**: ✅ CQRS Implementation Complete  
**Last Updated**: 2026-01-12T23:29:00Z  
**Author**: Development Team
