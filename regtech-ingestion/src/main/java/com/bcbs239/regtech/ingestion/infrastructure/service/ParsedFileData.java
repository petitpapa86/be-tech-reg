package com.bcbs239.regtech.ingestion.infrastructure.service;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

/**
 * Represents parsed exposure data from JSON or Excel files
 */
@Getter
@Builder
public class ParsedFileData {
    private final List<ExposureRecord> exposures;
    private final int totalCount;
    private final String fileName;
    private final String contentType;

    @Getter
    @Builder
    public static class ExposureRecord {
        private final String exposureId;
        private final BigDecimal amount;
        private final String currency;
        private final String country;
        private final String sector;
        private final int lineNumber; // For error reporting
    }
}