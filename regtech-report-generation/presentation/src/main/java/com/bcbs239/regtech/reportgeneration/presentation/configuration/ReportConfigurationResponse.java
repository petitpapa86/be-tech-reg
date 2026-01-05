package com.bcbs239.regtech.reportgeneration.presentation.configuration;

import java.time.Instant;

/**
 * Response DTO for report configuration
 */
public record ReportConfigurationResponse(
    Long bankId,
    String template,
    String language,
    String outputFormat,
    String frequency,
    int submissionDeadline,
    boolean autoGenerationEnabled,
    String scheduleDay,
    String scheduleTime,
    String primaryRecipient,
    String ccRecipient,
    boolean autoSendEnabled,
    boolean swiftAutoSubmitEnabled,
    String lastModified,
    String lastModifiedBy
) {
    /**
     * Map domain model → DTO
     */
    public static ReportConfigurationResponse from(
            com.bcbs239.regtech.reportgeneration.domain.configuration.ReportConfiguration config) {
        return new ReportConfigurationResponse(
            config.getBankId(),
            config.getTemplate().name(),
            config.getLanguage().name(),
            config.getOutputFormat().name(),
            config.getFrequency().name(),
            config.getSubmissionDeadline().getDays(),
            config.isAutoGenerationEnabled(),
            config.getScheduleDay().name(),
            config.getScheduleTime().toFormattedString(),
            config.getPrimaryRecipient().getEmail(),
            config.getCcRecipient().map(r -> r.getEmail()).orElse(null),
            config.isAutoSendEnabled(),
            config.isSwiftAutoSubmitEnabled(),
            config.getLastModified().toString(),
            config.getLastModifiedBy()
        );
    }
}
