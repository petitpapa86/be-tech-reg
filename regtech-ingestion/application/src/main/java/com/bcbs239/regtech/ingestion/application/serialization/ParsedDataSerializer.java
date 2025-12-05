package com.bcbs239.regtech.ingestion.application.serialization;

import com.bcbs239.regtech.core.domain.shared.dto.BatchDataDTO;
import com.bcbs239.regtech.ingestion.domain.model.ParsedFileData;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Service responsible for serializing ParsedFileData to JSON format via BatchDataDTO.
 * Ensures consistent data format across all modules by using shared DTOs.
 * Uses the centralized ObjectMapper from JacksonConfiguration for consistency.
 */
@Service
public class ParsedDataSerializer {
    
    private static final Logger log = LoggerFactory.getLogger(ParsedDataSerializer.class);
    
    private final ObjectMapper objectMapper;
    
    public ParsedDataSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper.copy();
        // Enable pretty printing for better readability of stored files
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }
    
    /**
     * Serialize ParsedFileData to JSON InputStream via BatchDataDTO.
     * 
     * @param parsedData The parsed file data to serialize
     * @return InputStream containing the JSON representation
     * @throws IOException if serialization fails
     */
    public InputStream serializeToInputStream(ParsedFileData parsedData) throws IOException {
        if (parsedData == null) {
            throw new IllegalArgumentException("ParsedFileData cannot be null");
        }
        
        // Convert domain model to DTO
        BatchDataDTO dto = parsedData.toDTO();
        
        log.debug("Serializing ParsedFileData to JSON via BatchDataDTO - exposures: {}, mitigations: {}, bank: {}",
            dto.exposures() != null ? dto.exposures().size() : 0,
            dto.creditRiskMitigation() != null ? dto.creditRiskMitigation().size() : 0,
            dto.bankInfo() != null ? dto.bankInfo().bankName() : "null");
        
        // Serialize DTO to JSON
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        objectMapper.writeValue(outputStream, dto);
        
        byte[] jsonBytes = outputStream.toByteArray();
        
        log.info("Successfully serialized ParsedFileData to JSON - size: {} bytes, exposures: {}, bank: {}",
            jsonBytes.length,
            dto.exposures() != null ? dto.exposures().size() : 0,
            dto.bankInfo() != null ? dto.bankInfo().bankName() : "null");
        
        return new ByteArrayInputStream(jsonBytes);
    }
    
    /**
     * Serialize ParsedFileData to JSON byte array via BatchDataDTO.
     * 
     * @param parsedData The parsed file data to serialize
     * @return byte array containing the JSON representation
     * @throws IOException if serialization fails
     */
    public byte[] serializeToBytes(ParsedFileData parsedData) throws IOException {
        if (parsedData == null) {
            throw new IllegalArgumentException("ParsedFileData cannot be null");
        }
        
        // Convert domain model to DTO
        BatchDataDTO dto = parsedData.toDTO();
        
        log.debug("Serializing ParsedFileData to JSON bytes via BatchDataDTO - exposures: {}, mitigations: {}, bank: {}",
            dto.exposures() != null ? dto.exposures().size() : 0,
            dto.creditRiskMitigation() != null ? dto.creditRiskMitigation().size() : 0,
            dto.bankInfo() != null ? dto.bankInfo().bankName() : "null");
        
        // Serialize DTO to JSON
        byte[] jsonBytes = objectMapper.writeValueAsBytes(dto);
        
        log.info("Successfully serialized ParsedFileData to JSON bytes - size: {} bytes, exposures: {}, bank: {}",
            jsonBytes.length,
            dto.exposures() != null ? dto.exposures().size() : 0,
            dto.bankInfo() != null ? dto.bankInfo().bankName() : "null");
        
        return jsonBytes;
    }
}
