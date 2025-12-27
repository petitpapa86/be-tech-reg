package com.bcbs239.regtech.dataquality.application.reporting;

import com.bcbs239.regtech.dataquality.domain.model.valueobject.LargeExposure;
import com.bcbs239.regtech.dataquality.domain.report.QualityReport;

import java.util.List;

/**
 * Application-facing calculator for Large Exposures.
 *
 * <p>Implementation belongs to application/infrastructure depending on data sources.
 * This abstraction keeps the domain free of orchestration concerns.</p>
 */
public interface LargeExposureCalculator {

    List<LargeExposure> calculate(QualityReport report);
}
