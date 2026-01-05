package com.bcbs239.regtech.reportgeneration.infrastructure.persistence.configuration;

import com.bcbs239.regtech.reportgeneration.domain.configuration.*;
import com.bcbs239.regtech.reportgeneration.domain.configuration.valueobject.*;
import com.bcbs239.regtech.core.domain.shared.Maybe;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * Repository Adapter - maps JPA ↔ Domain
 */
@Repository
@RequiredArgsConstructor
public class ReportConfigurationRepositoryAdapter implements ReportConfigurationRepository {
    
    private final ReportConfigurationJpaRepository jpaRepository;
    
    @Override
    public Maybe<ReportConfiguration> findByBankId(Long bankId) {
        return jpaRepository.findByBankId(bankId)
                .map(this::toDomain)
                .map(Maybe::some)
                .orElse(Maybe.none());
    }
    
    @Override
    public ReportConfiguration save(ReportConfiguration configuration) {
        ReportConfigurationJpaEntity entity = jpaRepository.findByBankId(configuration.getBankId())
                .map(existing -> {
                    existing.setBankId(configuration.getBankId());
                    existing.setTemplate(configuration.getTemplate().name());
                    existing.setLanguage(configuration.getLanguage().name());
                    existing.setOutputFormat(configuration.getOutputFormat().name());
                    existing.setFrequency(configuration.getFrequency().name());
                    existing.setSubmissionDeadline(configuration.getSubmissionDeadline().getDays());
                    existing.setAutoGenerationEnabled(configuration.isAutoGenerationEnabled());
                    existing.setScheduleDay(configuration.getScheduleDay().name());
                    existing.setScheduleTime(configuration.getScheduleTime().getTime());
                    existing.setPrimaryRecipient(configuration.getPrimaryRecipient().getEmail());
                    existing.setCcRecipient(configuration.getCcRecipient().map(EmailRecipient::getEmail).orElse(null));
                    existing.setAutoSendEnabled(configuration.isAutoSendEnabled());
                    existing.setSwiftAutoSubmitEnabled(configuration.isSwiftAutoSubmitEnabled());
                    existing.setLastModified(configuration.getLastModified());
                    existing.setLastModifiedBy(configuration.getLastModifiedBy());
                    return existing;
                })
                .orElseGet(() -> ReportConfigurationJpaEntity.builder()
                        .bankId(configuration.getBankId())
                        .template(configuration.getTemplate().name())
                        .language(configuration.getLanguage().name())
                        .outputFormat(configuration.getOutputFormat().name())
                        .frequency(configuration.getFrequency().name())
                        .submissionDeadline(configuration.getSubmissionDeadline().getDays())
                        .autoGenerationEnabled(configuration.isAutoGenerationEnabled())
                        .scheduleDay(configuration.getScheduleDay().name())
                        .scheduleTime(configuration.getScheduleTime().getTime())
                        .primaryRecipient(configuration.getPrimaryRecipient().getEmail())
                        .ccRecipient(configuration.getCcRecipient().map(EmailRecipient::getEmail).orElse(null))
                        .autoSendEnabled(configuration.isAutoSendEnabled())
                        .swiftAutoSubmitEnabled(configuration.isSwiftAutoSubmitEnabled())
                        .lastModified(configuration.getLastModified())
                        .lastModifiedBy(configuration.getLastModifiedBy())
                        .build()
                );
        
        ReportConfigurationJpaEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }
    
    private ReportConfiguration toDomain(ReportConfigurationJpaEntity entity) {
        return ReportConfiguration.builder()
                .bankId(entity.getBankId())
                .template(ReportTemplate.valueOf(entity.getTemplate()))
                .language(ReportLanguage.valueOf(entity.getLanguage()))
                .outputFormat(OutputFormat.valueOf(entity.getOutputFormat()))
                .frequency(ReportFrequency.valueOf(entity.getFrequency()))
                .submissionDeadline(SubmissionDeadline.of(entity.getSubmissionDeadline()).getValueOrThrow())
                .autoGenerationEnabled(entity.getAutoGenerationEnabled())
                .scheduleDay(ScheduleDay.valueOf(entity.getScheduleDay()))
                .scheduleTime(ScheduleTime.of(entity.getScheduleTime().toString()).getValueOrThrow())
                .primaryRecipient(EmailRecipient.of(entity.getPrimaryRecipient()).getValueOrThrow())
                .ccRecipient(EmailRecipient.ofOptional(entity.getCcRecipient()))
                .autoSendEnabled(entity.getAutoSendEnabled())
                .swiftAutoSubmitEnabled(entity.getSwiftAutoSubmitEnabled())
                .lastModified(entity.getLastModified())
                .lastModifiedBy(entity.getLastModifiedBy())
                .build();
    }
}
