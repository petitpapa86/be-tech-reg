# Design Document: Data Format Alignment

## Overview

This design establishes a unified data interchange format for batch processing across the Ingestion, Risk Calculation, and Data Quality modules. By creating shared DTOs in the Core module, we eliminate duplication, ensure consistency, and simplify maintenance. The design focuses on creating a clear separation between domain models (internal to each module) and DTOs (for inter-module communication).

## Architecture

### Module Dependencies

```
┌─────────────────┐
│  Core Module    │
│  - DTOs         │
│  - Mappers      │
└────────┬────────┘
         │
    ┌────┴────┬────────────┬────────────┐
    │         │            │            │
┌───▼───┐ ┌──▼──┐  ┌──────▼──────┐ ┌──▼──────────┐
│Ingest │ │Risk │  │Data Quality │ │Report Gen   │
│Module │ │Calc │  │Module       │ │Module       │
└───────┘ └─────┘  └─────────────┘ └─────────────┘
```

### Data Flow

1. **Ingestion**: Domain Models → DTOs → JSON → S3
2. **Risk Calculation**: S3 → JSON → DTOs → Domain Models
3. **Data Quality**: S3 → JSON → DTOs → Domain Models
4. **Report Generation**: S3 → JSON → DTOs → Domain Models

## Components and Interfaces

### Core Module DTOs

#### BankInfoDTO
```java
package com.bcbs239.regtech.core.domain.shared.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;

/**
 * Shared DTO for bank information across all modules.
 * Used for inter-module communication via JSON serialization.
 */
public record BankInfoDTO(
    @JsonProperty("bank_name") String bankName,
    @JsonProperty("abi_code") String abiCode,
    @JsonProperty("lei_code") String leiCode,
    @JsonProperty("report_date") LocalDate reportDate,
    @JsonProperty("total_exposures") Integer totalExposures
) {}
```

#### ExposureDTO
```java
package com.bcbs239.regtech.core.domain.shared.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

/**
 * Shared DTO for exposure data across all modules.
 * Supports any type of financial instrument (LOAN, BOND, DERIVATIVE, etc.)
 */
public record ExposureDTO(
    @JsonProperty("exposure_id") String exposureId,
    @JsonProperty("instrument_id") String instrumentId,
    @JsonProperty("instrument_type") String instrumentType,
    @JsonProperty("counterparty_name") String counterpartyName,
    @JsonProperty("counterparty_id") String counterpartyId,
    @JsonProperty("counterparty_lei") String counterpartyLei,
    @JsonProperty("exposure_amount") BigDecimal exposureAmount,
    @JsonProperty("currency") String currency,
    @JsonProperty("product_type") String productType,
    @JsonProperty("balance_sheet_type") String balanceSheetType,
    @JsonProperty("country_code") String countryCode
) {}
```

#### CreditRiskMitigationDTO
```java
package com.bcbs239.regtech.core.domain.shared.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

/**
 * Shared DTO for credit risk mitigation data across all modules.
 */
public record CreditRiskMitigationDTO(
    @JsonProperty("exposure_id") String exposureId,
    @JsonProperty("mitigation_type") String mitigationType,
    @JsonProperty("value") BigDecimal value,
    @JsonProperty("currency") String currency
) {}
```

#### BatchDataDTO
```java
package com.bcbs239.regtech.core.domain.shared.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Shared DTO for complete batch data across all modules.
 * This is the root object for batch data files stored in S3.
 */
public record BatchDataDTO(
    @JsonProperty("bank_info") BankInfoDTO bankInfo,
    @JsonProperty("exposures") List<ExposureDTO> exposures,
    @JsonProperty("credit_risk_mitigation") List<CreditRiskMitigationDTO> creditRiskMitigation
) {}
```

### DTO Conversion Methods

Following DDD principles, conversion logic lives on the objects themselves rather than in external mappers.

#### Core DTOs with Factory Methods

```java
// In BankInfoDTO
public record BankInfoDTO(...) {
    /**
     * Create from domain-specific BankInfo value object
     */
    public static BankInfoDTO from(Object bankInfo) {
        // Uses reflection or instanceof to handle different module's BankInfo types
        // Each module can also provide their own static factory method
    }
}

// In ExposureDTO
public record ExposureDTO(...) {
    /**
     * Create from domain-specific exposure object
     */
    public static ExposureDTO from(Object exposure) {
        // Flexible factory that works with different module's exposure types
    }
}
```

### Ingestion Module Changes

#### Updated ParsedFileData with Conversion Methods
```java
package com.bcbs239.regtech.ingestion.domain.model;

import com.bcbs239.regtech.core.domain.shared.dto.*;
import com.bcbs239.regtech.ingestion.domain.shared.BankInfo;
import java.util.List;

/**
 * Domain model representing parsed file data.
 * Now includes bank information and conversion to DTO.
 */
public record ParsedFileData(
    BankInfo bankInfo,
    List<LoanExposure> exposures,
    List<CreditRiskMitigation> creditRiskMitigation
) {
    public int totalExposures() {
        return exposures == null ? 0 : exposures.size();
    }
    
    /**
     * Convert this domain model to a DTO for inter-module communication.
     * Following DDD: the object knows how to represent itself as a DTO.
     */
    public BatchDataDTO toDTO() {
        return new BatchDataDTO(
            bankInfo.toDTO(),
            exposures.stream().map(LoanExposure::toDTO).toList(),
            creditRiskMitigation.stream().map(CreditRiskMitigation::toDTO).toList()
        );
    }
    
    /**
     * Create from DTO (factory method).
     * Following DDD: the object knows how to construct itself from a DTO.
     */
    public static ParsedFileData fromDTO(BatchDataDTO dto) {
        return new ParsedFileData(
            BankInfo.fromDTO(dto.bankInfo()),
            dto.exposures().stream().map(LoanExposure::fromDTO).toList(),
            dto.creditRiskMitigation().stream().map(CreditRiskMitigation::fromDTO).toList()
        );
    }
}
```

#### Updated LoanExposure with Conversion Methods
```java
package com.bcbs239.regtech.ingestion.domain.model;

import com.bcbs239.regtech.core.domain.shared.dto.ExposureDTO;

public record LoanExposure(
    String exposureId,
    String instrumentId,
    String instrumentType,
    String counterpartyName,
    String counterpartyId,
    String counterpartyLei,
    BigDecimal exposureAmount,
    String currency,
    String productType,
    String balanceSheetType,
    String countryCode
) {
    /**
     * Convert to DTO for inter-module communication
     */
    public ExposureDTO toDTO() {
        return new ExposureDTO(
            exposureId, instrumentId, instrumentType,
            counterpartyName, counterpartyId, counterpartyLei,
            exposureAmount, currency, productType,
            balanceSheetType, countryCode
        );
    }
    
    /**
     * Create from DTO
     */
    public static LoanExposure fromDTO(ExposureDTO dto) {
        return new LoanExposure(
            dto.exposureId(), dto.instrumentId(), dto.instrumentType(),
            dto.counterpartyName(), dto.counterpartyId(), dto.counterpartyLei(),
            dto.exposureAmount(), dto.currency(), dto.productType(),
            dto.balanceSheetType(), dto.countryCode()
        );
    }
}
```

#### Updated BankInfo with Conversion Methods
```java
package com.bcbs239.regtech.ingestion.domain.shared;

import com.bcbs239.regtech.core.domain.shared.dto.BankInfoDTO;

public record BankInfo(
    String bankName,
    String abiCode,
    String leiCode,
    LocalDate reportDate,
    Integer totalExposures
) {
    public BankInfoDTO toDTO() {
        return new BankInfoDTO(bankName, abiCode, leiCode, reportDate, totalExposures);
    }
    
    public static BankInfo fromDTO(BankInfoDTO dto) {
        return new BankInfo(dto.bankName(), dto.abiCode(), dto.leiCode(), 
                           dto.reportDate(), dto.totalExposures());
    }
}
```

### Risk Calculation Module Changes

#### Remove Duplicate DTOs
- Delete `regtech-risk-calculation/infrastructure/src/main/java/com/bcbs239/regtech/riskcalculation/infrastructure/dto/`
- Update all imports to use Core module DTOs

#### Add Conversion Methods to Domain Objects

```java
package com.bcbs239.regtech.riskcalculation.domain.exposure;

import com.bcbs239.regtech.core.domain.shared.dto.ExposureDTO;

/**
 * Domain model for exposure recording.
 * Knows how to construct itself from a DTO.
 */
public record ExposureRecording(
    InstrumentId instrumentId,
    CounterpartyRef counterpartyRef,
    MonetaryAmount amount,
    ExposureClassification classification
) {
    /**
     * Create from DTO (factory method).
     * Following DDD: the object knows how to construct itself from external data.
     */
    public static ExposureRecording fromDTO(ExposureDTO dto) {
        return new ExposureRecording(
            InstrumentId.of(dto.instrumentId()),
            CounterpartyRef.of(dto.counterpartyId()),
            MonetaryAmount.of(dto.exposureAmount(), dto.currency()),
            ExposureClassification.of(dto.instrumentType(), dto.productType())
        );
    }
}
```

```java
package com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects;

import com.bcbs239.regtech.core.domain.shared.dto.BankInfoDTO;

/**
 * Value object for bank information in Risk Calculation domain.
 */
public record BankInfo(
    String bankName,
    String abiCode,
    String leiCode
) {
    public static BankInfo of(String bankName, String abiCode, String leiCode) {
        // validation logic
        return new BankInfo(bankName, abiCode, leiCode);
    }
    
    /**
     * Create from DTO
     */
    public static BankInfo fromDTO(BankInfoDTO dto) {
        return of(dto.bankName(), dto.abiCode(), dto.leiCode());
    }
}
```

### Data Quality Module Changes

#### Update S3StorageServiceImpl to Use BatchDataDTO

```java
// In downloadAndParseStreaming method:
private List<ExposureRecord> downloadAndParseStreaming(ResponseInputStream<GetObjectResponse> inputStream) throws IOException {
    try (JsonParser parser = jsonFactory.createParser(inputStream)) {
        JsonNode rootNode = objectMapper.readTree(parser);
        
        // Support new format with bank_info at top level - deserialize to BatchDataDTO
        if (rootNode.has("exposures") && rootNode.has("bank_info")) {
            BatchDataDTO batchData = objectMapper.treeToValue(rootNode, BatchDataDTO.class);
            return batchData.exposures().stream()
                .map(ExposureRecord::fromDTO)
                .toList();
        }
        
        // Backward compatibility: support old direct array format
        if (rootNode.isArray()) {
            return parseExposuresFromArray(rootNode);
        }
        
        // Backward compatibility: support old loan_portfolio format
        if (rootNode.has("loan_portfolio")) {
            JsonNode loanPortfolioNode = rootNode.get("loan_portfolio");
            return parseExposuresFromArray(loanPortfolioNode);
        }
        
        throw new IOException("Unsupported JSON format. Expected 'exposures' field, direct array, or 'loan_portfolio' field");
    }
}

private List<ExposureRecord> parseExposuresFromArray(JsonNode arrayNode) {
    List<ExposureRecord> exposures = new ArrayList<>();
    for (JsonNode exposureNode : arrayNode) {
        ExposureRecord exposure = parseExposureRecord(exposureNode);
        exposures.add(exposure);
    }
    return exposures;
}
```

#### Add Conversion Method to ExposureRecord

```java
package com.bcbs239.regtech.dataquality.domain.validation;

import com.bcbs239.regtech.core.domain.shared.dto.ExposureDTO;

/**
 * Domain model for exposure record in Data Quality validation.
 */
public record ExposureRecord(
    String exposureId,
    String counterpartyId,
    BigDecimal amount,
    String currency,
    String country,
    String sector,
    String counterpartyType,
    String productType,
    String leiCode,
    // ... other fields
) {
    /**
     * Create from DTO.
     * Following DDD: the object knows how to construct itself from external data.
     */
    public static ExposureRecord fromDTO(ExposureDTO dto) {
        return ExposureRecord.builder()
            .exposureId(dto.exposureId())
            .counterpartyId(dto.counterpartyId())
            .amount(dto.exposureAmount())
            .currency(dto.currency())
            .country(dto.countryCode())
            .productType(dto.productType())
            .leiCode(dto.counterpartyLei())
            // map other fields
            .build();
    }
}
```

## Data Models

### JSON Structure

```json
{
  "bank_info": {
    "bank_name": "Community First Bank",
    "abi_code": "08081",
    "lei_code": "815600D7623147C25D86",
    "report_date": "2024-09-12",
    "total_exposures": 8
  },
  "exposures": [
    {
      "exposure_id": "EXP_001_2024",
      "instrument_id": "LOAN001",
      "instrument_type": "LOAN",
      "counterparty_name": "Mike's Pizza Inc",
      "counterparty_id": "CORP12345",
      "counterparty_lei": "549300ABCDEF1234567890",
      "exposure_amount": 250000,
      "currency": "EUR",
      "product_type": "Business Loan",
      "balance_sheet_type": "ON_BALANCE",
      "country_code": "IT"
    }
  ],
  "credit_risk_mitigation": [
    {
      "exposure_id": "EXP_001_2024",
      "mitigation_type": "FINANCIAL_COLLATERAL",
      "value": 10000.00,
      "currency": "EUR"
    }
  ]
}
```

## Error Handling

### Validation Errors

1. **Missing Required Fields**: Return clear error indicating which field is missing
2. **Invalid Field Types**: Return error with expected type and actual type
3. **Invalid Field Values**: Return error with validation rule that failed
4. **Unsupported Format**: Return error with supported format examples

### Backward Compatibility

1. **Old loan_portfolio format**: Continue to support for Data Quality module
2. **Direct array format**: Continue to support for Data Quality module
3. **Field name variations**: Support both snake_case and camelCase during deserialization

## Testing Strategy

### Unit Tests

1. **DTO Serialization**: Verify DTOs serialize to expected JSON format
2. **DTO Deserialization**: Verify DTOs deserialize from JSON correctly
3. **Mapper Tests**: Verify mappers convert between domain models and DTOs correctly
4. **Field Naming**: Verify Jackson annotations produce correct JSON field names

### Integration Tests

1. **End-to-End Flow**: Ingestion → JSON → Risk Calculation
2. **End-to-End Flow**: Ingestion → JSON → Data Quality
3. **Round-Trip**: Domain Model → DTO → JSON → DTO → Domain Model
4. **Backward Compatibility**: Old formats still parse correctly

### Test Data

Create test fixtures with:
- Valid batch data in new format
- Valid batch data in old formats (for backward compatibility)
- Invalid batch data (missing fields, wrong types)
- Edge cases (empty arrays, null values)


## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system-essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property 1: Serialization Round Trip Preserves Data

*For any* valid ParsedFileData, BatchInputData, or domain model, serializing to JSON via BatchDataDTO and then deserializing back should produce an equivalent object with all data preserved.

**Validates: Requirements 3.2, 4.1, 4.2, 5.1, 7.4**

### Property 2: Exposure Mapping Preserves All Fields

*For any* LoanExposure domain object, mapping to ExposureDTO and back should preserve all field values (exposureId, instrumentId, counterpartyName, exposureAmount, currency, etc.).

**Validates: Requirements 3.3, 4.2, 5.2**

### Property 3: Mitigation Mapping Preserves All Fields

*For any* CreditRiskMitigation domain object, mapping to CreditRiskMitigationDTO and back should preserve all field values (exposureId, mitigationType, value, currency).

**Validates: Requirements 3.4**

### Property 4: JSON Serialization Uses Snake Case

*For any* DTO serialized to JSON, all field names in the resulting JSON should be in snake_case format (e.g., "bank_name", "exposure_id", "counterparty_lei").

**Validates: Requirements 6.1**

### Property 5: JSON Deserialization Supports Both Naming Conventions

*For any* valid JSON with either snake_case or camelCase field names, deserializing to DTOs should succeed and produce the same result.

**Validates: Requirements 6.2**

### Property 6: Batch Data Structure Completeness

*For any* BatchDataDTO serialized to JSON, the resulting JSON should contain exactly three top-level fields: "bank_info", "exposures", and "credit_risk_mitigation".

**Validates: Requirements 3.5, 7.5**

### Property 7: New Format Parsing

*For any* JSON file with "exposures" field at the top level, the Data Quality module should successfully parse it into a list of ExposureRecord objects.

**Validates: Requirements 5.4**

### Property 8: Integration Compatibility

*For any* batch data produced by the Ingestion module, both Risk Calculation and Data Quality modules should be able to deserialize it to BatchDataDTO without errors.

**Validates: Requirements 7.1, 7.2, 7.3**

## Property Reflection

After reviewing all properties, the following observations were made:

1. **Property 1 (Round Trip)** is the most comprehensive and subsumes several other properties. It validates that the entire serialization/deserialization pipeline works correctly.

2. **Properties 2 and 3 (Field Mapping)** are specific cases of Property 1 but focus on individual domain object types. They provide more granular validation and better error messages when mapping fails.

3. **Properties 4 and 5 (Naming Conventions)** are complementary - one validates output format, the other validates input flexibility. Both are needed.

4. **Property 6 (Structure Completeness)** validates the JSON schema structure, which is distinct from data preservation.

5. **Property 7 (New Format Parsing)** is specific to the Data Quality module's parsing logic and is not redundant with other properties.

6. **Property 8 (Integration Compatibility)** is an integration-level property that validates the entire system works together, distinct from unit-level properties.

All properties provide unique validation value and should be retained.
