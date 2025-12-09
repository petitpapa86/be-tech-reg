package com.bcbs239.regtech.riskcalculation.infrastructure.database.repositories;

import com.bcbs239.regtech.riskcalculation.domain.persistence.CalculationResultsDeserializationException;
import com.bcbs239.regtech.riskcalculation.domain.protection.MitigationType;
import com.bcbs239.regtech.riskcalculation.domain.protection.RawMitigationData;
import com.bcbs239.regtech.riskcalculation.infrastructure.database.mappers.MitigationMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test for JpaMitigationRepository.loadFromJson() method
 * Verifies JSON parsing functionality for file-first architecture
 * 
 * Requirement: 5.5 - Provide methods to download and parse JSON files
 */
@ExtendWith(MockitoExtension.class)
class JpaMitigationRepositoryLoadFromJsonTest {

    @Mock
    private SpringDataMitigationRepository springDataRepository;
    
    @Mock
    private MitigationMapper mapper;
    
    private JpaMitigationRepository repository;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        repository = new JpaMitigationRepository(springDataRepository, mapper, objectMapper);
    }

    @Test
    void loadFromJson_withValidJson_shouldParseMitigations() {
        // Given
        String validJson = """
            {
              "format_version": "1.0",
              "batch_id": "batch_123",
              "calculated_at": "2024-12-07T12:00:00Z",
              "bank_info": {
                "bank_name": "Test Bank",
                "abi_code": "12345",
                "lei_code": "LEI123"
              },
              "summary": {
                "total_exposures": 2,
                "total_amount_eur": 2000000.00
              },
              "calculated_exposures": [
                {
                  "exposure_id": "EXP001",
                  "gross_exposure_eur": 1000000.00,
                  "net_exposure_eur": 800000.00,
                  "total_mitigation_eur": 200000.00,
                  "percentage_of_total": 50.0,
                  "mitigations": [
                    {
                      "type": "FINANCIAL_COLLATERAL",
                      "eur_value": 150000.00
                    },
                    {
                      "type": "GUARANTEE",
                      "eur_value": 50000.00
                    }
                  ]
                },
                {
                  "exposure_id": "EXP002",
                  "gross_exposure_eur": 1000000.00,
                  "net_exposure_eur": 900000.00,
                  "total_mitigation_eur": 100000.00,
                  "percentage_of_total": 50.0,
                  "mitigations": [
                    {
                      "type": "REAL_ESTATE",
                      "eur_value": 100000.00
                    }
                  ]
                }
              ]
            }
            """;

        // When
        List<RawMitigationData> mitigations = repository.loadFromJson(validJson);

        // Then
        assertThat(mitigations).hasSize(3);
        
        // First mitigation
        assertThat(mitigations.get(0).type()).isEqualTo(MitigationType.FINANCIAL_COLLATERAL);
        assertThat(mitigations.get(0).value()).isEqualByComparingTo(new BigDecimal("150000.00"));
        assertThat(mitigations.get(0).currency()).isEqualTo("EUR");
        
        // Second mitigation
        assertThat(mitigations.get(1).type()).isEqualTo(MitigationType.GUARANTEE);
        assertThat(mitigations.get(1).value()).isEqualByComparingTo(new BigDecimal("50000.00"));
        assertThat(mitigations.get(1).currency()).isEqualTo("EUR");
        
        // Third mitigation
        assertThat(mitigations.get(2).type()).isEqualTo(MitigationType.REAL_ESTATE);
        assertThat(mitigations.get(2).value()).isEqualByComparingTo(new BigDecimal("100000.00"));
        assertThat(mitigations.get(2).currency()).isEqualTo("EUR");
    }

    @Test
    void loadFromJson_withNoMitigations_shouldReturnEmptyList() {
        // Given
        String jsonWithNoMitigations = """
            {
              "format_version": "1.0",
              "batch_id": "batch_123",
              "calculated_at": "2024-12-07T12:00:00Z",
              "calculated_exposures": [
                {
                  "exposure_id": "EXP001",
                  "gross_exposure_eur": 1000000.00,
                  "net_exposure_eur": 1000000.00,
                  "total_mitigation_eur": 0.00,
                  "percentage_of_total": 100.0
                }
              ]
            }
            """;

        // When
        List<RawMitigationData> mitigations = repository.loadFromJson(jsonWithNoMitigations);

        // Then
        assertThat(mitigations).isEmpty();
    }

    @Test
    void loadFromJson_withNullJson_shouldThrowException() {
        // When/Then
        assertThatThrownBy(() -> repository.loadFromJson(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("JSON content cannot be null or empty");
    }

    @Test
    void loadFromJson_withEmptyJson_shouldThrowException() {
        // When/Then
        assertThatThrownBy(() -> repository.loadFromJson(""))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("JSON content cannot be null or empty");
    }

    @Test
    void loadFromJson_withMissingFormatVersion_shouldThrowException() {
        // Given
        String invalidJson = """
            {
              "batch_id": "batch_123",
              "calculated_exposures": []
            }
            """;

        // When/Then
        assertThatThrownBy(() -> repository.loadFromJson(invalidJson))
            .isInstanceOf(CalculationResultsDeserializationException.class)
            .hasMessageContaining("Missing format_version field");
    }

    @Test
    void loadFromJson_withMissingExposuresArray_shouldThrowException() {
        // Given
        String invalidJson = """
            {
              "format_version": "1.0",
              "batch_id": "batch_123"
            }
            """;

        // When/Then
        assertThatThrownBy(() -> repository.loadFromJson(invalidJson))
            .isInstanceOf(CalculationResultsDeserializationException.class)
            .hasMessageContaining("Missing calculated_exposures field");
    }

    @Test
    void loadFromJson_withMalformedJson_shouldThrowException() {
        // Given
        String malformedJson = "{ invalid json }";

        // When/Then
        assertThatThrownBy(() -> repository.loadFromJson(malformedJson))
            .isInstanceOf(CalculationResultsDeserializationException.class)
            .hasMessageContaining("Failed to parse JSON content");
    }

    @Test
    void loadFromJson_withEmptyExposuresArray_shouldReturnEmptyList() {
        // Given
        String jsonWithNoExposures = """
            {
              "format_version": "1.0",
              "batch_id": "batch_123",
              "calculated_at": "2024-12-07T12:00:00Z",
              "calculated_exposures": []
            }
            """;

        // When
        List<RawMitigationData> mitigations = repository.loadFromJson(jsonWithNoExposures);

        // Then
        assertThat(mitigations).isEmpty();
    }

    @Test
    void loadFromJson_withInvalidMitigationType_shouldThrowException() {
        // Given
        String invalidJson = """
            {
              "format_version": "1.0",
              "batch_id": "batch_123",
              "calculated_at": "2024-12-07T12:00:00Z",
              "calculated_exposures": [
                {
                  "exposure_id": "EXP001",
                  "gross_exposure_eur": 1000000.00,
                  "mitigations": [
                    {
                      "type": "INVALID_TYPE",
                      "eur_value": 100000.00
                    }
                  ]
                }
              ]
            }
            """;

        // When/Then
        assertThatThrownBy(() -> repository.loadFromJson(invalidJson))
            .isInstanceOf(CalculationResultsDeserializationException.class)
            .hasMessageContaining("Invalid mitigation type: INVALID_TYPE");
    }

    @Test
    void loadFromJson_withMissingMitigationType_shouldThrowException() {
        // Given
        String invalidJson = """
            {
              "format_version": "1.0",
              "batch_id": "batch_123",
              "calculated_at": "2024-12-07T12:00:00Z",
              "calculated_exposures": [
                {
                  "exposure_id": "EXP001",
                  "gross_exposure_eur": 1000000.00,
                  "mitigations": [
                    {
                      "eur_value": 100000.00
                    }
                  ]
                }
              ]
            }
            """;

        // When/Then
        assertThatThrownBy(() -> repository.loadFromJson(invalidJson))
            .isInstanceOf(CalculationResultsDeserializationException.class)
            .hasMessageContaining("Missing required field: type");
    }

    @Test
    void loadFromJson_withMissingEurValue_shouldThrowException() {
        // Given
        String invalidJson = """
            {
              "format_version": "1.0",
              "batch_id": "batch_123",
              "calculated_at": "2024-12-07T12:00:00Z",
              "calculated_exposures": [
                {
                  "exposure_id": "EXP001",
                  "gross_exposure_eur": 1000000.00,
                  "mitigations": [
                    {
                      "type": "GUARANTEE"
                    }
                  ]
                }
              ]
            }
            """;

        // When/Then
        assertThatThrownBy(() -> repository.loadFromJson(invalidJson))
            .isInstanceOf(CalculationResultsDeserializationException.class)
            .hasMessageContaining("Missing required field: eur_value");
    }

    @Test
    void loadFromJson_withNonArrayMitigations_shouldThrowException() {
        // Given
        String invalidJson = """
            {
              "format_version": "1.0",
              "batch_id": "batch_123",
              "calculated_at": "2024-12-07T12:00:00Z",
              "calculated_exposures": [
                {
                  "exposure_id": "EXP001",
                  "gross_exposure_eur": 1000000.00,
                  "mitigations": "not_an_array"
                }
              ]
            }
            """;

        // When/Then
        assertThatThrownBy(() -> repository.loadFromJson(invalidJson))
            .isInstanceOf(CalculationResultsDeserializationException.class)
            .hasMessageContaining("mitigations field must be an array");
    }

    @Test
    void loadFromJson_withMultipleExposuresAndMixedMitigations_shouldParseAll() {
        // Given
        String validJson = """
            {
              "format_version": "1.0",
              "batch_id": "batch_123",
              "calculated_at": "2024-12-07T12:00:00Z",
              "calculated_exposures": [
                {
                  "exposure_id": "EXP001",
                  "gross_exposure_eur": 1000000.00,
                  "mitigations": [
                    {
                      "type": "FINANCIAL_COLLATERAL",
                      "eur_value": 100000.00
                    }
                  ]
                },
                {
                  "exposure_id": "EXP002",
                  "gross_exposure_eur": 500000.00
                },
                {
                  "exposure_id": "EXP003",
                  "gross_exposure_eur": 750000.00,
                  "mitigations": [
                    {
                      "type": "PHYSICAL_ASSET",
                      "eur_value": 50000.00
                    },
                    {
                      "type": "GUARANTEE",
                      "eur_value": 25000.00
                    }
                  ]
                }
              ]
            }
            """;

        // When
        List<RawMitigationData> mitigations = repository.loadFromJson(validJson);

        // Then
        assertThat(mitigations).hasSize(3);
        assertThat(mitigations.get(0).type()).isEqualTo(MitigationType.FINANCIAL_COLLATERAL);
        assertThat(mitigations.get(1).type()).isEqualTo(MitigationType.PHYSICAL_ASSET);
        assertThat(mitigations.get(2).type()).isEqualTo(MitigationType.GUARANTEE);
    }
}
