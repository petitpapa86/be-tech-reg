package com.bcbs239.regtech.dataquality.application.filemanagement.mapper;

import com.bcbs239.regtech.dataquality.domain.report.QualityReport;
import com.bcbs239.regtech.dataquality.application.filemanagement.dto.FileResponse;
import org.springframework.stereotype.Component;

@Component
public class FileResponseMapper {

    public FileResponse toDto(QualityReport report) {
        if (report == null) {
            return null;
        }

        return new FileResponse(
            report.getReportId() != null ? report.getReportId().value() : null,
            report.getFileMetadata() != null ? report.getFileMetadata().filename() : "unknown",
            report.getCreatedAt(),
            report.getFileMetadata() != null ? report.getFileMetadata().size() : 0L,
            report.getStatus() != null ? report.getStatus().name() : "UNKNOWN",
            report.getFileMetadata() != null ? report.getFileMetadata().format() : "unknown",
            report.getBankId() != null ? report.getBankId().value() : null,
            report.getBatchId() != null ? report.getBatchId().value() : null
        );
    }
}
