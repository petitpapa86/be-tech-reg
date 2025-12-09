package com.bcbs239.regtech.iam.infrastructure.config;

import com.bcbs239.regtech.iam.infrastructure.database.services.RoleDatabaseInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Application startup component that initializes roles and permissions in the database.
 * Runs on application startup to ensure the database has the required roles.
 */
@Component
@Profile("!test") // Skip in test profile to avoid database setup in tests
public class RoleDatabaseInitializerRunner implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(RoleDatabaseInitializerRunner.class);

    private final RoleDatabaseInitializer roleDatabaseInitializer;

    public RoleDatabaseInitializerRunner(RoleDatabaseInitializer roleDatabaseInitializer) {
        this.roleDatabaseInitializer = roleDatabaseInitializer;
    }

    @Override
    public void run(String... args) throws Exception {
        try {
            logger.info("Starting role and permission database initialization...");
            roleDatabaseInitializer.initializeRolesAndPermissions();
            logger.info("Role and permission database initialization completed successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize roles and permissions in database", e);
            // Don't fail the application startup - roles can be initialized manually if needed
            // throw e; // Uncomment to fail startup on initialization error
        }
    }
}