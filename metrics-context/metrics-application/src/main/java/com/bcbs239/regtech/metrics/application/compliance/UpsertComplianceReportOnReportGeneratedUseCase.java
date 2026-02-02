package com.bcbs239.regtech.metrics.application.compliance;

import com.bcbs239.regtech.core.domain.events.integration.ComplianceReportGeneratedInboundEvent;
import com.bcbs239.regtech.metrics.application.compliance.port.ComplianceReportRepository;
import com.bcbs239.regtech.metrics.domain.BankId;
import com.bcbs239.regtech.metrics.domain.ComplianceReport;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class UpsertComplianceReportOnReportGeneratedUseCase {

    private final ComplianceReportRepository complianceReportRepository;

    public UpsertComplianceReportOnReportGeneratedUseCase(
            ComplianceReportRepository complianceReportRepository
    ) {
        this.complianceReportRepository = complianceReportRepository;
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
                event.getGeneratedAt(), // updatedAt initially same as generatedAt
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
    }
}
