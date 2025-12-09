package com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects;

/**
 * File size value object with human-readable formatting
 * Immutable value object representing file size in bytes
 */
public record FileSize(long bytes) {
    
    /**
     * Compact constructor with validation
     */
    public FileSize {
        if (bytes < 0) {
            throw new IllegalArgumentException("File size cannot be negative: " + bytes);
        }
    }
    
    /**
     * Create FileSize from bytes
     */
    public static FileSize ofBytes(long bytes) {
        return new FileSize(bytes);
    }
    
    /**
     * Create FileSize from kilobytes
     */
    public static FileSize ofKilobytes(long kilobytes) {
        return new FileSize(kilobytes * 1024);
    }
    
    /**
     * Create FileSize from megabytes
     */
    public static FileSize ofMegabytes(long megabytes) {
        return new FileSize(megabytes * 1024 * 1024);
    }
    
    /**
     * Format file size as human-readable string
     * Examples: "1.5 KB", "2.3 MB", "1.2 GB"
     */
    public String toHumanReadable() {
        if (bytes < 1024) {
            return bytes + " B";
        }
        
        double kb = bytes / 1024.0;
        if (kb < 1024) {
            return String.format("%.1f KB", kb);
        }
        
        double mb = kb / 1024.0;
        if (mb < 1024) {
            return String.format("%.1f MB", mb);
        }
        
        double gb = mb / 1024.0;
        return String.format("%.1f GB", gb);
    }
    
    /**
     * Get size in kilobytes
     */
    public double toKilobytes() {
        return bytes / 1024.0;
    }
    
    /**
     * Get size in megabytes
     */
    public double toMegabytes() {
        return bytes / (1024.0 * 1024.0);
    }
    
    /**
     * Get size in gigabytes
     */
    public double toGigabytes() {
        return bytes / (1024.0 * 1024.0 * 1024.0);
    }
    
    @Override
    public String toString() {
        return toHumanReadable();
    }
}
