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
@Profile("development")
@Order(2) // run after DatabaseMigrationConfig runner (@Order(1))
public class MetricsFileDevSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MetricsFileDevSeeder.class);

    private final JdbcTemplate jdbcTemplate;

    public MetricsFileDevSeeder(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            // Idempotent dev seeding: safe to run on every startup.
            int inserted1 = jdbcTemplate.update(
                    "INSERT INTO metrics.metrics_file (filename, date, score, status, batch_id, bank_id) " +
                            "VALUES (?, ?, ?, ?, ?, ?) " +
                            "ON CONFLICT (filename) DO NOTHING",
                    "esposizioni_settembre.xlsx",
                    "01 Gen 2026",
                    87.2,
                    "VIOLATIONS",
                    "batch-202509",
                    "BANK-XYZ"
            );

            int inserted2 = jdbcTemplate.update(
                    "INSERT INTO metrics.metrics_file (filename, date, score, status, batch_id, bank_id) " +
                            "VALUES (?, ?, ?, ?, ?, ?) " +
                            "ON CONFLICT (filename) DO NOTHING",
                    "grandi_esposizioni_agosto.xlsx",
                    "01 Gen 2026",
                    94.1,
                    "COMPLIANT",
                    "batch-202508",
                    "BANK-XYZ"
            );

            if (inserted1 + inserted2 > 0) {
                log.info("Seeded metrics.metrics_file with {} dev rows", inserted1 + inserted2);
            }
        } catch (Exception e) {
            // Keep startup resilient: table may not exist yet in some local setups.
            log.debug("Skipping metrics.metrics_file dev seeding: {}", e.getMessage());
        }
    }
}
