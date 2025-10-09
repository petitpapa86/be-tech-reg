package com.bcbs239.regtech.billing.domain.invoices;

import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.core.shared.ErrorDetail;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Invoice number value object with generation logic
 */
public record InvoiceNumber(String value) {
    
    private static final AtomicLong SEQUENCE = new AtomicLong(1);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    
    public InvoiceNumber {
        Objects.requireNonNull(value, "InvoiceNumber value cannot be null");
    }
    
    /**
     * Generate a new unique invoice number with format: INV-YYYYMMDD-NNNN
     */
    public static InvoiceNumber generate() {
        String dateStr = LocalDate.now().format(DATE_FORMAT);
        long sequence = SEQUENCE.getAndIncrement();
        String invoiceNumber = String.format("INV-%s-%04d", dateStr, sequence);
        return new InvoiceNumber(invoiceNumber);
    }
    
    /**
     * Generate invoice number for specific date (useful for testing)
     */
    public static InvoiceNumber generateForDate(LocalDate date) {
        String dateStr = date.format(DATE_FORMAT);
        long sequence = SEQUENCE.getAndIncrement();
        String invoiceNumber = String.format("INV-%s-%04d", dateStr, sequence);
        return new InvoiceNumber(invoiceNumber);
    }
    
    /**
     * Create InvoiceNumber from string value with validation
     */
    public static Result<InvoiceNumber> fromString(String value) {
        if (value == null) {
            return Result.failure(new ErrorDetail("INVALID_INVOICE_NUMBER", "InvoiceNumber value cannot be null"));
        }
        if (value.trim().isEmpty()) {
            return Result.failure(new ErrorDetail("INVALID_INVOICE_NUMBER", "InvoiceNumber value cannot be empty"));
        }
        if (!value.matches("^INV-\\d{8}-\\d{4}$")) {
            return Result.failure(new ErrorDetail("INVALID_INVOICE_NUMBER", 
                "InvoiceNumber must follow format INV-YYYYMMDD-NNNN"));
        }
        return Result.success(new InvoiceNumber(value));
    }
    
    /**
     * Extract the date portion from the invoice number
     */
    public Result<LocalDate> extractDate() {
        try {
            String datePart = value.substring(4, 12); // Extract YYYYMMDD part
            return Result.success(LocalDate.parse(datePart, DATE_FORMAT));
        } catch (Exception e) {
            return Result.failure(new ErrorDetail("INVALID_INVOICE_NUMBER_FORMAT", 
                "Cannot extract date from invoice number: " + value));
        }
    }
    
    /**
     * Extract the sequence number from the invoice number
     */
    public Result<Integer> extractSequence() {
        try {
            String sequencePart = value.substring(13); // Extract NNNN part
            return Result.success(Integer.parseInt(sequencePart));
        } catch (Exception e) {
            return Result.failure(new ErrorDetail("INVALID_INVOICE_NUMBER_FORMAT", 
                "Cannot extract sequence from invoice number: " + value));
        }
    }
    
    @Override
    public String toString() {
        return value;
    }

    /**
     * Get the string value of this InvoiceNumber
     */
    public String getValue() {
        return value;
    }
}
