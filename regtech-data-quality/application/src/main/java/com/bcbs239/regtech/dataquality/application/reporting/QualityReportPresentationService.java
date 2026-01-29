package com.bcbs239.regtech.dataquality.application.reporting;

import com.bcbs239.regtech.dataquality.domain.model.presentation.QualityReportPresentation;
import com.bcbs239.regtech.dataquality.domain.model.presentation.QualityReportPresentation.ActionPresentation;
import com.bcbs239.regtech.dataquality.domain.model.presentation.QualityReportPresentation.DimensionDetailPresentation;
import com.bcbs239.regtech.dataquality.domain.model.reporting.StoredValidationResults;
import com.bcbs239.regtech.dataquality.domain.model.valueobject.LargeExposure;
import com.bcbs239.regtech.dataquality.domain.report.IQualityReportRepository;
import com.bcbs239.regtech.dataquality.domain.report.QualityReport;
import com.bcbs239.regtech.core.domain.shared.valueobjects.QualityReportId;
import com.bcbs239.regtech.core.application.recommendations.ports.RecommendationRuleLoader;
import com.bcbs239.regtech.core.domain.quality.QualityThresholds;
import com.bcbs239.regtech.dataquality.domain.shared.BankId;
import com.bcbs239.regtech.dataquality.domain.validation.ValidationSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
            throw new IllegalArgumentException("QualityReport not found for reportId=" + reportIdStr);
        }

        StoredValidationResults stored = storedResultsReader.load(report.getDetailsReference().uri())
                    .orElse(null);
        ValidationSummary summaryOverride = stored != null ? stored.calculateSummary(report.getValidationSummary()) : null;
        List<ActionPresentation> externalActions = stored != null ? stored.getActions() : List.of();

        List<DimensionDetailPresentation> dimensionDetails = stored != null
                ? stored.getDimensionDetails() 
                : List.of();

        return report.toFrontendPresentation(calculator.calculate(report), summaryOverride, externalActions, ruleLoader.loadThresholds(), dimensionDetails);
    }
}
