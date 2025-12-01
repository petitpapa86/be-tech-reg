package com.bcbs239.regtech.riskcalculation.application.calculation;

import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.riskcalculation.domain.analysis.Breakdown;
import com.bcbs239.regtech.riskcalculation.domain.analysis.Share;
import com.bcbs239.regtech.riskcalculation.domain.protection.ProtectedExposure;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Serializes risk calculation results to JSON format for storage in S3/filesystem.
 * Creates a comprehensive JSON structure containing both summary and detailed exposure data.
 */
@Component
@Slf4j
public class CalculationResultsJsonSerializer {

    private final ObjectMapper objectMapper;
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_INSTANT;

    public CalculationResultsJsonSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Serialize complete calculation results to JSON string.
     * 
     * @param result The risk calculation result to serialize
     * @return Result containing the JSON string or error
     */
    public Result<String> serializeToJson(RiskCalculationResult result) {
        if (result == null) {
            return Result.failure(ErrorDetail.of("NULL_CALCULATION_RESULT", ErrorType.VALIDATION_ERROR,
                "Calculation result cannot be null", "calculation.serialization.null.result"));
        }

        try {
            ObjectNode rootNode = objectMapper.createObjectNode();

            // Basic metadata
            rootNode.put("batch_id", result.batchId());
            rootNode.put("calculated_at", ISO_FORMATTER.format(result.ingestedAt()));
            rootNode.put("bank_name", result.bankInfo().bankName());
            rootNode.put("abi_code", result.bankInfo().abiCode());
            rootNode.put("lei_code", result.bankInfo().leiCode());

            // Summary section
            ObjectNode summaryNode = createSummaryNode(result);
            rootNode.set("summary", summaryNode);

            // Detailed exposures section
            ArrayNode exposuresNode = createExposuresNode(result.calculatedExposures());
            rootNode.set("calculated_exposures", exposuresNode);

            // Convert to JSON string
            String jsonString = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootNode);

            log.debug("Successfully serialized calculation results for batch: {}", result.batchId());
            return Result.success(jsonString);

        } catch (Exception e) {
            log.error("Failed to serialize calculation results for batch: {}", result.batchId(), e);
            return Result.failure(ErrorDetail.of("JSON_SERIALIZATION_ERROR", ErrorType.SYSTEM_ERROR,
                String.format("Failed to serialize calculation results: %s", e.getMessage()),
                "calculation.serialization.error"));
        }
    }

    private ObjectNode createSummaryNode(RiskCalculationResult result) {
        ObjectNode summaryNode = objectMapper.createObjectNode();
        
        // Basic totals
        summaryNode.put("total_exposures", result.calculatedExposures().size());
        summaryNode.put("total_amount_eur", result.analysis().getTotalPortfolio().value());

        // Geographic breakdown
        ObjectNode geographicNode = createBreakdownNode(result.analysis().getGeographicBreakdown());
        summaryNode.set("geographic_breakdown", geographicNode);

        // Sector breakdown
        ObjectNode sectorNode = createBreakdownNode(result.analysis().getSectorBreakdown());
        summaryNode.set("sector_breakdown", sectorNode);

        // Concentration indices
        ObjectNode concentrationNode = objectMapper.createObjectNode();
        concentrationNode.put("herfindahl_geographic", result.analysis().getGeographicHHI().value());
        concentrationNode.put("herfindahl_sector", result.analysis().getSectorHHI().value());
        summaryNode.set("concentration_indices", concentrationNode);

        return summaryNode;
    }

    private ObjectNode createBreakdownNode(Breakdown breakdown) {
        ObjectNode breakdownNode = objectMapper.createObjectNode();
        
        Map<String, Share> shares = breakdown.shares();
        for (Map.Entry<String, Share> entry : shares.entrySet()) {
            ObjectNode shareNode = objectMapper.createObjectNode();
            shareNode.put("amount_eur", entry.getValue().amount().value());
            shareNode.put("percentage", entry.getValue().percentage().doubleValue());
            breakdownNode.set(entry.getKey().toLowerCase(), shareNode);
        }
        
        return breakdownNode;
    }

    private ArrayNode createExposuresNode(List<ProtectedExposure> exposures) {
        ArrayNode exposuresArray = objectMapper.createArrayNode();
        
        // Calculate total portfolio amount for percentage calculations
        double totalPortfolioAmount = exposures.stream()
            .mapToDouble(e -> e.getGrossExposure().value().doubleValue())
            .sum();
        
        for (ProtectedExposure exposure : exposures) {
            ObjectNode exposureNode = objectMapper.createObjectNode();
            
            // Basic exposure data
            exposureNode.put("exposure_id", exposure.getExposureId().value());
            
            // Amounts
            exposureNode.put("gross_exposure_eur", exposure.getGrossExposure().value().doubleValue());
            exposureNode.put("net_exposure_eur", exposure.getNetExposure().value().doubleValue());
            exposureNode.put("total_mitigation_eur", exposure.getTotalMitigation().value().doubleValue());
            
            // Percentage of total portfolio
            double eurAmount = exposure.getGrossExposure().value().doubleValue();
            double percentageOfTotal = totalPortfolioAmount > 0 
                ? (eurAmount / totalPortfolioAmount) * 100.0 
                : 0.0;
            exposureNode.put("percentage_of_total", percentageOfTotal);
            
            // Mitigation details (if any)
            if (exposure.hasMitigations()) {
                ArrayNode mitigationsArray = objectMapper.createArrayNode();
                for (var mitigation : exposure.getMitigations()) {
                    ObjectNode mitigationNode = objectMapper.createObjectNode();
                    mitigationNode.put("type", mitigation.getType().name());
                    mitigationNode.put("eur_value", mitigation.getEurValue().value().doubleValue());
                    mitigationsArray.add(mitigationNode);
                }
                exposureNode.set("mitigations", mitigationsArray);
            }
            
            exposuresArray.add(exposureNode);
        }
        
        return exposuresArray;
    }

    /**
     * Deserialize JSON string back to a structured object (for retrieval scenarios).
     * 
     * @param jsonString The JSON string to deserialize
     * @return Result containing the parsed JsonNode or error
     */
    public Result<JsonNode> deserializeFromJson(String jsonString) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            return Result.failure(ErrorDetail.of("NULL_JSON_STRING", ErrorType.VALIDATION_ERROR,
                "JSON string cannot be null or empty", "calculation.deserialization.null.json"));
        }

        try {
            JsonNode rootNode = objectMapper.readTree(jsonString);
            
            log.debug("Successfully deserialized calculation results JSON");
            return Result.success(rootNode);

        } catch (Exception e) {
            log.error("Failed to deserialize calculation results JSON", e);
            return Result.failure(ErrorDetail.of("JSON_DESERIALIZATION_ERROR", ErrorType.SYSTEM_ERROR,
                String.format("Failed to deserialize JSON: %s", e.getMessage()),
                "calculation.deserialization.error"));
        }
    }
}
