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
@Order(3) // run after other seeders
public class QualityThresholdDevSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(QualityThresholdDevSeeder.class);

    private final JdbcTemplate jdbcTemplate;

    public QualityThresholdDevSeeder(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            // Check if any threshold already exists for DEFAULT_BANK
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM dataquality.quality_thresholds WHERE bank_id = ?",
                    Integer.class,
                    "DEFAULT_BANK"
            );

            if (count == null || count == 0) {
                jdbcTemplate.update(
                        "INSERT INTO dataquality.quality_thresholds (" +
                                "bank_id, completeness_min_percent, accuracy_max_error_percent, " +
                                "timeliness_days, consistency_percent, is_active" +
                                ") VALUES (?, ?, ?, ?, ?, ?)",
                        "DEFAULT_BANK", 95.0, 5.0, 7, 98.0, true
                );
                log.info("Seeded default quality thresholds for DEFAULT_BANK");
            } else {
                log.info("Quality thresholds for DEFAULT_BANK already present; skipping seeding");
            }
        } catch (Exception e) {
            log.warn("Skipping quality thresholds dev seeding (table/schema may not exist yet): {}", e.getMessage());
        }
    }
}
