package com.bcbs239.regtech.reportgeneration.application.configuration;

import com.bcbs239.regtech.reportgeneration.domain.configuration.ReportConfiguration;
import com.bcbs239.regtech.reportgeneration.domain.configuration.ReportConfigurationRepository;
import com.bcbs239.regtech.reportgeneration.domain.configuration.valueobject.*;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.core.domain.shared.Maybe;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Command Handler: Update Report Configuration
 * 
 * Returns Result<ReportConfiguration> because:
 * - Value object construction can fail
 * - Validation can fail
 */
@Service
@RequiredArgsConstructor
public class UpdateReportConfigurationHandler {
    
    private final ReportConfigurationRepository repository;
    
    @Value
    public static class UpdateCommand {
        Long bankId;
        String template;
        String language;
        String outputFormat;
        String frequency;
        int submissionDeadline;
        boolean autoGenerationEnabled;
        String scheduleDay;
        String scheduleTime;
        String primaryRecipient;
        String ccRecipient;
        boolean autoSendEnabled;
        boolean swiftAutoSubmitEnabled;
        String modifiedBy;
    }
    
    @Transactional
    public Result<ReportConfiguration> handle(UpdateCommand command) {
        // Create value objects using smart constructors
        Result<ReportTemplate> templateResult = ReportTemplate.of(command.template);
        if (templateResult.isFailure()) {
            return Result.failure(templateResult.errors());
        }
        
        Result<ReportLanguage> languageResult = ReportLanguage.of(command.language);
        if (languageResult.isFailure()) {
            return Result.failure(languageResult.errors());
        }
        
        Result<OutputFormat> outputFormatResult = OutputFormat.of(command.outputFormat);
        if (outputFormatResult.isFailure()) {
            return Result.failure(outputFormatResult.errors());
        }
        
        Result<ReportFrequency> frequencyResult = ReportFrequency.of(command.frequency);
        if (frequencyResult.isFailure()) {
            return Result.failure(frequencyResult.errors());
        }
        
        Result<SubmissionDeadline> deadlineResult = SubmissionDeadline.of(command.submissionDeadline);
        if (deadlineResult.isFailure()) {
            return Result.failure(deadlineResult.errors());
        }
        
        Result<ScheduleDay> scheduleDayResult = ScheduleDay.of(command.scheduleDay);
        if (scheduleDayResult.isFailure()) {
            return Result.failure(scheduleDayResult.errors());
        }
        
        Result<ScheduleTime> scheduleTimeResult = ScheduleTime.of(command.scheduleTime);
        if (scheduleTimeResult.isFailure()) {
            return Result.failure(scheduleTimeResult.errors());
        }
        
        Result<EmailRecipient> primaryRecipientResult = EmailRecipient.of(command.primaryRecipient);
        if (primaryRecipientResult.isFailure()) {
            return Result.failure(primaryRecipientResult.errors());
        }
        
        Maybe<EmailRecipient> ccRecipient = EmailRecipient.ofOptional(command.ccRecipient);
        
        // Get existing configuration or create new one
        ReportConfiguration config = repository.findByBankId(command.bankId)
                .map(existing -> existing.update(
                        command.bankId,
                        templateResult.getValueOrThrow(),
                        languageResult.getValueOrThrow(),
                        outputFormatResult.getValueOrThrow(),
                        frequencyResult.getValueOrThrow(),
                        deadlineResult.getValueOrThrow(),
                        command.autoGenerationEnabled,
                        scheduleDayResult.getValueOrThrow(),
                        scheduleTimeResult.getValueOrThrow(),
                        primaryRecipientResult.getValueOrThrow(),
                        ccRecipient,
                        command.autoSendEnabled,
                        command.swiftAutoSubmitEnabled,
                        command.modifiedBy
                ))
                .orElseGet(() -> ReportConfiguration.builder()
                        .bankId(command.bankId)
                        .template(templateResult.getValueOrThrow())
                        .language(languageResult.getValueOrThrow())
                        .outputFormat(outputFormatResult.getValueOrThrow())
                        .frequency(frequencyResult.getValueOrThrow())
                        .submissionDeadline(deadlineResult.getValueOrThrow())
                        .autoGenerationEnabled(command.autoGenerationEnabled)
                        .scheduleDay(scheduleDayResult.getValueOrThrow())
                        .scheduleTime(scheduleTimeResult.getValueOrThrow())
                        .primaryRecipient(primaryRecipientResult.getValueOrThrow())
                        .ccRecipient(ccRecipient)
                        .autoSendEnabled(command.autoSendEnabled)
                        .swiftAutoSubmitEnabled(command.swiftAutoSubmitEnabled)
                        .lastModified(java.time.Instant.now())
                        .lastModifiedBy(command.modifiedBy)
                        .build()
                );
        
        ReportConfiguration saved = repository.save(config);
        
        return Result.success(saved);
    }
}
