package com.bcbs239.regtech.dataquality.domain.report;

import java.util.Objects;

/**
 * Value object representing metadata about the source file.
 */
public record FileMetadata(
    String filename,
    String format,
    long size
) {
    public FileMetadata {
        Objects.requireNonNull(filename, "Filename cannot be null");
        Objects.requireNonNull(format, "Format cannot be null");
        if (size < 0) {
            throw new IllegalArgumentException("File size cannot be negative");
        }
    }

    public static FileMetadata of(String filename, String format, long size) {
        return new FileMetadata(filename, format, size);
    }
    
    public static FileMetadata empty() {
        return new FileMetadata("unknown", "unknown", 0L);
    }
}
