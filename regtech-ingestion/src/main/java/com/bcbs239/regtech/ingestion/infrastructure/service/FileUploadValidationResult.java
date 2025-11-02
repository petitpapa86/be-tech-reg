package com.bcbs239.regtech.ingestion.infrastructure.service;

import lombok.Builder;
import lombok.Getter;

/**
 * Result of file upload validation containing validation status and file information
 */
@Getter
@Builder
public class FileUploadValidationResult {
    private final boolean valid;
    private final String fileName;
    private final String contentType;
    private final long fileSizeBytes;
    private final boolean isJson;
    private final boolean isExcel;
}