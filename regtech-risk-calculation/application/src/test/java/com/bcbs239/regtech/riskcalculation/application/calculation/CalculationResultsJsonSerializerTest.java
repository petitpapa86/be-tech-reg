package com.bcbs239.regtech.riskcalculation.application.calculation;

import com.bcbs239.regtech.riskcalculation.domain.analysis.PortfolioAnalysis;
import com.bcbs239.regtech.riskcalculation.domain.calculation.RiskCalculationResult;
import com.bcbs239.regtech.riskcalculation.domain.classification.ClassifiedExposure;
import com.bcbs239.regtech.riskcalculation.domain.classification.EconomicSector;
import com.bcbs239.regtech.riskcalculation.domain.protection.ProtectedExposure;
import com.bcbs239.regtech.riskcalculation.domain.shared.enums.GeographicRegion;
import com.bcbs239.regtech.core.domain.shared.valueobjects.BankInfo;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.ExposureId;
import com.bcbs239.regtech.riskcalculation.domain.storage.CalculationResultsJsonSerializer;
import com.bcbs239.regtech.riskcalculation.domain.valuation.EurAmount;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for CalculationResultsJsonSerializer.
 * Tests JSON serialization and deserialization of risk calculation results.
 */
class CalculationResultsJsonSerializerTest {

    private CalculationResultsJsonSerializer serializer;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules(); // This will register JavaTimeModule automatically
        serializer = new CalculationResultsJsonSerializer(objectMapper);
    }

    @Test
    void shouldSerializeCompleteCalculationResult() throws Exception {
        // Given: Complete calculation result
        RiskCalculationResult result = createSampleCalculationResult();
        
        // When: Serialize to JSON
        String json = serializer.serialize(result);
        
        // Then: Should succeed
        assertThat(json).isNotNull();
        assertThat(json).isNotEmpty();
        
        // Parse and verify JSON structure
        JsonNode rootNode = objectMapper.readTree(json);
        
        // Requirement 10.1: Verify format_version field
        assertThat(rootNode.has("format_version")).isTrue();
        assertThat(rootNode.get("format_version").asText()).isEqualTo("1.0");
        
        // Requirement 10.2: Verify batch_id and calculated_at
        assertThat(rootNode.get("batch_id").asText()).isEqualTo("batch_test_001");
        assertThat(rootNode.has("calculated_at")).isTrue();
        
        // Verify bank_info section
        JsonNode bankInfoNode = rootNode.get("bank_info");
        assertThat(bankInfoNode).isNotNull();
        assertThat(bankInfoNode.get("bank_name").asText()).isEqualTo("Test Bank");
        assertThat(bankInfoNode.get("abi_code").asText()).isEqualTo("08081");
        assertThat(bankInfoNode.get("lei_code").asText()).isEqualTo("LEI12345678901234567");
        
        // Requirement 10.3: Verify summary section with totals and breakdowns
        JsonNode summaryNode = rootNode.get("summary");
        assertThat(summaryNode).isNotNull();
        assertThat(summaryNode.get("total_exposures").asInt()).isEqualTo(2);
        assertThat(summaryNode.get("total_amount_eur").asDouble()).isEqualTo(1500000.0);
        
        // Geographic breakdown in summary
        JsonNode geoBreakdown = summaryNode.get("geographic_breakdown");
        assertThat(geoBreakdown).isNotNull();
        assertThat(geoBreakdown.has("italy")).isTrue();
        
        // Sector breakdown in summary
        JsonNode sectorBreakdown = summaryNode.get("sector_breakdown");
        assertThat(sectorBreakdown).isNotNull();
        assertThat(sectorBreakdown.has("retail_mortgage")).isTrue();
        
        // Concentration indices
        JsonNode concentrationNode = summaryNode.get("concentration_indices");
        assertThat(concentrationNode).isNotNull();
        assertThat(concentrationNode.has("herfindahl_geographic")).isTrue();
        assertThat(concentrationNode.has("herfindahl_sector")).isTrue();
        
        // Requirement 10.4: Verify exposures array with complete details
        JsonNode exposuresNode = rootNode.get("calculated_exposures");
        assertThat(exposuresNode).isNotNull();
        assertThat(exposuresNode.isArray()).isTrue();
        assertThat(exposuresNode.size()).isEqualTo(2);
        
        // First exposure details
        JsonNode firstExposure = exposuresNode.get(0);
        assertThat(firstExposure.get("exposure_id").asText()).isEqualTo("EXP_001");
        assertThat(firstExposure.get("gross_exposure_eur").asDouble()).isEqualTo(1000000.0);
        assertThat(firstExposure.get("net_exposure_eur").asDouble()).isEqualTo(1000000.0);
        assertThat(firstExposure.get("total_mitigation_eur").asDouble()).isEqualTo(0.0);
        assertThat(firstExposure.has("percentage_of_total")).isTrue();
    }

    @Test
    void shouldDeserializeJsonCorrectly() throws Exception {
        // Given: Valid JSON string with format_version
        String json = """
            {
                "format_version": "1.0",
                "batch_id": "batch_test_002",
                "calculated_at": "2024-03-31T14:30:00Z",
                "bank_info": {
                    "bank_name": "Another Bank",
                    "abi_code": "08082",
                    "lei_code": "LEI98765432109876543"
                },
                "summary": {
                    "total_exposures": 1,
                    "total_amount_eur": 800000.0,
                    "geographic_breakdown": {
                        "italy": {"amount_eur": 800000.0, "percentage": 100.0}
                    },
                    "sector_breakdown": {
                        "retail_mortgage": {"amount_eur": 800000.0, "percentage": 100.0}
                    },
                    "concentration_indices": {
                        "herfindahl_geographic": 1.0,
                        "herfindahl_sector": 1.0
                    }
                },
                "calculated_exposures": [
                    {
                        "exposure_id": "EXP_003",
                        "gross_exposure_eur": 800000.0,
                        "net_exposure_eur": 800000.0,
                        "total_mitigation_eur": 0.0,
                        "percentage_of_total": 100.0
                    }
                ]
            }""";
        
        // When: Deserialize
        RiskCalculationResult result = serializer.deserialize(json);
        
        // Then: Should succeed
        assertThat(result).isNotNull();
        assertThat(result.batchId()).isEqualTo("batch_test_002");
        assertThat(result.bankInfo().bankName()).isEqualTo("Another Bank");
        assertThat(result.bankInfo().abiCode()).isEqualTo("08082");
        assertThat(result.bankInfo().leiCode()).isEqualTo("LEI98765432109876543");
        assertThat(result.calculatedExposures()).hasSize(1);
        assertThat(result.analysis()).isNotNull();
        assertThat(result.analysis().getTotalPortfolio().value()).isEqualByComparingTo("800000.0");
    }
    
    @Test
    void shouldRoundTripSerializeAndDeserialize() {
        // Given: Complete calculation result
        RiskCalculationResult original = createSampleCalculationResult();
        
        // When: Serialize then deserialize
        String json = serializer.serialize(original);
        RiskCalculationResult deserialized = serializer.deserialize(json);
        
        // Then: Should match original
        assertThat(deserialized.batchId()).isEqualTo(original.batchId());
        assertThat(deserialized.bankInfo()).isEqualTo(original.bankInfo());
        assertThat(deserialized.calculatedExposures()).hasSameSizeAs(original.calculatedExposures());
        assertThat(deserialized.analysis().getTotalPortfolio().value())
            .isEqualByComparingTo(original.analysis().getTotalPortfolio().value());
    }

    @Test
    void shouldThrowExceptionForNullCalculationResult() {
        // When/Then: Serialize null result should throw exception
        assertThatThrownBy(() -> serializer.serialize(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("cannot be null");
    }

    @Test
    void shouldThrowExceptionForNullJsonString() {
        // When/Then: Deserialize null JSON should throw exception
        assertThatThrownBy(() -> serializer.deserialize(null))
            .isInstanceOf(CalculationResultsDeserializationException.class)
            .hasMessageContaining("cannot be null");
    }

    @Test
    void shouldThrowExceptionForEmptyJsonString() {
        // When/Then: Deserialize empty JSON should throw exception
        assertThatThrownBy(() -> serializer.deserialize(""))
            .isInstanceOf(CalculationResultsDeserializationException.class)
            .hasMessageContaining("cannot be null or empty");
    }

    @Test
    void shouldThrowExceptionForInvalidJsonString() {
        // When/Then: Deserialize invalid JSON should throw exception
        assertThatThrownBy(() -> serializer.deserialize("{ invalid json }"))
            .isInstanceOf(CalculationResultsDeserializationException.class)
            .hasMessageContaining("Failed to deserialize");
    }
    
    @Test
    void shouldThrowExceptionForMissingFormatVersion() {
        // Given: JSON without format_version
        String json = """
            {
                "batch_id": "batch_test_003",
                "calculated_at": "2024-03-31T14:30:00Z"
            }""";
        
        // When/Then: Should throw exception
        assertThatThrownBy(() -> serializer.deserialize(json))
            .isInstanceOf(CalculationResultsDeserializationException.class)
            .hasMessageContaining("format_version");
    }

    // Helper method to create sample calculation result
    private RiskCalculationResult createSampleCalculationResult() {
        String batchId = "batch_test_001";
        BankInfo bankInfo = BankInfo.of("Test Bank", "08081", "LEI12345678901234567");
        
        // Create exposures
        List<ProtectedExposure> exposures = new ArrayList<>();
        
        // Exposure 1 - no mitigations
        exposures.add(ProtectedExposure.withoutMitigations(
            ExposureId.of("EXP_001"),
            EurAmount.of(1000000.0)
        ));
        
        // Exposure 2 - no mitigations
        exposures.add(ProtectedExposure.withoutMitigations(
            ExposureId.of("EXP_002"),
            EurAmount.of(500000.0)
        ));
        
        // Create classified exposures for portfolio analysis
        List<ClassifiedExposure> classifiedExposures = new ArrayList<>();
        classifiedExposures.add(ClassifiedExposure.of(
            ExposureId.of("EXP_001"),
            EurAmount.of(1000000.0),
            GeographicRegion.ITALY,
            EconomicSector.RETAIL_MORTGAGE
        ));
        classifiedExposures.add(ClassifiedExposure.of(
            ExposureId.of("EXP_002"),
            EurAmount.of(500000.0),
            GeographicRegion.ITALY,
            EconomicSector.RETAIL_MORTGAGE
        ));
        
        // Create portfolio analysis
        PortfolioAnalysis analysis = PortfolioAnalysis.analyze(batchId, classifiedExposures);
        
        return new RiskCalculationResult(
            batchId,
            bankInfo,
            exposures,
            analysis,
            Instant.now()
        );
    }
}
