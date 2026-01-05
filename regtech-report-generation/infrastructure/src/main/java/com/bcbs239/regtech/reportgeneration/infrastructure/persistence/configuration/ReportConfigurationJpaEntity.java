package com.bcbs239.regtech.reportgeneration.infrastructure.persistence.configuration;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.time.LocalTime;

/**
 * JPA Entity for report_configuration table
 * Separate from domain model
 */
@Entity
@Table(name = "report_configuration")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportConfigurationJpaEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "bank_id", nullable = false)
    private Long bankId;
    
    @Column(name = "template", nullable = false, length = 50)
    private String template;
    
    @Column(name = "language", nullable = false, length = 50)
    private String language;
    
    @Column(name = "output_format", nullable = false, length = 50)
    private String outputFormat;
    
    @Column(name = "frequency", nullable = false, length = 50)
    private String frequency;
    
    @Column(name = "submission_deadline", nullable = false)
    private Integer submissionDeadline;
    
    @Column(name = "auto_generation_enabled", nullable = false)
    private Boolean autoGenerationEnabled;
    
    @Column(name = "schedule_day", nullable = false, length = 50)
    private String scheduleDay;
    
    @Column(name = "schedule_time", nullable = false)
    private LocalTime scheduleTime;
    
    @Column(name = "primary_recipient", nullable = false, length = 255)
    private String primaryRecipient;
    
    @Column(name = "cc_recipient", length = 255)
    private String ccRecipient;
    
    @Column(name = "auto_send_enabled", nullable = false)
    private Boolean autoSendEnabled;
    
    @Column(name = "swift_auto_submit_enabled", nullable = false)
    private Boolean swiftAutoSubmitEnabled;
    
    @Column(name = "last_modified", nullable = false)
    private Instant lastModified;
    
    @Column(name = "last_modified_by", nullable = false, length = 100)
    private String lastModifiedBy;
}
