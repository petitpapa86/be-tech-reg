package com.bcbs239.regtech.reportgeneration.presentation.configuration;

import java.util.Map;

/**
 * Hierarchical Response DTO for report configuration read model
 */
public record ReportConfigurationReadModelResponse(
    Map<String, Object> configuration,
    Map<String, String> emailTemplatePreview,
    Map<String, Object> configurationStatus,
    Map<String, Object> systemStatus
) {
    public static ReportConfigurationReadModelResponse from(
            com.bcbs239.regtech.reportgeneration.domain.configuration.ReportConfiguration config) {
        
        Map<String, Object> configuration = Map.of(
            "template", Map.of(
                "templateName", config.getTemplate().getDisplayName(),
                "language", config.getLanguage().getCode().toUpperCase(),
                "outputFormat", config.getOutputFormat().name()
            ),
            "generationFrequency", Map.of(
                "frequency", config.getFrequency().name(),
                "submissionDaysAfterPeriod", config.getSubmissionDeadline().getDays()
            ),
            "automaticScheduling", Map.of(
                "enabled", config.isAutoGenerationEnabled(),
                "dayOfWeek", config.getScheduleDay().name(),
                "time", config.getScheduleTime().toFormattedString()
            ),
            "distribution", Map.of(
                "primaryEmail", config.getPrimaryRecipient().getEmail(),
                "ccEmail", config.getCcRecipient().map(r -> r.getEmail()).orElse(""),
                "autoSendEnabled", config.isAutoSendEnabled(),
                "bankOfItaly", Map.of(
                    "autoSubmitEnabled", config.isSwiftAutoSubmitEnabled(),
                    "channel", "SWIFT"
                )
            )
        );

        Map<String, String> emailTemplatePreview = Map.of(
            "subject", "Report BCBS 239 - Grandi Esposizioni - [MESE/ANNO]",
            "body", """
                Gentile Team Compliance,

                In allegato il report BCBS 239 sulle Grandi Esposizioni per il periodo di riferimento [MESE/ANNO].

                Riepilogo:
                - Punteggio Conformità: [XX]%
                - Qualità Dati: [XX]%
                - Violazioni Critiche: [N]
                - Grandi Esposizioni: [N]

                Cordiali saluti,
                Sistema ComplianceCore
                """
        );

        Map<String, Object> configurationStatus = Map.of(
            "templateConfigured", true,
            "frequencyConfigured", true,
            "automaticSchedulingActive", config.isAutoGenerationEnabled(),
            "distributionConfigured", true,
            "swiftAutoSubmitStatus", "IN_DEVELOPMENT"
        );

        Map<String, Object> systemStatus = Map.of(
            "valid", true,
            "lastModifiedAt", config.getLastModified().toString(),
            "lastModifiedBy", config.getLastModifiedBy(),
            "nextScheduledReport", "2025-01-06T09:00:00" // Mocked as per requirement
        );

        return new ReportConfigurationReadModelResponse(
            configuration,
            emailTemplatePreview,
            configurationStatus,
            systemStatus
        );
    }
}
