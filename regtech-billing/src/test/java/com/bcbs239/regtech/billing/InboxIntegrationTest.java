package com.bcbs239.regtech.billing;

import com.bcbs239.regtech.core.inbox.InboxMessageOperations;
import com.bcbs239.regtech.core.inbox.MessageProcessor;
import com.bcbs239.regtech.core.inbox.ProcessInboxJob;
import com.bcbs239.regtech.core.inbox.InboxMessageEntity;
import com.bcbs239.regtech.core.events.UserRegisteredIntegrationEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.function.BiConsumer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.annotation.DirtiesContext;

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
    @ComponentScan(
        basePackages = {
            "com.bcbs239.regtech.core.inbox"
        }
    )
    @Import({BillingTestJpaConfiguration.class})
    static class TestConfig {
        // Minimal Spring Boot configuration to allow @SpringBootTest to bootstrap a context for module tests
        // Components for fetcher/processor should be available from core module

        @Bean
        public ProcessInboxJob processInboxJob(
                InboxMessageOperations repository,
                MessageProcessor messageProcessor,
                BiConsumer<String, String> markAsPermanentlyFailedFn) {
            return new ProcessInboxJob(repository.findPendingMessagesFn(), messageProcessor, markAsPermanentlyFailedFn);
        }
    }

    @Configuration
    @EnableJpaRepositories(basePackages = "com.bcbs239.regtech.core.inbox")
    @EntityScan(basePackages = "com.bcbs239.regtech.core.inbox")
    static class BillingTestJpaConfiguration {
    }

    @Autowired
    private InboxMessageOperations inboxMessageJpaRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProcessInboxJob processInboxJob;

    @Test
    public void whenPublishUserRegisteredIntegrationEvent_thenInboxRecordCreated() throws Exception {
        UUID userId = UUID.fromString("12345678-1234-1234-1234-123456789012");
        UserRegisteredIntegrationEvent event = new UserRegisteredIntegrationEvent(
            userId.toString(), "alice@example.com", "Alice Smith", "bank-1", 
            "pm_123", "555-0123", null
        );

        // Directly store the event in the inbox (simulating external message arrival)
        String eventData = objectMapper.writeValueAsString(event);
        InboxMessageEntity inboxMessage = new InboxMessageEntity(
            event.getClass().getName(),
            eventData,
            userId.toString()
        );
        inboxMessageJpaRepository.save(inboxMessage);

        // Now run the inbox processor to handle the message
        processInboxJob.processInboxMessages();
        
        // Verify the message was processed
        var processedMessage = inboxMessageJpaRepository.findById(inboxMessage.getId());
        assertThat(processedMessage).isPresent();
        assertThat(processedMessage.get().getProcessingStatus()).isEqualTo(InboxMessageEntity.ProcessingStatus.PROCESSED);
    }
}
