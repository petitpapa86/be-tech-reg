package com.bcbs239.regtech.core.infrastructure.storage;

import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Helper class for JSON parsing and validation in storage operations
 */
@Component
public class JsonStorageHelper {
    
    private static final Logger log = LoggerFactory.getLogger(JsonStorageHelper.class);
    
    private final ObjectMapper objectMapper;
    
    public JsonStorageHelper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    
    /**
     * Validates that the given content is valid JSON
     * 
     * @param content The content to validate
     * @return Result containing the content if valid, or error
     * @throws com.fasterxml.jackson.core.JsonProcessingException if JSON parsing fails
     */
    public Result<String> validateJson(String content) throws com.fasterxml.jackson.core.JsonProcessingException {
        if (content == null || content.isBlank()) {
            return Result.failure(
                ErrorDetail.of("JSON_CONTENT_REQUIRED", ErrorType.VALIDATION_ERROR, 
                    "Content is null or blank", "storage.json_content_required"));
        }
        
        // Let Jackson parsing exceptions propagate to GlobalExceptionHandler
        objectMapper.readTree(content);
        return Result.success(content);
    }
    
    /**
     * Pretty-prints JSON content
     * 
     * @param content The JSON content to format
     * @return Result containing formatted JSON, or error
     * @throws com.fasterxml.jackson.core.JsonProcessingException if JSON parsing fails
     */
    public Result<String> prettyPrint(String content) throws com.fasterxml.jackson.core.JsonProcessingException {
        // Let Jackson exceptions propagate to GlobalExceptionHandler
        Object json = objectMapper.readValue(content, Object.class);
        String formatted = objectMapper.writerWithDefaultPrettyPrinter()
            .writeValueAsString(json);
        return Result.success(formatted);
    }
    
    /**
     * Minifies JSON content (removes whitespace)
     * 
     * @param content The JSON content to minify
     * @return Result containing minified JSON, or error
     * @throws com.fasterxml.jackson.core.JsonProcessingException if JSON parsing fails
     */
    public Result<String> minify(String content) throws com.fasterxml.jackson.core.JsonProcessingException {
        // Let Jackson exceptions propagate to GlobalExceptionHandler
        Object json = objectMapper.readValue(content, Object.class);
        String minified = objectMapper.writeValueAsString(json);
        return Result.success(minified);
    }
}
