package com.bcbs239.regtech.ingestion.domain.batch;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.UUID;

/**
 * Value object representing a unique batch identifier.
 * Format: "batch_YYYYMMDD_HHMMSS_UUID"
 */
public record BatchId(@JsonValue String value) {
    
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final String PREFIX = "batch_";
    
    @JsonCreator
    public BatchId {
        Objects.requireNonNull(value, "BatchId value cannot be null");
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException("BatchId value cannot be empty");
        }
        if (!value.startsWith(PREFIX)) {
            throw new IllegalArgumentException("BatchId must start with 'batch_'");
        }
    }
    
    /**
     * Generate a new unique batch ID with the required format.
     */
    public static BatchId generate() {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        String uuid = UUID.randomUUID().toString();
        return new BatchId(PREFIX + timestamp + "_" + uuid);
    }
    
    /**
     * Create a BatchId from an existing string value.
     */
    public static BatchId of(String value) {
        return new BatchId(value);
    }
    

}

