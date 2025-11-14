package com.bcbs239.regtech.ingestion.domain.file;

/**
 * Represents the format of an uploaded file.
 * This is a domain-level classification that the infrastructure layer
 * will detect based on content type, file extension, or content inspection.
 */
public enum FileFormat {
    /**
     * Excel format for bank statements (BCBS 239 compliant)
     */
    BANK_STATEMENT_EXCEL("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
    
    /**
     * JSON format for transaction data
     */
    TRANSACTION_JSON("application/json"),
    
    /**
     * CSV format for ledger entries
     */
    LEDGER_CSV("text/csv");
    
    private final String contentType;
    
    FileFormat(String contentType) {
        this.contentType = contentType;
    }
    
    public String getContentType() {
        return contentType;
    }
    
    /**
     * Detect file format from content type.
     */
    public static FileFormat fromContentType(String contentType) {
        if (contentType == null) {
            throw new IllegalArgumentException("Content type cannot be null");
        }
        
        for (FileFormat format : values()) {
            if (format.contentType.equalsIgnoreCase(contentType)) {
                return format;
            }
        }
        
        throw new IllegalArgumentException("Unsupported content type: " + contentType);
    }
    
    /**
     * Check if the format is supported for ingestion.
     */
    public boolean isSupported() {
        return this == BANK_STATEMENT_EXCEL || this == TRANSACTION_JSON;
    }
}
