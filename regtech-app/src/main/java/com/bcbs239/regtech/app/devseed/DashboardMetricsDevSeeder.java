package com.bcbs239.regtech.app.devseed;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Profile({"dev", "development"})
@Order(3) // run after DatabaseMigrationConfig runner (@Order(1)) and MetricsFileDevSeeder (@Order(2))
public class DashboardMetricsDevSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DashboardMetricsDevSeeder.class);

    private final JdbcTemplate jdbcTemplate;

    public DashboardMetricsDevSeeder(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            int affected = jdbcTemplate.update(
                    "INSERT INTO metrics.dashboard_metrics " +
                    "(bank_id, period_start, overall_score, data_quality_score, bcbs_rules_score, completeness_score, " +
                    " total_files_processed, total_violations, total_reports_generated, " +
                    " total_exposures, valid_exposures, total_errors) " +
                    "VALUES (?, date_trunc('month', CURRENT_DATE)::date, 89.0, 94.0, 87.0, 96.0, 34, 87, 156, 2000, 1950, 50) " +
                    "ON CONFLICT (bank_id, period_start) DO UPDATE SET " +
                            "overall_score = EXCLUDED.overall_score, " +
                            "data_quality_score = EXCLUDED.data_quality_score, " +
                            "bcbs_rules_score = EXCLUDED.bcbs_rules_score, " +
                            "completeness_score = EXCLUDED.completeness_score, " +
                            "total_files_processed = EXCLUDED.total_files_processed, " +
                            "total_violations = EXCLUDED.total_violations, " +
                            "total_reports_generated = EXCLUDED.total_reports_generated, " +
                            "total_exposures = EXCLUDED.total_exposures, " +
                            "valid_exposures = EXCLUDED.valid_exposures, " +
                            "total_errors = EXCLUDED.total_errors"
                ,
                "BANK-XYZ"
            );

            if (affected > 0) {
                log.info("Seeded metrics.dashboard_metrics dev row (bank/month)");
            } else {
                log.info("metrics.dashboard_metrics dev seed already present; no rows inserted");
            }
        } catch (Exception e) {
            // Keep startup resilient: table may not exist yet in some local setups.
            log.warn("Skipping metrics.dashboard_metrics dev seeding (table/schema may not exist yet): {}", e.getMessage());
        }
    }
}
