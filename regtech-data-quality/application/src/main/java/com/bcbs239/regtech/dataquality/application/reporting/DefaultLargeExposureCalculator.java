package com.bcbs239.regtech.dataquality.application.reporting;

import com.bcbs239.regtech.dataquality.domain.model.valueobject.LargeExposure;
import com.bcbs239.regtech.dataquality.domain.report.QualityReport;

import java.util.List;

/**
 * Default Large Exposure calculator.
 *
 * <p>Currently returns an empty list until a concrete data source is wired
 * (e.g., ingestion results, risk-calculation outputs, or a reporting read model).</p>
 */
public class DefaultLargeExposureCalculator implements LargeExposureCalculator {

    @Override
    public List<LargeExposure> calculate(QualityReport report) {
        return List.of();
    }
}
