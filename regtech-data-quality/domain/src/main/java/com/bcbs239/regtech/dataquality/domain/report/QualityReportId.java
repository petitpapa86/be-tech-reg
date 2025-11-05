package com.bcbs239.regtech.dataquality.domain.report;

import java.util.Objects;
import java.util.UUID;

/**
 * Value object representing a unique quality report identifier.
 */
public record QualityReportId(String value) {
    
    private static final String PREFIX = "qr_";
    
    public QualityReportId {
        Objects.requireNonNull(value, "QualityReportId value cannot be null");
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException("QualityReportId value cannot be empty");
        }
        if (!value.startsWith(PREFIX)) {
            throw new IllegalArgumentException("QualityReportId must start with 'qr_'");
        }
    }
    
    /**
     * Generate a new unique quality report ID.
     */
    public static QualityReportId generate() {
        String uuid = UUID.randomUUID().toString();
        return new QualityReportId(PREFIX + uuid);
    }
    
    /**
     * Create a QualityReportId from an existing string value.
     */
    public static QualityReportId of(String value) {
        return new QualityReportId(value);
    }
    
    @Override
    public String toString() {
        return value;
    }
}

