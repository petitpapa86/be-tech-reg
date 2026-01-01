package com.bcbs239.regtech.app.config;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("development")
public class FlywayAutoRepairConfig {

    private static final Logger log = LoggerFactory.getLogger(FlywayAutoRepairConfig.class);
//
//    @Bean
//    FlywayMigrationStrategy flywayMigrationStrategy() {
//        return (Flyway flyway) -> {
//            log.warn("Flyway auto-repair enabled for development profile. Running repair before migrate.");
//            flyway.repair();
//            flyway.migrate();
//        };
//    }
}
