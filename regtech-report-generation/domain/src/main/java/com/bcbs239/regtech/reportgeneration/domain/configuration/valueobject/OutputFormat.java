package com.bcbs239.regtech.reportgeneration.domain.configuration.valueobject;

import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OutputFormat {
    PDF("PDF", "application/pdf", ".pdf"),
    EXCEL("Excel (.xlsx)", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", ".xlsx"),
    BOTH("Entrambi i formati", "mixed", ".pdf,.xlsx");
    
    private final String displayName;
    private final String mimeType;
    private final String extension;
    
    public static Result<OutputFormat> of(String value) {
        if (value == null || value.isBlank()) {
            return Result.failure(ErrorDetail.of(
                "INVALID_OUTPUT_FORMAT",
                ErrorType.VALIDATION_ERROR,
                "Output format cannot be null or blank",
                "report.outputFormat.required"
            ));
        }
        
        String trimmed = value.trim().toUpperCase();
        
        // Support name or BOTH/ENTRAMBI
        for (OutputFormat format : values()) {
            if (format.name().equals(trimmed) || format.displayName.toUpperCase().contains(trimmed)) {
                return Result.success(format);
            }
        }
        
        // Special case for "BOTH" vs "ENTRAMBI"
        if (trimmed.equals("BOTH") || trimmed.equals("ENTRAMBI")) {
            return Result.success(BOTH);
        }
        
        return Result.failure(ErrorDetail.of(
            "INVALID_OUTPUT_FORMAT",
            ErrorType.VALIDATION_ERROR,
            "Invalid output format: " + value + ". Valid values: PDF, EXCEL, BOTH",
            "report.outputFormat.invalid"
        ));
    }
    
    public boolean includesPdf() {
        return this == PDF || this == BOTH;
    }
    
    public boolean includesExcel() {
        return this == EXCEL || this == BOTH;
    }
}
