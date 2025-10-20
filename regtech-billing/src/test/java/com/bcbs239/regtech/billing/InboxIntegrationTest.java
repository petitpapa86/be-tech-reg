package com.bcbs239.regtech.billing;

import com.bcbs239.regtech.core.inbox.IntegrationEventDispatcher;
import com.bcbs239.regtech.core.inbox.InboxMessage;
import com.bcbs239.regtech.core.inbox.InboxMessageJpaRepository;
import com.bcbs239.regtech.core.inbox.InboxMessageRepository;
import com.bcbs239.regtech.core.inbox.IntegrationEventConsumer;
import com.bcbs239.regtech.core.inbox.ProcessInboxJob;
import com.bcbs239.regtech.core.inbox.InboxMessageEntity;
import com.bcbs239.regtech.iam.domain.users.events.UserRegisteredIntegrationEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;
import java.util.UUID;
import java.util.function.BiFunction;
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
        basePackages = {
            "com.bcbs239.regtech.core.inbox",
            "com.bcbs239.regtech.billing.application.events"
        }
    )
    @Import({BillingTestJpaConfiguration.class, IntegrationEventConsumer.class})
    static class TestConfig {
        // Minimal Spring Boot configuration to allow @SpringBootTest to bootstrap a context for module tests
        // IntegrationEventConsumer is now automatically configured as a @Component in core module

        @Bean
        public InboxMessageRepository inboxMessageRepository(InboxMessageJpaRepository jpaRepository) {
            return new InboxMessageRepository() {
                @Override
                public Function<Integer, List<InboxMessage>> messageLoader() {
                    return batchSize -> jpaRepository.findPendingMessages(InboxMessageEntity.ProcessingStatus.PENDING)
                        .stream()
                        .limit(batchSize)
                        .map(entity -> new InboxMessage(
                            entity.getId(),
                            entity.getEventType(),
                            entity.getEventData(),
                            entity.getReceivedAt(),
                            entity.getAggregateId()
                        ))
                        .toList();
                }

                @Override
                public Function<String, Boolean> markProcessed() {
                    return id -> {
                        int updated = jpaRepository.markAsProcessed(id, java.time.Instant.now());
                        return updated > 0;
                    };
                }

                @Override
                public Function<Integer, List<InboxMessage>> failedMessageLoader() {
                    return batchSize -> jpaRepository.findFailedMessagesEligibleForRetry(java.time.Instant.now())
                        .stream()
                        .limit(batchSize)
                        .map(entity -> new InboxMessage(
                            entity.getId(),
                            entity.getEventType(),
                            entity.getEventData(),
                            entity.getReceivedAt(),
                            entity.getAggregateId()
                        ))
                        .toList();
                }

                @Override
                public BiFunction<String, String, Boolean> markFailed() {
                    return (id, errorMessage) -> {
                        // For simplicity, mark as permanently failed for now
                        // In a real implementation, you might want to implement retry logic
                        int updated = jpaRepository.markAsPermanentlyFailed(id, errorMessage);
                        return updated > 0;
                    };
                }
            };
        }

        @Bean
        public ProcessInboxJob processInboxJob(
                InboxMessageJpaRepository inboxRepository,
                ObjectMapper objectMapper,
                IntegrationEventDispatcher integrationEventDispatcher) {
            return new ProcessInboxJob(inboxRepository, objectMapper, integrationEventDispatcher);
        }
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

    @Autowired
    private ProcessInboxJob processInboxJob;

    @Test
    public void whenPublishUserRegisteredIntegrationEvent_thenInboxRecordCreated() throws InterruptedException {
        UUID userId = UUID.fromString("12345678-1234-1234-1234-123456789012");
        UserRegisteredIntegrationEvent event = new UserRegisteredIntegrationEvent(
            userId, "alice@example.com", "Alice", "Smith", "bank-1"
        );

        // Publish event via ApplicationEventPublisher
        publisher.publishEvent(event);

        // Poll for the inbox processor to store the message
        List<InboxMessage> found = null;
        int attempts = 0;
        int maxAttempts = 25; // ~5 seconds (25 * 200ms)
        while ((found == null || found.isEmpty()) && attempts < maxAttempts) {
            Thread.sleep(200);
            found = inboxMessageRepository.messageLoader().apply(10); // Load up to 10 messages
            found = found.stream()
                .filter(msg -> "com.bcbs239.regtech.iam.domain.users.events.UserRegisteredIntegrationEvent".equals(msg.getEventType()))
                .toList();
            attempts++;
        }
        assertThat(found).isNotEmpty();
        InboxMessage rec = found.get(0);
        assertThat(rec.getAggregateId()).isEqualTo(userId.toString());
        assertThat(rec.getPayload()).contains("alice@example.com");

        // Now run the inbox processor to handle the message
        processInboxJob.processInboxMessages();
        
        // Verify the message was processed
        List<InboxMessage> processedMessages = inboxMessageRepository.messageLoader().apply(10).stream()
            .filter(msg -> "com.bcbs239.regtech.iam.domain.users.events.UserRegisteredIntegrationEvent".equals(msg.getEventType()))
            .toList();
        assertThat(processedMessages).isEmpty(); // Should be processed and not in pending list
    }
}
