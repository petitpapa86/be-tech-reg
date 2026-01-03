package com.bcbs239.regtech.app.devseed;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@Profile({"dev", "development"})
@Order(4) // run after DatabaseMigrationConfig (@Order(1)), MetricsFileDevSeeder (@Order(2)), DashboardMetricsDevSeeder (@Order(3))
public class ComplianceReportsDevSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ComplianceReportsDevSeeder.class);

    private final JdbcTemplate jdbcTemplate;

    public ComplianceReportsDevSeeder(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            LocalDate reportingDate = LocalDate.now().withDayOfMonth(1);

            // Keep it deterministic + idempotent for dev.
            seedOne(
                    "DEV-REPORT-001",
                    "batch-" + reportingDate.getYear() + String.format("%02d", reportingDate.getMonthValue()),
                    "BANK-XYZ",
                    "BCBS239",
                    reportingDate,
                    "COMPLETED",
                    "s3://dev-bucket/reports/BCBS239_" + reportingDate + "_DEV-REPORT-001.html",
                    "s3://dev-bucket/reports/BCBS239_" + reportingDate + "_DEV-REPORT-001.xbrl",
                    1_245_000L,
                    842_000L,
                    94.10,
                    "COMPLIANT",
                    12_340L
            );

            seedOne(
                    "DEV-REPORT-002",
                    "batch-" + reportingDate.getYear() + String.format("%02d", reportingDate.getMonthValue()),
                    "BANK-XYZ",
                    "LARGE_EXPOSURES",
                    reportingDate,
                    "COMPLETED",
                    "s3://dev-bucket/reports/LARGE_EXPOSURES_" + reportingDate + "_DEV-REPORT-002.html",
                    null,
                    980_000L,
                    null,
                    87.20,
                    "VIOLATIONS",
                    9_210L
            );

            log.info("Seeded metrics.compliance_reports dev rows");
        } catch (Exception e) {
            // Keep startup resilient: table may not exist yet in some local setups.
            log.warn("Skipping metrics.compliance_reports dev seeding (table/schema may not exist yet): {}", e.getMessage());
        }
    }

    private void seedOne(
            String reportId,
            String batchId,
            String bankId,
            String reportType,
            LocalDate reportingDate,
            String status,
            String htmlS3Uri,
            String xbrlS3Uri,
            Long htmlFileSize,
            Long xbrlFileSize,
            Double overallQualityScore,
            String complianceStatus,
            Long generationDurationMillis
    ) {
        // Use NOW() timestamps (dev only).
        jdbcTemplate.update(
                "INSERT INTO metrics.compliance_reports (" +
                        "report_id, batch_id, bank_id, report_type, reporting_date, status, generated_at, " +
                        "html_s3_uri, xbrl_s3_uri, html_file_size, xbrl_file_size, overall_quality_score, compliance_status, generation_duration_millis" +
                        ") VALUES (?, ?, ?, ?, ?, ?, NOW(), ?, ?, ?, ?, ?, ?, ?) " +
                        "ON CONFLICT (report_id) DO UPDATE SET " +
                        "batch_id = EXCLUDED.batch_id, " +
                        "bank_id = EXCLUDED.bank_id, " +
                        "report_type = EXCLUDED.report_type, " +
                        "reporting_date = EXCLUDED.reporting_date, " +
                        "status = EXCLUDED.status, " +
                        "generated_at = EXCLUDED.generated_at, " +
                        "html_s3_uri = EXCLUDED.html_s3_uri, " +
                        "xbrl_s3_uri = EXCLUDED.xbrl_s3_uri, " +
                        "html_file_size = EXCLUDED.html_file_size, " +
                        "xbrl_file_size = EXCLUDED.xbrl_file_size, " +
                        "overall_quality_score = EXCLUDED.overall_quality_score, " +
                        "compliance_status = EXCLUDED.compliance_status, " +
                        "generation_duration_millis = EXCLUDED.generation_duration_millis, " +
                        "updated_at = NOW()",
                reportId,
                batchId,
                bankId,
                reportType,
                reportingDate,
                status,
                htmlS3Uri,
                xbrlS3Uri,
                htmlFileSize,
                xbrlFileSize,
                overallQualityScore,
                complianceStatus,
                generationDurationMillis
        );
    }
}
