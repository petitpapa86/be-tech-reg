package com.bcbs239.regtech.reportgeneration.application.configuration;

import com.bcbs239.regtech.reportgeneration.domain.configuration.ReportConfiguration;
import com.bcbs239.regtech.reportgeneration.domain.configuration.ReportConfigurationRepository;
import com.bcbs239.regtech.reportgeneration.domain.configuration.valueobject.*;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.core.domain.shared.Maybe;
import com.bcbs239.regtech.core.domain.shared.FieldError;
import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger logger = LoggerFactory.getLogger(UpdateReportConfigurationHandler.class);
    
    private final ReportConfigurationRepository repository;
    
    @Value
    public static class UpdateCommand {
        Long bankId;
        String template;
        String language;
        String outputFormat;
        ReportFrequency frequency;
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
        logger.info("UpdateReportConfigurationHandler.handle start | bankId={} modifiedBy={}", command.bankId, command.modifiedBy);
        // Create value objects using smart constructors and collect validation errors
        List<FieldError> fieldErrors = new ArrayList<>();

        Result<ReportTemplate> templateResult = ReportTemplate.of(command.template);
        if (templateResult.isFailure()) {
            ErrorDetail e = templateResult.getError().orElseThrow();
            fieldErrors.add(new FieldError("template", e.getMessage(), e.getMessageKey()));
        }

        Result<ReportLanguage> languageResult = ReportLanguage.of(command.language);
        if (languageResult.isFailure()) {
            ErrorDetail e = languageResult.getError().orElseThrow();
            fieldErrors.add(new FieldError("language", e.getMessage(), e.getMessageKey()));
        }

        Result<OutputFormat> outputFormatResult = OutputFormat.of(command.outputFormat);
        if (outputFormatResult.isFailure()) {
            ErrorDetail e = outputFormatResult.getError().orElseThrow();
            fieldErrors.add(new FieldError("outputFormat", e.getMessage(), e.getMessageKey()));
        }

        // Frequency is provided as an enum in the command. Validate presence.
        if (command.frequency == null) {
            fieldErrors.add(new FieldError("frequency", "Report frequency cannot be null", "report.frequency.required"));
        }

        Result<SubmissionDeadline> deadlineResult = SubmissionDeadline.of(command.submissionDeadline);
        if (deadlineResult.isFailure()) {
            ErrorDetail e = deadlineResult.getError().orElseThrow();
            fieldErrors.add(new FieldError("submissionDeadline", e.getMessage(), e.getMessageKey()));
        }

        Result<ScheduleDay> scheduleDayResult = ScheduleDay.of(command.scheduleDay);
        if (scheduleDayResult.isFailure()) {
            ErrorDetail e = scheduleDayResult.getError().orElseThrow();
            fieldErrors.add(new FieldError("scheduleDay", e.getMessage(), e.getMessageKey()));
        }

        Result<ScheduleTime> scheduleTimeResult = ScheduleTime.of(command.scheduleTime);
        if (scheduleTimeResult.isFailure()) {
            ErrorDetail e = scheduleTimeResult.getError().orElseThrow();
            fieldErrors.add(new FieldError("scheduleTime", e.getMessage(), e.getMessageKey()));
        }


        Result<EmailRecipient> primaryRecipientResult;
        Maybe<EmailRecipient> ccRecipient;

        var corePrimaryResult = com.bcbs239.regtech.core.domain.shared.valueobjects.Email.create(command.primaryRecipient);
        if (corePrimaryResult.isFailure()) {
            ErrorDetail e = corePrimaryResult.getError().orElseThrow();
            fieldErrors.add(new FieldError("primaryRecipient", e.getMessage(), e.getMessageKey()));
            primaryRecipientResult = Result.failure(e);
        } else {
            String normalized = corePrimaryResult.getValueOrThrow().getValue();
            primaryRecipientResult = EmailRecipient.of(normalized);
            if (primaryRecipientResult.isFailure()) {
                ErrorDetail e = primaryRecipientResult.getError().orElseThrow();
                fieldErrors.add(new FieldError("primaryRecipient", e.getMessage(), e.getMessageKey()));
            }
        }

        if (command.ccRecipient == null || command.ccRecipient.isBlank()) {
            ccRecipient = Maybe.none();
        } else {
            var coreCc = com.bcbs239.regtech.core.domain.shared.valueobjects.Email.create(command.ccRecipient);
            if (coreCc.isFailure()) {
                ccRecipient = Maybe.none();
            } else {
                var rr = EmailRecipient.of(coreCc.getValueOrThrow().getValue());
                ccRecipient = rr.isSuccess() ? Maybe.some(rr.getValueOrThrow()) : Maybe.none();
            }
        }

        // If there are validation errors, return them all together
        if (!fieldErrors.isEmpty()) {
            logger.warn("UpdateReportConfiguration validation failed | bankId={} errors={}", command.bankId, fieldErrors);
            return Result.failure(ErrorDetail.validationError(fieldErrors));
        }
        
        ReportConfiguration config = repository.findByBankId(command.bankId)
                .map(existing -> existing.update(
                        command.bankId,
                        templateResult.getValueOrThrow(),
                        languageResult.getValueOrThrow(),
                        outputFormatResult.getValueOrThrow(),
                        command.frequency,
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
                        .frequency(command.frequency)
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
        logger.info("UpdateReportConfigurationHandler.handle end | bankId={}", command.bankId);
        return Result.success(saved);
    }
}
