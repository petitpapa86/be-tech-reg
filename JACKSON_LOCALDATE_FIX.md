# Jackson LocalDate Serialization Fix

## Problem
Runtime exception when serializing `java.time.LocalDate` fields:
```
java.lang.RuntimeException: Command handling failed: Failed to parse JSON: 
Java 8 date/time type `java.time.LocalDate` not supported by default: 
add Module "com.fasterxml.jackson.datatype:jackson-datatype-jsr310" to enable handling
```

## Root Cause
Multiple classes were creating `ObjectMapper` instances directly without registering the `JavaTimeModule`, which is required for Java 8 date/time types like `LocalDate`.

While the main `JacksonConfiguration` properly configures the Spring-managed `ObjectMapper` bean, several classes were creating their own instances.

## Files Fixed

### 1. S3StorageServiceImpl
**File:** `regtech-data-quality/infrastructure/src/main/java/com/bcbs239/regtech/dataquality/infrastructure/integration/S3StorageServiceImpl.java`

Added JavaTimeModule registration in constructor.

### 2. StripePaymentService
**File:** `regtech-billing/infrastructure/src/main/java/com/bcbs239/regtech/billing/infrastructure/services/StripePaymentService.java`

Added JavaTimeModule registration in instance initializer block.

### 3. CreateStripeCustomerCommandHandler
**File:** `regtech-billing/application/src/main/java/com/bcbs239/regtech/billing/application/subscriptions/CreateStripeCustomerCommandHandler.java`

Added JavaTimeModule registration in instance initializer block.

### 4. BillingValidationUtils (domain)
**File:** `regtech-billing/domain/src/main/java/com/bcbs239/regtech/billing/domain/shared/validation/BillingValidationUtils.java`

Added JavaTimeModule registration in static initializer block.

### 5. BillingValidationUtils (infrastructure)
**File:** `regtech-billing/infrastructure/src/main/java/com/bcbs239/regtech/billing/infrastructure/validation/BillingValidationUtils.java`

Added JavaTimeModule registration in static initializer block.

### 6. JsonbConverter (domain)
**File:** `regtech-data-quality/domain/src/main/java/com/bcbs239/regtech/dataquality/rulesengine/domain/JsonbConverter.java`

Added JavaTimeModule registration in static initializer block.

### 7. JsonbConverter (infrastructure)
**File:** `regtech-data-quality/infrastructure/src/main/java/com/bcbs239/regtech/dataquality/infrastructure/rulesengine/model/JsonbConverter.java`

Added JavaTimeModule registration in static initializer block.

### 8. EventProcessingFailureMapper
**File:** `regtech-core/infrastructure/src/main/java/com/bcbs239/regtech/core/infrastructure/eventprocessing/EventProcessingFailureMapper.java`

Added JavaTimeModule registration in static initializer block.

## Solution Applied
For each ObjectMapper instance, added:
```java
objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
```

## Best Practice Recommendation
Going forward, prefer dependency injection of the configured `ObjectMapper` bean from `JacksonConfiguration` rather than creating new instances. If a new instance is required, always register the `JavaTimeModule`.

## Status
✅ All files fixed and verified - no compilation errors
