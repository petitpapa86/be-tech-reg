package com.bcbs239.regtech.riskcalculation.application.calculation;

import com.bcbs239.regtech.core.domain.shared.valueobjects.BankInfo;
import com.bcbs239.regtech.riskcalculation.domain.analysis.Breakdown;
import com.bcbs239.regtech.riskcalculation.domain.analysis.PortfolioAnalysis;
import com.bcbs239.regtech.riskcalculation.domain.analysis.Share;
import com.bcbs239.regtech.riskcalculation.domain.calculation.CalculationResultsSerializationException;
import com.bcbs239.regtech.riskcalculation.domain.calculation.RiskCalculationResult;
import com.bcbs239.regtech.riskcalculation.domain.protection.Mitigation;
import com.bcbs239.regtech.riskcalculation.domain.protection.ProtectedExposure;

import com.bcbs239.regtech.riskcalculation.domain.valuation.EurAmount;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Serializes risk calculation results to JSON format for storage in S3/filesystem.
 * Creates a comprehensive JSON structure containing both summary and detailed exposure data.
 * 
 * Requirements: 1.1, 1.2, 10.1, 10.2, 10.3, 10.4
 */
@Component
public class CalculationResultsJsonSerializer {

    private static final Logger log = LoggerFactory.getLogger(CalculationResultsJsonSerializer.class);
    private final ObjectMapper objectMapper;
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_INSTANT;
    private static final String FORMAT_VERSION = "1.0";

    public CalculationResultsJsonSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Serialize complete calculation results to JSON string.
     * Requirement: 1.1 - Serialize complete results to JSON format
     * Requirement: 1.2 - Include batch metadata, exposures, mitigations, and portfolio analysis
     * Requirement: 10.1 - Include format_version field
     * Requirement: 10.2 - Include batch_id and calculated_at timestamp
     * Requirement: 10.3 - Include summary section with totals and breakdowns
     * Requirement: 10.4 - Include exposures array with complete exposure details
     * 
     * @param result The risk calculation result to serialize
     * @return JSON string representation
     * @throws CalculationResultsSerializationException if serialization fails
     */
    public String serialize(RiskCalculationResult result) {
        Objects.requireNonNull(result, "Calculation result cannot be null");
        
        String batchId = result.batchId();
        
        try {
            ObjectNode rootNode = objectMapper.createObjectNode();

            // Requirement 10.1: Include format_version field
            rootNode.put("format_version", FORMAT_VERSION);
            
            // Requirement 10.2: Include batch_id and calculated_at timestamp
            rootNode.put("batch_id", batchId);
            rootNode.put("calculated_at", ISO_FORMATTER.format(result.ingestedAt()));
            
            // Bank info section
            ObjectNode bankInfoNode = createBankInfoNode(result.bankInfo());
            rootNode.set("bank_info", bankInfoNode);

            // Requirement 10.3: Include summary section with totals and breakdowns
            ObjectNode summaryNode = createSummaryNode(result);
            rootNode.set("summary", summaryNode);

            // Requirement 10.4: Include exposures array with complete exposure details
            ArrayNode exposuresNode = createExposuresNode(result.calculatedExposures());
            rootNode.set("calculated_exposures", exposuresNode);

            // Convert to JSON string with pretty printing
            String jsonString = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootNode);

            log.debug("Successfully serialized calculation results for batch: {}", batchId);
            return jsonString;

        } catch (Exception e) {
            log.error("Failed to serialize calculation results for batch: {}", batchId, e);
            throw new CalculationResultsSerializationException(
                String.format("Failed to serialize calculation results for batch %s: %s", batchId, e.getMessage()),
                batchId,
                e
            );
        }
    }

    /**
     * Create bank info node for JSON output.
     */
    private ObjectNode createBankInfoNode(BankInfo bankInfo) {
        ObjectNode bankInfoNode = objectMapper.createObjectNode();
        bankInfoNode.put("bank_name", bankInfo.bankName());
        bankInfoNode.put("abi_code", bankInfo.abiCode());
        bankInfoNode.put("lei_code", bankInfo.leiCode());
        return bankInfoNode;
    }
    
    /**
     * Create summary node with totals and breakdowns.
     */
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
     * Deserialize JSON string back to RiskCalculationResult.
     * Requirement: 7.4 - Handle CalculationResultsDeserializationException for deserialization errors
     * 
     * @param json The JSON string to deserialize
     * @return Reconstituted RiskCalculationResult
     * @throws CalculationResultsDeserializationException if deserialization fails
     */
    public RiskCalculationResult deserialize(String json) {
        if (json == null || json.trim().isEmpty()) {
            throw new CalculationResultsDeserializationException("JSON string cannot be null or empty");
        }

        try {
            JsonNode rootNode = objectMapper.readTree(json);
            
            // Validate format version
            if (!rootNode.has("format_version")) {
                throw new CalculationResultsDeserializationException("Missing format_version field in JSON");
            }
            
            String formatVersion = rootNode.get("format_version").asText();
            if (!FORMAT_VERSION.equals(formatVersion)) {
                log.warn("JSON format version mismatch. Expected: {}, Found: {}", FORMAT_VERSION, formatVersion);
                // Continue with deserialization but log warning for potential compatibility issues
            }
            
            // Extract basic fields
            String batchId = getRequiredTextField(rootNode, "batch_id");
            Instant calculatedAt = Instant.parse(getRequiredTextField(rootNode, "calculated_at"));
            
            // Extract bank info
            JsonNode bankInfoNode = getRequiredNode(rootNode, "bank_info");
            BankInfo bankInfo = BankInfo.of(
                getRequiredTextField(bankInfoNode, "bank_name"),
                getRequiredTextField(bankInfoNode, "abi_code"),
                getRequiredTextField(bankInfoNode, "lei_code")
            );
            
            // Extract summary for portfolio analysis reconstruction
            JsonNode summaryNode = getRequiredNode(rootNode, "summary");
            BigDecimal totalAmountEur = new BigDecimal(getRequiredTextField(summaryNode, "total_amount_eur"));
            
            // Extract geographic breakdown
            JsonNode geographicNode = getRequiredNode(summaryNode, "geographic_breakdown");
            Breakdown geographicBreakdown = deserializeBreakdown(geographicNode, totalAmountEur);
            
            // Extract sector breakdown
            JsonNode sectorNode = getRequiredNode(summaryNode, "sector_breakdown");
            Breakdown sectorBreakdown = deserializeBreakdown(sectorNode, totalAmountEur);
            
            // Extract concentration indices
            JsonNode concentrationNode = getRequiredNode(summaryNode, "concentration_indices");
            BigDecimal geographicHHI = new BigDecimal(getRequiredTextField(concentrationNode, "herfindahl_geographic"));
            BigDecimal sectorHHI = new BigDecimal(getRequiredTextField(concentrationNode, "herfindahl_sector"));
            
            // Reconstitute portfolio analysis
            PortfolioAnalysis analysis = PortfolioAnalysis.reconstitute(
                batchId,
                EurAmount.of(totalAmountEur),
                geographicBreakdown,
                sectorBreakdown,
                com.bcbs239.regtech.riskcalculation.domain.analysis.HHI.calculate(geographicBreakdown),
                com.bcbs239.regtech.riskcalculation.domain.analysis.HHI.calculate(sectorBreakdown),
                calculatedAt
            );
            
            // Extract exposures
            JsonNode exposuresNode = getRequiredNode(rootNode, "calculated_exposures");
            List<ProtectedExposure> exposures = deserializeExposures(exposuresNode);
            
            log.debug("Successfully deserialized calculation results for batch: {}", batchId);
            
            return new RiskCalculationResult(
                batchId,
                bankInfo,
                exposures,
                analysis,
                calculatedAt
            );

        } catch (CalculationResultsDeserializationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to deserialize calculation results JSON", e);
            throw new CalculationResultsDeserializationException(
                String.format("Failed to deserialize JSON: %s", e.getMessage()),
                json,
                e
            );
        }
    }
    
    /**
     * Helper method to get a required text field from a JSON node.
     */
    private String getRequiredTextField(JsonNode node, String fieldName) {
        if (!node.has(fieldName)) {
            throw new CalculationResultsDeserializationException(
                String.format("Missing required field: %s", fieldName)
            );
        }
        return node.get(fieldName).asText();
    }
    
    /**
     * Helper method to get a required object node from a JSON node.
     */
    private JsonNode getRequiredNode(JsonNode node, String fieldName) {
        if (!node.has(fieldName)) {
            throw new CalculationResultsDeserializationException(
                String.format("Missing required node: %s", fieldName)
            );
        }
        return node.get(fieldName);
    }
    
    /**
     * Deserialize a breakdown from JSON.
     */
    private Breakdown deserializeBreakdown(JsonNode breakdownNode, BigDecimal total) {
        Map<String, Share> shares = new java.util.HashMap<>();
        
        breakdownNode.fields().forEachRemaining(entry -> {
            String category = entry.getKey();
            JsonNode shareNode = entry.getValue();
            
            BigDecimal amount = new BigDecimal(shareNode.get("amount_eur").asText());
            BigDecimal percentage = new BigDecimal(shareNode.get("percentage").asText());
            
            Share share = new Share(EurAmount.of(amount), percentage);
            shares.put(category, share);
        });
        
        return new Breakdown(shares);
    }
    
    /**
     * Deserialize exposures from JSON array.
     */
    private List<ProtectedExposure> deserializeExposures(JsonNode exposuresNode) {
        if (!exposuresNode.isArray()) {
            throw new CalculationResultsDeserializationException("calculated_exposures must be an array");
        }
        
        List<ProtectedExposure> exposures = new java.util.ArrayList<>();
        
        for (JsonNode exposureNode : exposuresNode) {
            ProtectedExposure exposure = deserializeExposure(exposureNode);
            exposures.add(exposure);
        }
        
        return exposures;
    }
    
    /**
     * Deserialize a single exposure from JSON.
     */
    private ProtectedExposure deserializeExposure(JsonNode exposureNode) {
        String exposureId = getRequiredTextField(exposureNode, "exposure_id");
        BigDecimal grossExposure = new BigDecimal(getRequiredTextField(exposureNode, "gross_exposure_eur"));
        BigDecimal netExposure = new BigDecimal(getRequiredTextField(exposureNode, "net_exposure_eur"));
        
        // Deserialize mitigations if present
        List<Mitigation> mitigations = new java.util.ArrayList<>();
        if (exposureNode.has("mitigations")) {
            JsonNode mitigationsNode = exposureNode.get("mitigations");
            if (mitigationsNode.isArray()) {
                for (JsonNode mitigationNode : mitigationsNode) {
                    String type = getRequiredTextField(mitigationNode, "type");
                    BigDecimal eurValue = new BigDecimal(getRequiredTextField(mitigationNode, "eur_value"));
                    
                    Mitigation mitigation = Mitigation.reconstitute(
                        com.bcbs239.regtech.riskcalculation.domain.protection.MitigationType.valueOf(type),
                        EurAmount.of(eurValue)
                    );
                    mitigations.add(mitigation);
                }
            }
        }
        
        // Reconstitute the protected exposure
        return ProtectedExposure.calculate(
            com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.ExposureId.of(exposureId),
            EurAmount.of(grossExposure),
            mitigations
        );
    }
}
