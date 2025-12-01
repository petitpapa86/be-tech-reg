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
            rootNode.put("bank_id", result.bankInfo().getBankId());
            rootNode.put("bank_name", result.bankInfo().getBankName());

            // Summary section
            ObjectNode summaryNode = createSummaryNode(result);
            rootNode.set("summary", summaryNode);

            // Detailed exposures section
            ArrayNode exposuresNode = createExposuresNode(result.getCalculatedExposures());
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
        summaryNode.put("total_exposures", result.totalExposures());
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
            .mapToDouble(e -> e.getExposure().getAmount().getEurAmount().getValue())
            .sum();
        
        for (ProtectedExposure exposure : exposures) {
            ObjectNode exposureNode = objectMapper.createObjectNode();
            
            // Basic exposure data
            exposureNode.put("instrument_id", exposure.getExposure().getInstrumentId().getValue());
            exposureNode.put("counterparty_ref", exposure.getExposure().getCounterpartyRef().getValue());
            
            // Amounts
            exposureNode.put("original_amount", exposure.getExposure().getAmount().getOriginalAmount());
            exposureNode.put("original_currency", exposure.getExposure().getAmount().getOriginalCurrency());
            exposureNode.put("eur_amount", exposure.getExposure().getAmount().getEurAmount().getValue());
            exposureNode.put("mitigated_amount_eur", exposure.getMitigatedAmount().getValue());
            
            // Exchange rate used for conversion
            double eurAmount = exposure.getExposure().getAmount().getEurAmount().getValue();
            double originalAmount = exposure.getExposure().getAmount().getOriginalAmount();
            double exchangeRate = originalAmount > 0 ? eurAmount / originalAmount : 1.0;
            exposureNode.put("exchange_rate_used", exchangeRate);
            
            // Percentage of total portfolio
            double percentageOfTotal = totalPortfolioAmount > 0 
                ? (eurAmount / totalPortfolioAmount) * 100.0 
                : 0.0;
            exposureNode.put("percentage_of_total", percentageOfTotal);
            
            // Classification
            exposureNode.put("country", exposure.getClassification().getCountryCode());
            exposureNode.put("geographic_region", exposure.getClassification().getGeographicRegion());
            exposureNode.put("economic_sector", exposure.getClassification().getEconomicSector().name());
            
            // Mitigation details (if any)
            if (exposure.getMitigation() != null) {
                ObjectNode mitigationNode = objectMapper.createObjectNode();
                mitigationNode.put("type", exposure.getMitigation().getType());
                mitigationNode.put("coverage_ratio", exposure.getMitigation().getCoverageRatio());
                mitigationNode.put("provider", exposure.getMitigation().getProvider());
                exposureNode.set("mitigation", mitigationNode);
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
