package com.bcbs239.regtech.reportgeneration.presentation.configuration;

import java.util.Map;

/**
 * Request DTO for updating report configuration command input
 */
public record ReportConfigurationCommandRequest(
    Map<String, Object> configuration
) {
    @SuppressWarnings("unchecked")
    public com.bcbs239.regtech.reportgeneration.application.configuration.UpdateReportConfigurationHandler.UpdateCommand 
            toCommand(Long bankId, String modifiedBy) {
        
        Map<String, Object> templateMap = (Map<String, Object>) configuration.get("template");
        Map<String, Object> frequencyMap = (Map<String, Object>) configuration.get("generationFrequency");
        Map<String, Object> schedulingMap = (Map<String, Object>) configuration.get("automaticScheduling");
        Map<String, Object> distributionMap = (Map<String, Object>) configuration.get("distribution");
        Map<String, Object> boiMap = (Map<String, Object>) distributionMap.get("bankOfItaly");

        return new com.bcbs239.regtech.reportgeneration.application.configuration.UpdateReportConfigurationHandler.UpdateCommand(
            bankId,
            (String) templateMap.get("templateName"),
            (String) templateMap.get("language"),
            (String) templateMap.get("outputFormat"),
            (String) frequencyMap.get("frequency"),
            ((Number) frequencyMap.get("submissionDaysAfterPeriod")).intValue(),
            (Boolean) schedulingMap.get("enabled"),
            (String) schedulingMap.get("dayOfWeek"),
            (String) schedulingMap.get("time"),
            (String) distributionMap.get("primaryEmail"),
            (String) distributionMap.get("ccEmail"),
            (Boolean) distributionMap.get("autoSendEnabled"),
            (Boolean) boiMap.get("autoSubmitEnabled"),
            modifiedBy
        );
    }
}
