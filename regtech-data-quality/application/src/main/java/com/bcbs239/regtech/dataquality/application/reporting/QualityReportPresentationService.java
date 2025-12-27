package com.bcbs239.regtech.dataquality.application.reporting;

import com.bcbs239.regtech.dataquality.domain.model.presentation.QualityReportPresentation;
import com.bcbs239.regtech.dataquality.domain.model.valueobject.LargeExposure;
import com.bcbs239.regtech.dataquality.domain.report.IQualityReportRepository;
import com.bcbs239.regtech.dataquality.domain.report.QualityReport;
import com.bcbs239.regtech.dataquality.domain.quality.QualityDimension;
import com.bcbs239.regtech.dataquality.domain.report.QualityStatus;
import com.bcbs239.regtech.dataquality.domain.shared.BankId;
import com.bcbs239.regtech.dataquality.domain.validation.ValidationError;
import com.bcbs239.regtech.dataquality.domain.validation.ValidationSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    /**
     * Returns the frontend presentation for the most recent report for a bank in a given status.
     *
     * <p>Used by the API endpoint that doesn't expose batchId directly.</p>
     */
    public QualityReportPresentation getLatestFrontendPresentation(BankId bankId, QualityStatus status) {
        if (bankId == null) {
            throw new IllegalArgumentException("bankId is required");
        }
        QualityStatus effectiveStatus = status != null ? status : QualityStatus.COMPLETED;

        QualityReport report = repository.findMostRecentByBankIdAndStatus(bankId, effectiveStatus)
            .orElseThrow(() -> new IllegalArgumentException(
                "QualityReport not found for bankId=" + bankId.value() + " with status=" + effectiveStatus
            ));

        List<LargeExposure> largeExposures = calculator.calculate(report);
        ValidationSummary summaryOverride = buildSummaryOverrideFromStoredDetails(report);
        return report.toFrontendPresentation(largeExposures, summaryOverride);
    }

    /**
     * Returns the frontend presentation for the most recent COMPLETED report for a bank.
     */
    public QualityReportPresentation getLatestFrontendPresentation(BankId bankId) {
        return getLatestFrontendPresentation(bankId, QualityStatus.COMPLETED);
    }

    private ValidationSummary buildSummaryOverrideFromStoredDetails(QualityReport report) {
        if (report.getDetailsReference() == null || report.getDetailsReference().uri() == null) {
            return null;
        }

        StoredValidationResults stored = storedResultsReader.load(report.getDetailsReference().uri());
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
