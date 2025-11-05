package com.bcbs239.regtech.core.application.outbox;

import com.bcbs239.regtech.core.application.integration.DomainEventDispatcher;
import com.bcbs239.regtech.core.domain.events.DomainEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import java.util.function.Consumer;

/**
 * Configuration for outbox processing beans.
 */
@Configuration
@EnableConfigurationProperties({OutboxOptions.class})
public class OutboxProcessingConfiguration {

}

