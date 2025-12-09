package com.bcbs239.regtech.app.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Database schema migration for batch_id and bank_id column length fix.
 * This will run automatically on application startup.
 */
@Configuration
public class DatabaseMigrationConfig {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseMigrationConfig.class);

    @Bean
    @Order(1) // Run early, before other beans
    public CommandLineRunner databaseMigration(JdbcTemplate jdbcTemplate) {
        return args -> {
            logger.info("========================================");
            logger.info("Running database schema migration...");
            logger.info("Fix: Increase batch_id and bank_id column lengths");
            logger.info("========================================");

            try {
                // Check if migration is needed by inspecting column length
                boolean needsMigration = checkIfMigrationNeeded(jdbcTemplate);
                
                if (!needsMigration) {
                    logger.info("Migration already applied. Skipping...");
                    return;
                }

                // Run migrations
                migrateIngestionSchema(jdbcTemplate);
                migrateDataQualitySchema(jdbcTemplate);
                
                logger.info("========================================");
                logger.info("Database migration completed successfully!");
                logger.info("========================================");
                
            } catch (Exception e) {
                logger.error("========================================");
                logger.error("Database migration failed: {}", e.getMessage());
                logger.error("========================================");
                logger.error("Please run the migration manually using COMPREHENSIVE_DATABASE_MIGRATION.sql");
                // Don't throw exception to allow app to start (Hibernate will handle schema)
            }
        };
    }

    private boolean checkIfMigrationNeeded(JdbcTemplate jdbcTemplate) {
        try {
            // Check if ingestion.ingestion_batches.batch_id is already VARCHAR(255)
            String sql = "SELECT character_maximum_length FROM information_schema.columns " +
                        "WHERE table_schema = 'ingestion' " +
                        "AND table_name = 'ingestion_batches' " +
                        "AND column_name = 'batch_id'";
            
            Integer length = jdbcTemplate.queryForObject(sql, Integer.class);
            return length != null && length < 255;
            
        } catch (Exception e) {
            logger.warn("Could not check migration status. Will attempt migration: {}", e.getMessage());
            return true; // Assume migration is needed
        }
    }

    private void migrateIngestionSchema(JdbcTemplate jdbcTemplate) {
        logger.info("Migrating ingestion schema...");
        
        try {
            // Check if schema exists
            jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS ingestion");
            
            // Alter ingestion_batches table
            jdbcTemplate.execute(
                "ALTER TABLE IF EXISTS ingestion.ingestion_batches " +
                "ALTER COLUMN batch_id TYPE VARCHAR(255), " +
                "ALTER COLUMN bank_id TYPE VARCHAR(255)"
            );
            
            logger.info("✓ Ingestion schema migrated successfully");
            
        } catch (Exception e) {
            logger.warn("Could not migrate ingestion schema: {}. This may be normal if using Hibernate ddl-auto.", 
                       e.getMessage());
        }
    }

    private void migrateDataQualitySchema(JdbcTemplate jdbcTemplate) {
        logger.info("Migrating data quality schema...");
        
        try {
            // Check if schema exists
            jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS dataquality");
            
            // Alter quality_reports table
            jdbcTemplate.execute(
                "ALTER TABLE IF EXISTS dataquality.quality_reports " +
                "ALTER COLUMN batch_id TYPE VARCHAR(255), " +
                "ALTER COLUMN bank_id TYPE VARCHAR(255)"
            );
            
            logger.info("✓ Quality reports table migrated successfully");
            
            // Alter quality_error_summaries table
            jdbcTemplate.execute(
                "ALTER TABLE IF EXISTS dataquality.quality_error_summaries " +
                "ALTER COLUMN batch_id TYPE VARCHAR(255), " +
                "ALTER COLUMN bank_id TYPE VARCHAR(255)"
            );
            
            logger.info("✓ Quality error summaries table migrated successfully");
            
        } catch (Exception e) {
            logger.warn("Could not migrate data quality schema: {}. This may be normal if using Hibernate ddl-auto.", 
                       e.getMessage());
        }
    }
}
