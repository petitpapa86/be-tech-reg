package com.bcbs239.regtech.reportgeneration.domain.configuration;

import com.bcbs239.regtech.reportgeneration.domain.configuration.valueobject.*;
import com.bcbs239.regtech.core.domain.shared.Maybe;
import lombok.Builder;
import lombok.Value;
import java.time.Instant;

/**
 * ReportConfiguration - Aggregate Root
 * 
 * Represents report generation and distribution settings.
 * Immutable - use builder for updates.
 */
@Value
@Builder(toBuilder = true)
public class ReportConfiguration {
    
    // Bank Identifier
    Long bankId;
    
    // Template & Format
    ReportTemplate template;
    ReportLanguage language;
    OutputFormat outputFormat;
    
    // Scheduling
    ReportFrequency frequency;
    SubmissionDeadline submissionDeadline;
    boolean autoGenerationEnabled;
    ScheduleDay scheduleDay;
    ScheduleTime scheduleTime;
    
    // Distribution
    EmailRecipient primaryRecipient;
    Maybe<EmailRecipient> ccRecipient;
    boolean autoSendEnabled;
    boolean swiftAutoSubmitEnabled;  // Currently in development
    
    // Metadata
    Instant lastModified;
    String lastModifiedBy;
    
    /**
     * Domain behavior: Is automatic generation enabled?
     */
    public boolean isAutomationEnabled() {
        return autoGenerationEnabled && autoSendEnabled;
    }
    
    /**
     * Domain behavior: Should generate PDF?
     */
    public boolean shouldGeneratePdf() {
        return outputFormat.includesPdf();
    }
    
    /**
     * Domain behavior: Should generate Excel?
     */
    public boolean shouldGenerateExcel() {
        return outputFormat.includesExcel();
    }
    
    /**
     * Domain behavior: Is this a high-frequency report?
     */
    public boolean isHighFrequency() {
        return frequency == ReportFrequency.MONTHLY;
    }
    
    /**
     * Domain behavior: Has tight deadline?
     */
    public boolean hasTightDeadline() {
        return submissionDeadline.isTight();
    }
    
    /**
     * Domain method: Update configuration with new values
     * 
     * @return new immutable ReportConfiguration instance
     */
    public ReportConfiguration update(
            Long bankId,
            ReportTemplate template,
            ReportLanguage language,
            OutputFormat outputFormat,
            ReportFrequency frequency,
            SubmissionDeadline submissionDeadline,
            boolean autoGenerationEnabled,
            ScheduleDay scheduleDay,
            ScheduleTime scheduleTime,
            EmailRecipient primaryRecipient,
            Maybe<EmailRecipient> ccRecipient,
            boolean autoSendEnabled,
            boolean swiftAutoSubmitEnabled,
            String modifiedBy) {
        
        return this.toBuilder()
                .bankId(bankId)
                .template(template)
                .language(language)
                .outputFormat(outputFormat)
                .frequency(frequency)
                .submissionDeadline(submissionDeadline)
                .autoGenerationEnabled(autoGenerationEnabled)
                .scheduleDay(scheduleDay)
                .scheduleTime(scheduleTime)
                .primaryRecipient(primaryRecipient)
                .ccRecipient(ccRecipient)
                .autoSendEnabled(autoSendEnabled)
                .swiftAutoSubmitEnabled(swiftAutoSubmitEnabled)
                .lastModified(Instant.now())
                .lastModifiedBy(modifiedBy)
                .build();
    }
}
