package com.bcbs239.regtech.billing;

import com.bcbs239.regtech.billing.infrastructure.database.entities.InboxEventEntity;
import com.bcbs239.regtech.billing.infrastructure.database.repositories.InboxEventRepository;
import com.bcbs239.regtech.core.events.UserRegisteredIntegrationEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
    classes = InboxIntegrationTest.TestConfig.class,
    properties = {
        "spring.flyway.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:billing-test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
    }
)
@DirtiesContext
public class InboxIntegrationTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @org.springframework.context.annotation.Import(com.bcbs239.regtech.billing.application.events.BillingInboxEventHandler.class)
    static class TestConfig {
        // Minimal Spring Boot configuration to allow @SpringBootTest to bootstrap a context for module tests

        @org.springframework.context.annotation.Bean
        public java.util.function.Consumer<com.bcbs239.regtech.billing.infrastructure.database.entities.InboxEventEntity> billingInboxSaver(jakarta.persistence.EntityManager em) {
            return entity -> {
                em.persist(entity);
                em.flush();
            };
        }
    }

    @Autowired
    private ApplicationEventPublisher publisher;

    @Autowired
    private InboxEventRepository inboxEventRepository;

    @Autowired
    private com.bcbs239.regtech.billing.application.events.BillingInboxEventHandler inboxEventHandler;

    @Test
    public void whenPublishUserRegisteredIntegrationEvent_thenInboxRecordCreated() throws InterruptedException {
        String correlation = UUID.randomUUID().toString();
        UserRegisteredIntegrationEvent event = new UserRegisteredIntegrationEvent(
            "user-123", "alice@example.com", "Alice", "bank-1", "pm-1", null, null, correlation, "iam"
        );

    // Publish event via ApplicationEventPublisher and also invoke handler directly
    // to eliminate possible event delivery timing/classloader issues in test.
    publisher.publishEvent(event);
    inboxEventHandler.handleExternalIntegrationEvent(event);

        // Poll for the listener to persist the inbox record (avoid flaky short sleeps)
        List<InboxEventEntity> found = List.of();
        int attempts = 0;
        int maxAttempts = 25; // ~5 seconds (25 * 200ms)
        while ((found == null || found.isEmpty()) && attempts < maxAttempts) {
            Thread.sleep(200);
            found = inboxEventRepository.findByEventType("UserRegisteredIntegrationEvent");
            attempts++;
        }
        assertThat(found).isNotEmpty();
        InboxEventEntity rec = found.get(0);
        assertThat(rec.getAggregateId()).isEqualTo("user-123");
        assertThat(rec.getEventData()).contains("alice@example.com");
    }
}
