package com.bcbs239.regtech.riskcalculation.application.calculation;

import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.riskcalculation.domain.analysis.Breakdown;
import com.bcbs239.regtech.riskcalculation.domain.analysis.HHI;
import com.bcbs239.regtech.riskcalculation.domain.analysis.PortfolioAnalysis;
import com.bcbs239.regtech.riskcalculation.domain.analysis.Share;
import com.bcbs239.regtech.riskcalculation.domain.exposure.*;
import com.bcbs239.regtech.riskcalculation.domain.protection.ProtectedExposure;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.AmountEur;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.BankInfo;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.PercentageOfTotal;
import com.bcbs239.regtech.riskcalculation.domain.valuation.EurAmount;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

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
        serializer = new CalculationResultsJsonSerializer(objectMapper);
    }

    @Test
    void shouldSerializeCompleteCalculationResult() throws Exception {
        // Given: Complete calculation result
        RiskCalculationResult result = createSampleCalculationResult();
        
        // When: Serialize to JSON
        Result<String> jsonResult = serializer.serializeToJson(result);
        
        // Then: Should succeed
        assertThat(jsonResult.isSuccess()).isTrue();
        
        String json = jsonResult.getValue();
        assertThat(json).isNotNull();
        assertThat(json).isNotEmpty();
        
        // Parse and verify JSON structure
        JsonNode rootNode = objectMapper.readTree(json);
        
        // Basic metadata
        assertThat(rootNode.get("batch_id").asText()).isEqualTo("batch_test_001");
        assertThat(rootNode.get("bank_id").asText()).isEqualTo("08081");
        assertThat(rootNode.get("bank_name").asText()).isEqualTo("Test Bank");
        assertThat(rootNode.has("calculated_at")).isTrue();
        
        // Summary section
        JsonNode summaryNode = rootNode.get("summary");
        assertThat(summaryNode).isNotNull();
        assertThat(summaryNode.get("total_exposures").asInt()).isEqualTo(2);
        assertThat(summaryNode.get("total_amount_eur").asDouble()).isEqualTo(1500000.0);
        
        // Geographic breakdown in summary
        JsonNode geoBreakdown = summaryNode.get("geographic_breakdown");
        assertThat(geoBreakdown).isNotNull();
        assertThat(geoBreakdown.has("italy")).isTrue();
        assertThat(geoBreakdown.get("italy").get("amount_eur").asDouble()).isEqualTo(1000000.0);
        assertThat(geoBreakdown.get("italy").get("percentage").asDouble()).isCloseTo(66.67, within(0.01));
        
        // Sector breakdown in summary
        JsonNode sectorBreakdown = summaryNode.get("sector_breakdown");
        assertThat(sectorBreakdown).isNotNull();
        assertThat(sectorBreakdown.has("retail")).isTrue();
        
        // Concentration indices
        JsonNode concentrationNode = summaryNode.get("concentration_indices");
        assertThat(concentrationNode).isNotNull();
        assertThat(concentrationNode.get("herfindahl_geographic").asDouble()).isEqualTo(0.5556);
        assertThat(concentrationNode.get("herfindahl_sector").asDouble()).isEqualTo(0.5);
        
        // Calculated exposures section
        JsonNode exposuresNode = rootNode.get("calculated_exposures");
        assertThat(exposuresNode).isNotNull();
        assertThat(exposuresNode.isArray()).isTrue();
        assertThat(exposuresNode.size()).isEqualTo(2);
        
        // First exposure details
        JsonNode firstExposure = exposuresNode.get(0);
        assertThat(firstExposure.get("instrument_id").asText()).isEqualTo("INST_001");
        assertThat(firstExposure.get("counterparty_ref").asText()).isEqualTo("CP_001");
        assertThat(firstExposure.get("original_amount").asDouble()).isEqualTo(1000000.0);
        assertThat(firstExposure.get("original_currency").asText()).isEqualTo("EUR");
        assertThat(firstExposure.get("eur_amount").asDouble()).isEqualTo(1000000.0);
        assertThat(firstExposure.get("mitigated_amount_eur").asDouble()).isEqualTo(1000000.0);
        
        // New fields
        assertThat(firstExposure.has("exchange_rate_used")).isTrue();
        assertThat(firstExposure.get("exchange_rate_used").asDouble()).isEqualTo(1.0);
        assertThat(firstExposure.has("percentage_of_total")).isTrue();
        assertThat(firstExposure.get("percentage_of_total").asDouble()).isCloseTo(66.67, within(0.01));
        assertThat(firstExposure.has("country")).isTrue();
        assertThat(firstExposure.get("country").asText()).isEqualTo("IT");
        
        assertThat(firstExposure.get("geographic_region").asText()).isEqualTo("ITALY");
        assertThat(firstExposure.get("economic_sector").asText()).isEqualTo("RETAIL");
    }

    @Test
    void shouldDeserializeJsonCorrectly() throws Exception {
        // Given: Valid JSON string
        String json = """
            {
                "batch_id": "batch_test_002",
                "calculated_at": "2024-03-31T14:30:00Z",
                "bank_id": "08082",
                "bank_name": "Another Bank",
                "summary": {
                    "total_exposures": 3,
                    "total_amount_eur": 2000000.0,
                    "geographic_breakdown": {
                        "italy": {"amount_eur": 1200000.0, "percentage": 60.0},
                        "eu": {"amount_eur": 600000.0, "percentage": 30.0},
                        "non_eu": {"amount_eur": 200000.0, "percentage": 10.0}
                    },
                    "sector_breakdown": {
                        "retail": {"amount_eur": 800000.0, "percentage": 40.0},
                        "corporate": {"amount_eur": 700000.0, "percentage": 35.0},
                        "sovereign": {"amount_eur": 500000.0, "percentage": 25.0}
                    },
                    "concentration_indices": {
                        "herfindahl_geographic": 0.46,
                        "herfindahl_sector": 0.335
                    }
                },
                "calculated_exposures": [
                    {
                        "instrument_id": "INST_003",
                        "counterparty_ref": "CP_003",
                        "original_amount": 800000.0,
                        "original_currency": "EUR",
                        "eur_amount": 800000.0,
                        "mitigated_amount_eur": 800000.0,
                        "geographic_region": "ITALY",
                        "economic_sector": "RETAIL"
                    }
                ]
            }""";
        
        // When: Deserialize
        Result<JsonNode> result = serializer.deserializeFromJson(json);
        
        // Then: Should succeed
        assertThat(result.isSuccess()).isTrue();
        
        JsonNode rootNode = result.getValue();
        assertThat(rootNode.get("batch_id").asText()).isEqualTo("batch_test_002");
        assertThat(rootNode.get("bank_id").asText()).isEqualTo("08082");
        assertThat(rootNode.get("summary").get("total_exposures").asInt()).isEqualTo(3);
        assertThat(rootNode.get("calculated_exposures").size()).isEqualTo(1);
    }

    @Test
    void shouldHandleNullCalculationResult() {
        // When: Serialize null result
        Result<String> result = serializer.serializeToJson(null);
        
        // Then: Should fail with validation error
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getCode()).isEqualTo("NULL_CALCULATION_RESULT");
        assertThat(result.getError().getMessage()).contains("cannot be null");
    }

    @Test
    void shouldHandleNullJsonString() {
        // When: Deserialize null JSON
        Result<JsonNode> result = serializer.deserializeFromJson(null);
        
        // Then: Should fail with validation error
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getCode()).isEqualTo("NULL_JSON_STRING");
        assertThat(result.getError().getMessage()).contains("cannot be null");
    }

    @Test
    void shouldHandleEmptyJsonString() {
        // When: Deserialize empty JSON
        Result<JsonNode> result = serializer.deserializeFromJson("");
        
        // Then: Should fail with validation error
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getCode()).isEqualTo("NULL_JSON_STRING");
    }

    @Test
    void shouldHandleInvalidJsonString() {
        // When: Deserialize invalid JSON
        Result<JsonNode> result = serializer.deserializeFromJson("{ invalid json }");
        
        // Then: Should fail with deserialization error
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError().getCode()).isEqualTo("JSON_DESERIALIZATION_ERROR");
    }

    // Helper method to create sample calculation result
    private RiskCalculationResult createSampleCalculationResult() {
        String batchId = "batch_test_001";
        BankInfo bankInfo = new BankInfo("08081", "Test Bank");
        
        // Create exposures
        List<ProtectedExposure> exposures = new ArrayList<>();
        
        // Exposure 1
        ExposureRecording recording1 = new ExposureRecording(
            InstrumentId.of("INST_001"),
            CounterpartyRef.of("CP_001"),
            MonetaryAmount.of(1000000.0, "EUR"),
            ExposureClassification.of("ITALY", "RETAIL")
        );
        exposures.add(new ProtectedExposure(
            recording1,
            EurAmount.of(1000000.0),
            EurAmount.of(1000000.0)
        ));
        
        // Exposure 2
        ExposureRecording recording2 = new ExposureRecording(
            InstrumentId.of("INST_002"),
            CounterpartyRef.of("CP_002"),
            MonetaryAmount.of(500000.0, "EUR"),
            ExposureClassification.of("EU", "RETAIL")
        );
        exposures.add(new ProtectedExposure(
            recording2,
            EurAmount.of(500000.0),
            EurAmount.of(500000.0)
        ));
        
        // Create portfolio analysis
        Map<String, Share> geoShares = new HashMap<>();
        geoShares.put("ITALY", new Share(AmountEur.of(1000000.0), PercentageOfTotal.of(66.67)));
        geoShares.put("EU", new Share(AmountEur.of(500000.0), PercentageOfTotal.of(33.33)));
        
        Map<String, Share> sectorShares = new HashMap<>();
        sectorShares.put("RETAIL", new Share(AmountEur.of(1500000.0), PercentageOfTotal.of(100.0)));
        
        PortfolioAnalysis analysis = new PortfolioAnalysis(
            new Breakdown(geoShares),
            new Breakdown(sectorShares),
            HHI.of(0.5556),
            HHI.of(0.5)
        );
        
        return new RiskCalculationResult(
            batchId,
            bankInfo,
            exposures,
            analysis,
            Instant.now()
        );
    }
}
