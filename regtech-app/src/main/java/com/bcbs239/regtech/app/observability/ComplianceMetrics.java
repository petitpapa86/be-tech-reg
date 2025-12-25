package com.bcbs239.regtech.app.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Business metrics facade for Prometheus/Grafana/Alertmanager.
 *
 * Metric names use Micrometer dotted notation which Prometheus will expose as underscores.
 * Example: regtech.business.transactions.total -> regtech_business_transactions_total
 */
@Component
public class ComplianceMetrics {

    private final MeterRegistry registry;

    // Keep a stable Gauge instance (avoid registering a new gauge per call)
    private final AtomicLong dataQualityScoreMilli = new AtomicLong(0);

    public ComplianceMetrics(MeterRegistry registry) {
        this.registry = registry;

        Gauge.builder("regtech.data.quality.score", dataQualityScoreMilli, v -> v.get() / 1000.0)
                .description("Latest data quality score (0.0 to 1.0)")
                .register(registry);
    }

    /** Business Transactions (maps to regtech_business_transactions_total / _failed_total). */
    public void recordBusinessTransaction(String transactionType, boolean success) {
        String metricName = success
                ? "regtech.business.transactions.total"
                : "regtech.business.transactions.failed.total";

        Counter.builder(metricName)
                .description("Business transactions")
                .tag("type", transactionType)
                .register(registry)
                .increment();
    }

    /** Data Quality Score (maps to regtech_data_quality_score). */
    public void recordDataQualityScore(double score) {
        // store as milli-units to avoid floating point issues in Atomic*
        long milli = Math.round(score * 1000.0);
        dataQualityScoreMilli.set(Math.max(0, Math.min(1000, milli)));
    }

    /** Risk Assessments (maps to regtech_risk_assessments_total). */
    public void recordRiskAssessment(String riskType) {
        Counter.builder("regtech.risk.assessments.total")
                .description("Risk assessments")
                .tag("type", riskType)
                .register(registry)
                .increment();
    }

    /** Risk Detected (maps to regtech_risk_detected_total). */
    public void recordRiskDetected(String category) {
        Counter.builder("regtech.risk.detected.total")
                .description("Risks detected")
                .tag("category", category)
                .register(registry)
                .increment();
    }

    /** Reports Generated (maps to regtech_reports_generated_total). */
    public void recordReportGenerated(String reportType) {
        Counter.builder("regtech.reports.generated.total")
                .description("Reports generated")
                .tag("type", reportType)
                .register(registry)
                .increment();
    }

    /** Compliance Checks (maps to regtech_compliance_checks_total). */
    public void recordComplianceCheck(String result) {
        Counter.builder("regtech.compliance.checks.total")
                .description("Compliance checks")
                .tag("result", result)
                .register(registry)
                .increment();
    }

    /** Alerts (maps to regtech_alerts_total). */
    public void recordAlert(String severity, String type) {
        Counter.builder("regtech.alerts.total")
                .description("Business alerts")
                .tag("severity", severity)
                .tag("type", type)
                .register(registry)
                .increment();
    }

    /** Business Process Duration (maps to regtech_business_process_duration_seconds_bucket). */
    public void recordBusinessProcessDuration(Timer.Sample sample, String processType) {
        sample.stop(Timer.builder("regtech.business.process.duration.seconds")
                .description("Business process duration")
                .tag("process_type", processType)
                .publishPercentiles(0.5, 0.95, 0.99)
                .publishPercentileHistogram()
                .register(registry));
    }
}
