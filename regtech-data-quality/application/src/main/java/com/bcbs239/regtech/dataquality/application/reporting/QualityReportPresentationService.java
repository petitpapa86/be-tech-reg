package com.bcbs239.regtech.dataquality.application.reporting;

import com.bcbs239.regtech.dataquality.domain.model.presentation.QualityReportPresentation;
import com.bcbs239.regtech.dataquality.domain.model.presentation.QualityReportPresentation.ActionPresentation;
import com.bcbs239.regtech.dataquality.domain.model.valueobject.LargeExposure;
import com.bcbs239.regtech.dataquality.domain.report.IQualityReportRepository;
import com.bcbs239.regtech.dataquality.domain.report.QualityReport;
import com.bcbs239.regtech.dataquality.domain.quality.QualityDimension;
import com.bcbs239.regtech.dataquality.domain.report.QualityStatus;
import com.bcbs239.regtech.core.domain.shared.valueobjects.QualityReportId;
import com.bcbs239.regtech.core.application.recommendations.ports.RecommendationRuleLoader;
import com.bcbs239.regtech.core.domain.quality.QualityThresholds;
import com.bcbs239.regtech.dataquality.domain.shared.BankId;
import com.bcbs239.regtech.dataquality.domain.validation.ValidationError;
import com.bcbs239.regtech.dataquality.domain.validation.ValidationSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Application service for producing the frontend presentation model.
 *
 * <p>NO business logic here: orchestration only.</p>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QualityReportPresentationService {

    private final IQualityReportRepository repository;
    private final LargeExposureCalculator calculator;
    private final StoredValidationResultsReader storedResultsReader;
    private final RecommendationRuleLoader ruleLoader;

    /**
     * Returns the frontend presentation for a specific report identified by reportId (QualityReportId) for a bank.
     *
     * <p>Used by the API endpoint to view specific report details.</p>
     */
    public QualityReportPresentation getLatestFrontendPresentation(BankId bankId, String reportIdStr) {
        
        if (reportIdStr == null || reportIdStr.isBlank()) {
            throw new IllegalArgumentException("reportId is required");
        }

        QualityReportId reportId = QualityReportId.of(reportIdStr);

        QualityReport report = repository.findByReportId(reportId)
            .orElseThrow(() -> new IllegalArgumentException(
                "QualityReport not found for reportId=" + reportIdStr
            ));

        if (!report.getBankId().equals(bankId)) {
            // Use IllegalArgumentException to avoid leaking existence of reports for other banks
            throw new IllegalArgumentException("QualityReport not found for reportId=" + reportIdStr);
        }

        StoredValidationResults stored = null;
        if (report.getDetailsReference() != null && report.getDetailsReference().uri() != null) {
            stored = storedResultsReader.load(report.getDetailsReference().uri());
        }

        List<LargeExposure> largeExposures = calculator.calculate(report);
        ValidationSummary summaryOverride = buildSummaryOverrideFromStoredDetails(report, stored);
        List<ActionPresentation> externalActions = buildActionsFromStoredResults(stored);
        QualityThresholds thresholds = ruleLoader.loadThresholds();

        return report.toFrontendPresentation(largeExposures, summaryOverride, externalActions, thresholds);
    }

    private ValidationSummary buildSummaryOverrideFromStoredDetails(QualityReport report, StoredValidationResults stored) {
        if (stored == null) {
            return null;
        }

        int totalExposures = stored.totalExposures() > 0
            ? stored.totalExposures()
            : (report.getValidationSummary() != null ? report.getValidationSummary().totalExposures() : 0);

        int validExposures = stored.validExposures() > 0
            ? stored.validExposures()
            : (int) stored.exposureResults().stream().filter(DetailedExposureResult::isValid).count();

        int invalidExposures = totalExposures > 0 ? Math.max(0, totalExposures - validExposures) : 0;

        Map<String, Integer> errorsByCode = new HashMap<>();
        Map<QualityDimension, Integer> errorsByDimension = new HashMap<>();
        Map<ValidationError.ErrorSeverity, Integer> errorsBySeverity = new HashMap<>();

        // Exposure-level errors
        stored.exposureResults().forEach(exposure -> {
            if (exposure.errors() == null) {
                return;
            }
            exposure.errors().forEach(err -> accumulateError(err, errorsByCode, errorsByDimension, errorsBySeverity));
        });

        // Batch-level errors
        stored.batchErrors().forEach(err -> accumulateError(err, errorsByCode, errorsByDimension, errorsBySeverity));

        int totalErrors = stored.totalErrors() > 0
            ? stored.totalErrors()
            : errorsByCode.values().stream().mapToInt(Integer::intValue).sum();

        double overallValidationRate = totalExposures > 0
            ? ((double) validExposures / (double) totalExposures)
            : 0.0;

        return new ValidationSummary(
            totalExposures,
            validExposures,
            invalidExposures,
            totalErrors,
            errorsByDimension,
            errorsBySeverity,
            errorsByCode,
            overallValidationRate
        );
    }

    private List<ActionPresentation> buildActionsFromStoredResults(StoredValidationResults stored) {
        if (stored == null || stored.recommendations() == null) {
            return List.of();
        }

        return stored.recommendations().stream()
            .map(this::mapRecommendation)
            .collect(Collectors.toList());
    }

    private ActionPresentation mapRecommendation(StoredValidationResults.StoredRecommendation rec) {
        String title = rec.message() != null && !rec.message().isBlank()
            ? rec.message()
            : (rec.ruleId() != null ? rec.ruleId() : "Raccomandazione");

        String description = rec.actionItems() != null
            ? String.join("\n", rec.actionItems())
            : "";

        String priority = mapSeverityToPriority(rec.severity());
        String color = rec.color() != null ? rec.color() : "blue";

        return new ActionPresentation(
            title,
            description,
            "", // deadline not available in stored results
            priority,
            color
        );
    }

    private String mapSeverityToPriority(String severity) {
        if (severity == null) return "Media";
        switch (severity.trim().toUpperCase()) {
            case "CRITICAL": return "Critica";
            case "HIGH": return "Alta";
            case "MEDIUM": return "Media";
            case "LOW": return "Bassa";
            case "SUCCESS": return "Info";
            default: return "Media";
        }
    }

    private void accumulateError(
        DetailedExposureResult.DetailedError err,
        Map<String, Integer> errorsByCode,
        Map<QualityDimension, Integer> errorsByDimension,
        Map<ValidationError.ErrorSeverity, Integer> errorsBySeverity
    ) {
        if (err == null) {
            return;
        }

        if (err.ruleCode() != null && !err.ruleCode().isBlank()) {
            errorsByCode.merge(err.ruleCode(), 1, Integer::sum);
        }

        if (err.dimension() != null && !err.dimension().isBlank()) {
            try {
                QualityDimension dim = QualityDimension.valueOf(err.dimension().trim().toUpperCase());
                errorsByDimension.merge(dim, 1, Integer::sum);
            } catch (IllegalArgumentException ignored) {
                // ignore unknown dimension strings
            }
        }

        if (err.severity() != null && !err.severity().isBlank()) {
            try {
                ValidationError.ErrorSeverity sev = ValidationError.ErrorSeverity.valueOf(err.severity().trim().toUpperCase());
                errorsBySeverity.merge(sev, 1, Integer::sum);
            } catch (IllegalArgumentException ignored) {
                // ignore unknown severity strings
            }
        }
    }
}
