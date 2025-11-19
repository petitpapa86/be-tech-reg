package com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects;

import lombok.NonNull;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Reporting date value object wrapping LocalDate
 * Represents the date for which the report is generated
 */
public record ReportingDate(@NonNull LocalDate value) {
    
    private static final DateTimeFormatter DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter FILE_NAME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    /**
     * Compact constructor with validation
     */
    public ReportingDate {
        if (value == null) {
            throw new IllegalArgumentException("Reporting date cannot be null");
        }
        if (value.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Reporting date cannot be in the future: " + value);
        }
    }
    
    /**
     * Create reporting date from LocalDate
     */
    public static ReportingDate of(LocalDate date) {
        return new ReportingDate(date);
    }
    
    /**
     * Create reporting date from string (yyyy-MM-dd format)
     */
    public static ReportingDate fromString(String dateString) {
        return new ReportingDate(LocalDate.parse(dateString, DISPLAY_FORMATTER));
    }
    
    /**
     * Create reporting date for today
     */
    public static ReportingDate today() {
        return new ReportingDate(LocalDate.now());
    }
    
    /**
     * Create reporting date for yesterday
     */
    public static ReportingDate yesterday() {
        return new ReportingDate(LocalDate.now().minusDays(1));
    }
    
    /**
     * Format for display (yyyy-MM-dd)
     */
    public String toDisplayString() {
        return value.format(DISPLAY_FORMATTER);
    }
    
    /**
     * Format for file names (yyyy-MM-dd)
     */
    public String toFileNameString() {
        return value.format(FILE_NAME_FORMATTER);
    }
    
    /**
     * Check if this date is before another date
     */
    public boolean isBefore(ReportingDate other) {
        return value.isBefore(other.value);
    }
    
    /**
     * Check if this date is after another date
     */
    public boolean isAfter(ReportingDate other) {
        return value.isAfter(other.value);
    }
    
    @Override
    public String toString() {
        return toDisplayString();
    }
}
