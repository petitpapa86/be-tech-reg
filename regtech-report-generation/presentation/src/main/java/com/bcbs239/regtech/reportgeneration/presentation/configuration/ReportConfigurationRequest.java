package com.bcbs239.regtech.reportgeneration.presentation.configuration;

import jakarta.validation.constraints.*;

/**
 * Request DTO for updating report configuration
 * Validation at the boundary
 */
public record ReportConfigurationRequest(
    
    @NotNull(message = "Bank ID is required")
    Long bankId,
    
    @NotBlank(message = "Template is required")
    String template,
    
    @NotBlank(message = "Language is required")
    String language,
    
    @NotBlank(message = "Output format is required")
    String outputFormat,
    
    @NotBlank(message = "Frequency is required")
    String frequency,
    
    @Min(value = 1, message = "Submission deadline must be at least 1 day")
    @Max(value = 30, message = "Submission deadline cannot exceed 30 days")
    int submissionDeadline,
    
    boolean autoGenerationEnabled,
    
    @NotBlank(message = "Schedule day is required")
    String scheduleDay,
    
    @NotBlank(message = "Schedule time is required")
    @Pattern(regexp = "([01][0-9]|2[0-3]):[0-5][0-9]", message = "Invalid time format (expected HH:mm)")
    String scheduleTime,
    
    @NotBlank(message = "Primary recipient email is required")
    @Email(message = "Invalid primary recipient email format")
    String primaryRecipient,
    
    @Email(message = "Invalid CC recipient email format")
    String ccRecipient,
    
    boolean autoSendEnabled,
    
    boolean swiftAutoSubmitEnabled
) {
    /**
     * Map DTO → Command
     */
    public com.bcbs239.regtech.reportgeneration.application.configuration.UpdateReportConfigurationHandler.UpdateCommand 
            toCommand(String modifiedBy) {
        // Convert frequency string to domain enum when possible; pass null if invalid so handler will validate
        com.bcbs239.regtech.reportgeneration.domain.configuration.valueobject.ReportFrequency frequencyEnum = null;
        if (frequency != null) {
            var fr = com.bcbs239.regtech.reportgeneration.domain.configuration.valueobject.ReportFrequency.of(frequency);
            if (fr.isSuccess()) {
                frequencyEnum = fr.getValueOrThrow();
            }
        }

        return new com.bcbs239.regtech.reportgeneration.application.configuration.UpdateReportConfigurationHandler.UpdateCommand(
            bankId,
            template,
            language,
            outputFormat,
            frequencyEnum,
            submissionDeadline,
            autoGenerationEnabled,
            scheduleDay,
            scheduleTime,
            primaryRecipient,
            ccRecipient,
            autoSendEnabled,
            swiftAutoSubmitEnabled,
            modifiedBy
        );
    }
}
