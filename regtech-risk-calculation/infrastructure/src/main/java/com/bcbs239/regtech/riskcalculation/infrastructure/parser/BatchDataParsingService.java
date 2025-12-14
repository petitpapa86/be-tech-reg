package com.bcbs239.regtech.riskcalculation.domain.services;

import com.bcbs239.regtech.core.domain.shared.dto.BatchDataDTO;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.BankInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Domain service for parsing batch data from JSON content.
 * 
 * This service encapsulates the JSON parsing logic and validation,
 * providing a clean interface for the command handler to use.
 * It handles the technical concerns of JSON deserialization while
 * providing domain-meaningful results.
 */
@RequiredArgsConstructor
@Slf4j
@Service
public class BatchDataParsingService {
    
    private final ObjectMapper objectMapper;
    
    /**
     * Parse batch data from JSON content.
     * 
     * @param jsonContent The JSON content to parse
     * @return ParsedBatchData containing the batch data and extracted bank info
     * @throws BatchDataParsingException if parsing fails
     */
    public ParsedBatchData parseBatchData(String jsonContent) {
        try {
            log.info("Parsing batch data from JSON, size: {} bytes", jsonContent.length());
            
            BatchDataDTO batchData = objectMapper.readValue(jsonContent, BatchDataDTO.class);
            
            if (batchData == null) {
                throw new BatchDataParsingException("Failed to deserialize JSON: result is null");
            }
            
            // Extract bank info
            BankInfo bankInfo = extractBankInfo(batchData);
            
            // Validate batch data
            validateBatchData(batchData);
            
            log.info("Successfully parsed batch data with {} exposures and {} mitigations",
                batchData.exposures() != null ? batchData.exposures().size() : 0,
                batchData.creditRiskMitigation() != null ? batchData.creditRiskMitigation().size() : 0);
            
            return new ParsedBatchData(batchData, bankInfo);
            
        } catch (JsonProcessingException e) {
            log.error("Failed to parse JSON content", e);
            throw new BatchDataParsingException("JSON parsing failed: " + e.getMessage(), e);
        } catch (BatchDataParsingException e) {
            throw e; // Re-throw our own exceptions
        } catch (Exception e) {
            log.error("Unexpected error parsing batch data", e);
            throw new BatchDataParsingException("Batch data parsing failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Extract bank information from batch data.
     */
    private BankInfo extractBankInfo(BatchDataDTO batchData) {
        if (batchData.bankInfo() != null) {
            BankInfo bankInfo = BankInfo.fromDTO(batchData.bankInfo());
            log.info("Processing batch from bank: {} (ABI: {}, LEI: {})", 
                bankInfo.bankName(), bankInfo.abiCode(), bankInfo.leiCode());
            return bankInfo;
        } else {
            log.warn("No bank_info found in batch data, using default");
            return BankInfo.of("Unknown", "00000", "UNKNOWN");
        }
    }
    
    /**
     * Validate the parsed batch data.
     */
    private void validateBatchData(BatchDataDTO batchData) {
        if (batchData.exposures() == null || batchData.exposures().isEmpty()) {
            throw new BatchDataParsingException("No exposures found in batch data");
        }
        
        // Additional validation rules can be added here
    }
    
    /**
     * Value object containing parsed batch data and extracted bank info.
     */
    public record ParsedBatchData(
        BatchDataDTO batchData,
        BankInfo bankInfo
    ) {}
    
    /**
     * Exception thrown when batch data parsing fails.
     */
    public static class BatchDataParsingException extends RuntimeException {
        public BatchDataParsingException(String message) {
            super(message);
        }
        
        public BatchDataParsingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}