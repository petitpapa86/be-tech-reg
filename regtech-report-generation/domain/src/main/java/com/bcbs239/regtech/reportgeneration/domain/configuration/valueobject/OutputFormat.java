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
        
        try {
            return Result.success(OutputFormat.valueOf(value.trim().toUpperCase()));
        } catch (IllegalArgumentException e) {
            return Result.failure(ErrorDetail.of(
                "INVALID_OUTPUT_FORMAT",
                ErrorType.VALIDATION_ERROR,
                "Invalid output format: " + value + ". Valid values: " +
                String.join(", ", java.util.Arrays.stream(values())
                    .map(Enum::name)
                    .toList()),
                "report.outputFormat.invalid"
            ));
        }
    }
    
    public boolean includesPdf() {
        return this == PDF || this == BOTH;
    }
    
    public boolean includesExcel() {
        return this == EXCEL || this == BOTH;
    }
}
