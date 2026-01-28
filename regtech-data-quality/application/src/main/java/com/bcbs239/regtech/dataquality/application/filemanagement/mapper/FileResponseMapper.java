package com.bcbs239.regtech.dataquality.application.filemanagement.mapper;

import com.bcbs239.regtech.dataquality.application.filemanagement.dto.FileResponse;
import com.bcbs239.regtech.dataquality.domain.quality.QualityScores;
import com.bcbs239.regtech.dataquality.domain.report.QualityReport;
import com.bcbs239.regtech.dataquality.domain.report.QualityStatus;
import com.bcbs239.regtech.dataquality.domain.validation.ViolationSummary;
import com.bcbs239.regtech.core.application.recommendations.ports.RecommendationRuleLoader;
import com.bcbs239.regtech.core.domain.quality.QualityThresholds;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Component
public class FileResponseMapper {

    private final RecommendationRuleLoader ruleLoader;

    public FileResponseMapper(RecommendationRuleLoader ruleLoader) {
        this.ruleLoader = ruleLoader;
    }

    private static final DateTimeFormatter DATE_FORMATTER = 
        DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ITALIAN);
    
    private static final DateTimeFormatter TIME_FORMATTER = 
        DateTimeFormatter.ofPattern("HH:mm");
    
    private static final ZoneId ROME_ZONE = ZoneId.of("Europe/Rome");

    public FileResponse toDto(QualityReport report) {
        if (report == null) {
            return null;
        }

        String uploadDateFormatted = null;
        String uploadTimeFormatted = null;
        if (report.getCreatedAt() != null) {
            uploadDateFormatted = DATE_FORMATTER.format(report.getCreatedAt().atZone(ROME_ZONE));
            uploadTimeFormatted = TIME_FORMATTER.format(report.getCreatedAt().atZone(ROME_ZONE));
        }

        Double qualityScore = getQualityScore(report);
        Double complianceScore = report.getComplianceScore();
        ViolationSummary violations = report.getViolationSummary();

        // Load thresholds for UI logic
        QualityThresholds thresholds = ruleLoader.loadThresholds();
        Integer totalViolations = violations != null ? violations.getTotal() : 0;

        return new FileResponse(
            report.getReportId() != null ? report.getReportId().value() : null,
            report.getFileMetadata() != null ? report.getFileMetadata().filename() : "unknown",
            report.getCreatedAt(),
            uploadDateFormatted,
            uploadTimeFormatted,
            report.getFileMetadata() != null ? report.getFileMetadata().size() : 0L,
            report.getStatus() != null ? report.getStatus().name() : "UNKNOWN",
            report.getFileMetadata() != null ? report.getFileMetadata().format() : "unknown",
            report.getBankId() != null ? report.getBankId().value() : null,
            report.getBatchId() != null ? report.getBatchId().value() : null,
            qualityScore,
            complianceScore,
            violations,
            // UI Metadata
            thresholds.getQualityScoreColor(qualityScore),
            thresholds.getQualityScoreBadge(qualityScore),
            thresholds.getComplianceScoreColor(complianceScore),
            thresholds.getComplianceBadge(complianceScore),
            report.getStatus() != null ? report.getStatus().getColor() : "blue",
            report.getStatus() != null ? report.getStatus().getIcon() : "⟳",
            thresholds.getViolationsColor(totalViolations),
            thresholds.getViolationsSeverity(totalViolations)
        );
    }

    private Double getQualityScore(QualityReport report) {
        if (report.getScores() != null) {
            return report.getScores().overallScore();
        }
        return null;
    }
}
