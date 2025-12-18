package com.bcbs239.regtech.riskcalculation.infrastructure.database.repositories;

import com.bcbs239.regtech.riskcalculation.domain.exposure.ExposureRecording;
import com.bcbs239.regtech.riskcalculation.domain.exposure.CalculationResultsDeserializationException;
import com.bcbs239.regtech.riskcalculation.infrastructure.database.mappers.ExposureMapper;
import com.bcbs239.regtech.riskcalculation.infrastructure.database.repository.ExposureJdbcBatchInserter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test for JpaExposureRepository.loadFromJson() method
 * Verifies JSON parsing functionality for file-first architecture
 * 
 * Requirement: 5.5 - Provide methods to download and parse JSON files
 */
@ExtendWith(MockitoExtension.class)
class JpaExposureRepositoryLoadFromJsonTest {

    @Mock
    private SpringDataExposureRepository springDataRepository;
    
    @Mock
    private ExposureMapper mapper;

    @Mock
    private ExposureJdbcBatchInserter jdbcBatchInserter;
    
    private JpaExposureRepository repository;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
      repository = new JpaExposureRepository(springDataRepository, mapper, objectMapper, jdbcBatchInserter);
    }

    @Test
    void loadFromJson_withValidJson_shouldParseExposures() {
        // Given
        String validJson = """
            {
              "format_version": "1.0",
              "batch_id": "batch_123",
              "calculated_at": "2024-12-07T12:00:00Z",
              "bank_info": {
                "bank_name": "Test Bank",
                "abi_code": "12345",
                "lei_code": "LEI12345678901234567"
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
                  "percentage_of_total": 50.0
                },
                {
                  "exposure_id": "EXP002",
                  "gross_exposure_eur": 1000000.00,
                  "net_exposure_eur": 1000000.00,
                  "total_mitigation_eur": 0.00,
                  "percentage_of_total": 50.0
                }
              ]
            }
            """;

        // When
        List<ExposureRecording> exposures = repository.loadFromJson(validJson);

        // Then
        assertThat(exposures).hasSize(2);
        assertThat(exposures.get(0).id().value()).isEqualTo("EXP001");
        assertThat(exposures.get(0).exposureAmount().amount()).isEqualByComparingTo("1000000.00");
        assertThat(exposures.get(0).exposureAmount().currency()).isEqualTo("EUR");
        assertThat(exposures.get(1).id().value()).isEqualTo("EXP002");
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
        List<ExposureRecording> exposures = repository.loadFromJson(jsonWithNoExposures);

        // Then
        assertThat(exposures).isEmpty();
    }

    @Test
    void loadFromJson_withMissingExposureId_shouldThrowException() {
        // Given
        String invalidJson = """
            {
              "format_version": "1.0",
              "batch_id": "batch_123",
              "calculated_at": "2024-12-07T12:00:00Z",
              "calculated_exposures": [
                {
                  "gross_exposure_eur": 1000000.00,
                  "net_exposure_eur": 800000.00
                }
              ]
            }
            """;

        // When/Then
        assertThatThrownBy(() -> repository.loadFromJson(invalidJson))
            .isInstanceOf(CalculationResultsDeserializationException.class)
            .hasMessageContaining("Missing required field: exposure_id");
    }

    @Test
    void loadFromJson_withMissingGrossExposure_shouldThrowException() {
        // Given
        String invalidJson = """
            {
              "format_version": "1.0",
              "batch_id": "batch_123",
              "calculated_at": "2024-12-07T12:00:00Z",
              "calculated_exposures": [
                {
                  "exposure_id": "EXP001",
                  "net_exposure_eur": 800000.00
                }
              ]
            }
            """;

        // When/Then
        assertThatThrownBy(() -> repository.loadFromJson(invalidJson))
            .isInstanceOf(CalculationResultsDeserializationException.class)
            .hasMessageContaining("Missing required field: gross_exposure_eur");
    }
}
