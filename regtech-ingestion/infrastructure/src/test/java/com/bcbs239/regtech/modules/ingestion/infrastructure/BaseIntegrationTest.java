package com.bcbs239.regtech.modules.ingestion.infrastructure;

import com.bcbs239.regtech.modules.ingestion.infrastructure.config.IngestionTestConfiguration;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for integration tests in the ingestion module.
 * Provides common test configuration and container setup.
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(
    classes = {IngestionTestConfiguration.class},
    properties = {
        "spring.profiles.active=test",
        "ingestion.processing.async-enabled=false",
        "ingestion.outbox.processing-interval=1000",
        "logging.level.com.bcbs239.regtech.modules.ingestion=DEBUG"
    }
)
@ActiveProfiles("test")
@Testcontainers
public abstract class BaseIntegrationTest {
    
    // Common test setup and utilities can be added here
}

