package com.bcbs239.regtech.riskcalculation.infrastructure.database.repositories;

import com.bcbs239.regtech.riskcalculation.domain.exposure.BalanceSheetType;
import com.bcbs239.regtech.riskcalculation.domain.exposure.CounterpartyRef;
import com.bcbs239.regtech.riskcalculation.domain.exposure.ExposureClassification;
import com.bcbs239.regtech.riskcalculation.domain.exposure.ExposureRecording;
import com.bcbs239.regtech.riskcalculation.domain.exposure.InstrumentId;
import com.bcbs239.regtech.riskcalculation.domain.exposure.InstrumentType;
import com.bcbs239.regtech.riskcalculation.domain.exposure.MonetaryAmount;
import com.bcbs239.regtech.riskcalculation.domain.exposure.CalculationResultsDeserializationException;
import com.bcbs239.regtech.riskcalculation.domain.exposure.ExposureRepository;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.ExposureId;
import com.bcbs239.regtech.riskcalculation.infrastructure.database.mappers.ExposureMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * JPA implementation of ExposureRepository
 * Adapts Spring Data JPA repository to domain repository interface
 * 
 * <p><strong>Migration Notice:</strong> This repository is transitioning to a file-first architecture.
 * Database persistence methods are deprecated. Use {@link #loadFromJson(String)} to load exposures
 * from JSON files.</p>
 */
@Repository
@Slf4j
public class JpaExposureRepository implements ExposureRepository {
    
    private final SpringDataExposureRepository springDataRepository;
    private final ExposureMapper mapper;
    private final ObjectMapper objectMapper;
    
    public JpaExposureRepository(
        SpringDataExposureRepository springDataRepository,
        ExposureMapper mapper,
        ObjectMapper objectMapper
    ) {
        this.springDataRepository = springDataRepository;
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }
    

    @Override
    @Transactional(readOnly = true)
    public Optional<ExposureRecording> findById(ExposureId id) {
        return springDataRepository.findById(id.value())
            .map(mapper::toDomain);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<ExposureRecording> findByBatchId(String batchId) {
        return springDataRepository.findByBatchId(batchId).stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }
    
    /**
     * Load exposure recordings from JSON content.
     * 
     * <p>This method parses calculation results JSON and reconstructs ExposureRecording objects
     * from the "calculated_exposures" array. Since the JSON stores ProtectedExposure data
     * (calculation results), some fields are reconstructed with placeholder values where
     * the original raw exposure data is not available.</p>
     * 
     * <p><strong>Note:</strong> The JSON format stores calculation results, not raw exposure data.
     * Therefore, some ExposureRecording fields (instrumentId, counterparty details, classification)
     * are reconstructed with minimal/placeholder values. This is acceptable because the JSON
     * serves as the source of truth for calculated amounts and exposure IDs.</p>
     * 
     * @param jsonContent the JSON string containing calculation results
     * @return list of ExposureRecording objects parsed from JSON
     * @throws IllegalArgumentException if JSON content is null or empty
     * @throws CalculationResultsDeserializationException if JSON is malformed or missing required fields
     * 
     * <p>Requirement: 5.5 - Provide methods to download and parse JSON files for exposure details</p>
     */
    @Override
    public List<ExposureRecording> loadFromJson(String jsonContent) {
        if (jsonContent == null || jsonContent.trim().isEmpty()) {
            throw new IllegalArgumentException("JSON content cannot be null or empty");
        }
        
        try {
            log.debug("Loading exposures from JSON content");
            
            JsonNode rootNode = objectMapper.readTree(jsonContent);
            
            // Validate format
            if (!rootNode.has("format_version")) {
                throw new CalculationResultsDeserializationException(
                    "Missing format_version field in JSON"
                );
            }
            
            // Extract calculated_at timestamp for recordedAt field
            Instant recordedAt = Instant.now();
            if (rootNode.has("calculated_at")) {
                recordedAt = Instant.parse(rootNode.get("calculated_at").asText());
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
            
            List<ExposureRecording> exposures = new ArrayList<>();
            
            for (JsonNode exposureNode : exposuresNode) {
                ExposureRecording exposure = parseExposureFromJson(exposureNode, recordedAt);
                exposures.add(exposure);
            }
            
            log.debug("Successfully loaded {} exposures from JSON", exposures.size());
            return exposures;
            
        } catch (CalculationResultsDeserializationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to load exposures from JSON", e);
            throw new CalculationResultsDeserializationException(
                "Failed to parse JSON content: " + e.getMessage(),
                jsonContent,
                e
            );
        }
    }
    
    /**
     * Parse a single exposure from JSON node.
     * 
     * <p>Since the JSON stores ProtectedExposure (calculation results) rather than raw
     * ExposureRecording data, we reconstruct with available information and use placeholder
     * values for missing fields.</p>
     */
    private ExposureRecording parseExposureFromJson(JsonNode exposureNode, Instant recordedAt) {
        // Extract required fields
        String exposureId = getRequiredTextField(exposureNode, "exposure_id");
        BigDecimal grossExposureEur = new BigDecimal(
            getRequiredTextField(exposureNode, "gross_exposure_eur")
        );
        
        // Reconstruct ExposureRecording with available data
        // Note: Some fields use placeholder values since the JSON doesn't contain
        // the original raw exposure data (instrumentId, counterparty, classification)
        return ExposureRecording.reconstitute(
            ExposureId.of(exposureId),
            InstrumentId.of(exposureId), // Use exposure ID as instrument ID placeholder
            CounterpartyRef.of(
                "UNKNOWN", // Counterparty ID not in JSON
                "Unknown Counterparty", // Counterparty name not in JSON
                "UNKNOWN0000000000000" // LEI not in JSON (20 alphanumeric chars)
            ),
            MonetaryAmount.of(grossExposureEur, "EUR"), // JSON stores EUR amounts
            ExposureClassification.of(
                "UNKNOWN", // Product type not in JSON
                InstrumentType.OTHER, // Instrument type not in JSON
                BalanceSheetType.ON_BALANCE, // Balance sheet type not in JSON
                "XX" // Country code not in JSON
            ),
            recordedAt
        );
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
