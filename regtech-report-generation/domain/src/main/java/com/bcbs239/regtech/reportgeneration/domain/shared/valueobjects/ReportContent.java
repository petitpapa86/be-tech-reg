package com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects;

import java.util.Map;

/**
 * Report Content Value Object
 * 
 * Encapsulates report content with its associated metadata.
 * This ensures that content, filename, and metadata are always handled together.
 */
public record ReportContent(
    String content,
    String fileName,
    Map<String, String> metadata
) {
    public ReportContent {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Content cannot be null or blank");
        }
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("File name cannot be null or blank");
        }
        if (metadata == null) {
            throw new IllegalArgumentException("Metadata cannot be null");
        }
        
        // Make metadata immutable
        metadata = Map.copyOf(metadata);
    }
    
    public static ReportContent of(String content, String fileName, Map<String, String> metadata) {
        return new ReportContent(content, fileName, metadata);
    }
    
    public static ReportContent html(String htmlContent, String fileName, Map<String, String> metadata) {
        return new ReportContent(htmlContent, fileName, metadata);
    }
    
    public static ReportContent xbrl(String xbrlContent, String fileName, Map<String, String> metadata) {
        return new ReportContent(xbrlContent, fileName, metadata);
    }
    
    /**
     * Get content size in bytes
     */
    public long sizeInBytes() {
        return content.getBytes().length;
    }
    
    /**
     * Check if metadata contains a specific key
     */
    public boolean hasMetadata(String key) {
        return metadata.containsKey(key);
    }
    
    /**
     * Get metadata value by key
     */
    public String getMetadata(String key) {
        return metadata.get(key);
    }
}
