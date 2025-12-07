package com.bcbs239.regtech.riskcalculation.infrastructure.database.repositories;

import com.bcbs239.regtech.riskcalculation.domain.persistence.CalculationResultsDeserializationException;
import com.bcbs239.regtech.riskcalculation.domain.persistence.MitigationRepository;
import com.bcbs239.regtech.riskcalculation.domain.protection.MitigationType;
import com.bcbs239.regtech.riskcalculation.domain.protection.RawMitigationData;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.ExposureId;
import com.bcbs239.regtech.riskcalculation.infrastructure.database.entities.MitigationEntity;
import com.bcbs239.regtech.riskcalculation.infrastructure.database.mappers.MitigationMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JPA implementation of MitigationRepository
 * Adapts Spring Data JPA repository to domain repository interface
 * 
 * <p><strong>Migration Notice:</strong> This repository is transitioning to a file-first architecture.
 * Database persistence methods are deprecated. Use {@link #loadFromJson(String)} to load mitigations
 * from JSON files.</p>
 */
@Repository
@Slf4j
public class JpaMitigationRepository implements MitigationRepository {
    
    private final SpringDataMitigationRepository springDataRepository;
    private final MitigationMapper mapper;
    private final ObjectMapper objectMapper;
    
    public JpaMitigationRepository(
        SpringDataMitigationRepository springDataRepository,
        MitigationMapper mapper,
        ObjectMapper objectMapper
    ) {
        this.springDataRepository = springDataRepository;
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }
    
    /**
     * @deprecated Database persistence is deprecated. Use JSON file storage instead.
     */
    @Override
    @Deprecated(since = "2.0", forRemoval = true)
    @Transactional
    public void save(ExposureId exposureId, String batchId, RawMitigationData mitigation) {
        log.warn("Using deprecated save() method. Database persistence will be removed in future release. " +
                "Use JSON file storage via CalculationResultsStorageService instead.");
        MitigationEntity entity = mapper.toEntity(exposureId, batchId, mitigation);
        springDataRepository.save(entity);
    }
    
    /**
     * @deprecated Database persistence is deprecated. Use JSON file storage instead.
     */
    @Override
    @Deprecated(since = "2.0", forRemoval = true)
    @Transactional
    public void saveAll(ExposureId exposureId, String batchId, List<RawMitigationData> mitigations) {
        log.warn("Using deprecated saveAll() method. Database persistence will be removed in future release. " +
                "Use JSON file storage via CalculationResultsStorageService instead.");
        List<MitigationEntity> entities = mitigations.stream()
            .map(mitigation -> mapper.toEntity(exposureId, batchId, mitigation))
            .collect(Collectors.toList());
        springDataRepository.saveAll(entities);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<RawMitigationData> findByExposureId(ExposureId exposureId) {
        return springDataRepository.findByExposureId(exposureId.value()).stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<RawMitigationData> findByBatchId(String batchId) {
        return springDataRepository.findByBatchId(batchId).stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }
    
    /**
     * Load mitigation data from JSON content.
     * 
     * <p>This method parses calculation results JSON and reconstructs RawMitigationData objects
     * from the "mitigations" arrays within each exposure in the "calculated_exposures" array.
     * Since the JSON stores EUR-converted mitigation values (calculation results), the mitigations
     * are reconstructed with EUR as the currency.</p>
     * 
     * <p><strong>Note:</strong> The JSON format stores calculation results with EUR-converted values,
     * not the original raw mitigation data with original currencies. Therefore, all reconstructed
     * RawMitigationData objects will have EUR as the currency. This is acceptable because the JSON
     * serves as the source of truth for calculated mitigation amounts.</p>
     * 
     * @param jsonContent the JSON string containing calculation results
     * @return list of RawMitigationData objects parsed from JSON
     * @throws IllegalArgumentException if JSON content is null or empty
     * @throws CalculationResultsDeserializationException if JSON is malformed or missing required fields
     * 
     * <p>Requirement: 5.5 - Provide methods to download and parse JSON files for mitigation details</p>
     */
    @Override
    public List<RawMitigationData> loadFromJson(String jsonContent) {
        if (jsonContent == null || jsonContent.trim().isEmpty()) {
            throw new IllegalArgumentException("JSON content cannot be null or empty");
        }
        
        try {
            log.debug("Loading mitigations from JSON content");
            
            JsonNode rootNode = objectMapper.readTree(jsonContent);
            
            // Validate format
            if (!rootNode.has("format_version")) {
                throw new CalculationResultsDeserializationException(
                    "Missing format_version field in JSON"
                );
            }
            
            // Extract exposures array
            if (!rootNode.has("calculated_exposures")) {
                throw new CalculationResultsDeserializationException(
                    "Missing calculated_exposures field in JSON"
                );
            }
            
            JsonNode exposuresNode = rootNode.get("calculated_exposures");
            if (!exposuresNode.isArray()) {
                throw new CalculationResultsDeserializationException(
                    "calculated_exposures must be an array"
                );
            }
            
            List<RawMitigationData> allMitigations = new ArrayList<>();
            
            // Iterate through exposures and extract mitigations
            for (JsonNode exposureNode : exposuresNode) {
                if (exposureNode.has("mitigations")) {
                    JsonNode mitigationsNode = exposureNode.get("mitigations");
                    
                    if (!mitigationsNode.isArray()) {
                        throw new CalculationResultsDeserializationException(
                            "mitigations field must be an array"
                        );
                    }
                    
                    for (JsonNode mitigationNode : mitigationsNode) {
                        RawMitigationData mitigation = parseMitigationFromJson(mitigationNode);
                        allMitigations.add(mitigation);
                    }
                }
            }
            
            log.debug("Successfully loaded {} mitigations from JSON", allMitigations.size());
            return allMitigations;
            
        } catch (CalculationResultsDeserializationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to load mitigations from JSON", e);
            throw new CalculationResultsDeserializationException(
                "Failed to parse JSON content: " + e.getMessage(),
                jsonContent,
                e
            );
        }
    }
    
    /**
     * Parse a single mitigation from JSON node.
     * 
     * <p>The JSON stores EUR-converted mitigation values, so all reconstructed
     * RawMitigationData objects will have EUR as the currency.</p>
     */
    private RawMitigationData parseMitigationFromJson(JsonNode mitigationNode) {
        // Extract required fields
        String typeStr = getRequiredTextField(mitigationNode, "type");
        BigDecimal eurValue = new BigDecimal(
            getRequiredTextField(mitigationNode, "eur_value")
        );
        
        // Parse mitigation type
        MitigationType type;
        try {
            type = MitigationType.valueOf(typeStr);
        } catch (IllegalArgumentException e) {
            throw new CalculationResultsDeserializationException(
                String.format("Invalid mitigation type: %s", typeStr)
            );
        }
        
        // Reconstruct RawMitigationData with EUR currency
        // Note: JSON stores EUR-converted values, not original currency values
        return RawMitigationData.of(type, eurValue, "EUR");
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
}
