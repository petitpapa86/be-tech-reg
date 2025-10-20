package com.bcbs239.regtech.billing;

import com.bcbs239.regtech.core.inbox.CoreInboxConfiguration;
import com.bcbs239.regtech.core.inbox.InboxMessage;
import com.bcbs239.regtech.core.inbox.InboxMessageJpaRepository;
import com.bcbs239.regtech.core.inbox.InboxMessageRepository;
import com.bcbs239.regtech.core.inbox.IntegrationEventConsumer;
import com.bcbs239.regtech.iam.domain.users.events.UserRegisteredIntegrationEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;

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
    @ComponentScan(
        basePackages = {"com.bcbs239.regtech.core.inbox"}
    )
    @Import({CoreInboxConfiguration.class, BillingTestJpaConfiguration.class, IntegrationEventConsumer.class})
    static class TestConfig {
        // Minimal Spring Boot configuration to allow @SpringBootTest to bootstrap a context for module tests
        // IntegrationEventConsumer is now automatically configured as a @Component in core module
    }

    @Configuration
    @EnableJpaRepositories(basePackages = "com.bcbs239.regtech.core.inbox")
    @EntityScan(basePackages = "com.bcbs239.regtech.core.inbox")
    static class BillingTestJpaConfiguration {
    }

    @Autowired
    private ApplicationEventPublisher publisher;

    @Autowired
    private InboxMessageRepository inboxMessageRepository;

    @Test
    public void whenPublishUserRegisteredIntegrationEvent_thenInboxRecordCreated() throws InterruptedException {
        UUID userId = UUID.fromString("12345678-1234-1234-1234-123456789012");
        UserRegisteredIntegrationEvent event = new UserRegisteredIntegrationEvent(
            userId, "alice@example.com", "Alice", "Smith", "bank-1"
        );

        // Publish event via ApplicationEventPublisher
        publisher.publishEvent(event);

        // Poll for the inbox processor to store the message
        List<InboxMessage> found = List.of();
        int attempts = 0;
        int maxAttempts = 25; // ~5 seconds (25 * 200ms)
        while ((found == null || found.isEmpty()) && attempts < maxAttempts) {
            Thread.sleep(200);
            found = inboxMessageRepository.messageLoader().apply(10); // Load up to 10 messages
            found = found.stream()
                .filter(msg -> "UserRegisteredIntegrationEvent".equals(msg.getEventType()))
                .toList();
            attempts++;
        }
        assertThat(found).isNotEmpty();
        InboxMessage rec = found.get(0);
        assertThat(rec.getAggregateId()).isEqualTo(userId.toString());
        assertThat(rec.getPayload()).contains("alice@example.com");
    }
}
