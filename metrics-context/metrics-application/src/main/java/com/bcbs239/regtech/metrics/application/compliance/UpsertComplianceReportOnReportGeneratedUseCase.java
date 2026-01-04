package com.bcbs239.regtech.metrics.application.compliance;

import com.bcbs239.regtech.core.domain.events.integration.ComplianceReportGeneratedInboundEvent;
import com.bcbs239.regtech.metrics.application.compliance.port.ComplianceReportRepository;
import com.bcbs239.regtech.metrics.application.signal.ApplicationSignalPublisher;
import com.bcbs239.regtech.metrics.application.signal.ComplianceReportUpsertedSignal;
import com.bcbs239.regtech.metrics.domain.BankId;
import com.bcbs239.regtech.metrics.domain.ComplianceReport;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class UpsertComplianceReportOnReportGeneratedUseCase {

    private final ComplianceReportRepository complianceReportRepository;
    private final ApplicationSignalPublisher signalPublisher;

    public UpsertComplianceReportOnReportGeneratedUseCase(
            ComplianceReportRepository complianceReportRepository,
            ApplicationSignalPublisher signalPublisher
    ) {
        this.complianceReportRepository = complianceReportRepository;
        this.signalPublisher = signalPublisher;
    }

    public void process(ComplianceReportGeneratedInboundEvent event) {
        if (event == null) {
            return;
        }

        LocalDate reportingDate = LocalDate.parse(event.getReportingDate());

        String status = (event.getHtmlS3Uri() != null && event.getXbrlS3Uri() != null)
                ? "COMPLETED"
                : "PARTIAL";

        ComplianceReport report = new ComplianceReport(
                event.getReportId(),
                event.getBatchId(),
                BankId.of(event.getBankId()),
                reportingDate,
                event.getReportType(),
                status,
                event.getGeneratedAt(),
                event.getHtmlS3Uri(),
                event.getXbrlS3Uri(),
                event.getHtmlPresignedUrl(),
                event.getXbrlPresignedUrl(),
                event.getHtmlFileSize(),
                event.getXbrlFileSize(),
                event.getOverallQualityScore(),
                event.getComplianceStatus(),
                event.getGenerationDurationMillis()
        );

        complianceReportRepository.save(report);
        signalPublisher.publish(new ComplianceReportUpsertedSignal(
                event.getReportId(),
                event.getBankId(),
                status,
                event.getReportType(),
                event.getReportingDate()
        ));
    }
}
