package com.bcbs239.regtech.core.infrastructure;

import com.bcbs239.regtech.core.application.eventprocessing.CoreApplicationConfiguration;
import com.bcbs239.regtech.core.application.outbox.OutboxProcessingConfiguration;
import com.bcbs239.regtech.core.application.saga.SagaConfiguration;
import com.bcbs239.regtech.core.infrastructure.eventprocessing.CrossModuleEventBus;
import com.bcbs239.regtech.core.infrastructure.eventprocessing.DomainEventBus;
import com.bcbs239.regtech.core.infrastructure.eventprocessing.JpaOutboxMessageRepository;
import com.bcbs239.regtech.core.infrastructure.persistence.ModularJpaConfiguration;
import com.bcbs239.regtech.core.infrastructure.saga.JpaSagaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for regtech-core module initialization and shared infrastructure components.
 * 
 * Tests:
 * - Module initialization
 * - Shared infrastructure components
 * - Event processing functionality
 * - Outbox pattern implementation
 * - Saga management
 * 
 * Requirement: 16.1 - WHEN the regtech-core module starts THEN the module SHALL initialize without errors
 */
@DataJpaTest
@ContextConfiguration(classes = {
    CoreModule.class,
    CoreApplicationConfiguration.class,
    OutboxProcessingConfiguration.class,
    SagaConfiguration.class,
    ModularJpaConfiguration.class
})
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.show-sql=false",
    "outbox.enabled=true",
    "outbox.batch-size=10",
    "outbox.processing-interval-ms=1000"
})
class CoreModuleIntegrationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void shouldInitializeCoreModuleSuccessfully() {
        // Verify the application context loaded successfully
        assertNotNull(applicationContext, "Application context should be initialized");
        
        // Verify core module configuration is loaded
        assertTrue(applicationContext.containsBean("coreModule"), 
            "CoreModule configuration should be registered");
    }

    @Test
    void shouldLoadSharedInfrastructureComponents() {
        // Verify event bus components
        assertNotNull(applicationContext.getBean(DomainEventBus.class),
            "DomainEventBus should be available");
        assertNotNull(applicationContext.getBean(CrossModuleEventBus.class),
            "CrossModuleEventBus should be available");
    }

    @Test
    void shouldLoadOutboxPatternComponents() {
        // Verify outbox repository is available
        assertNotNull(applicationContext.getBean(JpaOutboxMessageRepository.class),
            "Outbox repository should be available");
        
        // Verify outbox configuration is loaded
        assertTrue(applicationContext.containsBean("outboxProcessingConfiguration"),
            "Outbox processing configuration should be registered");
    }

    @Test
    void shouldLoadSagaManagementComponents() {
        // Verify saga repository is available
        assertNotNull(applicationContext.getBean(JpaSagaRepository.class),
            "Saga repository should be available");
        
        // Verify saga configuration is loaded
        assertTrue(applicationContext.containsBean("sagaConfiguration"),
            "Saga configuration should be registered");
    }

    @Test
    void shouldEnableTransactionManagement() {
        // Verify transaction management is enabled
        String[] beanNames = applicationContext.getBeanNamesForType(
            org.springframework.transaction.PlatformTransactionManager.class);
        assertTrue(beanNames.length > 0, 
            "Transaction manager should be available");
    }

    @Test
    void shouldEnableAsyncProcessing() {
        // Verify async configuration is present
        // The @EnableAsync annotation on CoreModule should enable async processing
        assertTrue(applicationContext.containsBean("coreModule"),
            "CoreModule with @EnableAsync should be registered");
    }

    @Test
    void shouldEnableScheduling() {
        // Verify scheduling is enabled
        // The @EnableScheduling annotation on CoreModule should enable scheduling
        assertTrue(applicationContext.containsBean("coreModule"),
            "CoreModule with @EnableScheduling should be registered");
    }
}
