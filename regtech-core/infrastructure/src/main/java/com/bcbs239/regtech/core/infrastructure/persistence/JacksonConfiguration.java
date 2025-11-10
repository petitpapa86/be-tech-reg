package com.bcbs239.regtech.core.infrastructure.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * Jackson configuration to register custom modules for the application-wide ObjectMapper.
 * Ensures MaybeJacksonModule is registered for proper serialization/deserialization of Maybe<T>.
 */
@Configuration
public class JacksonConfiguration {

    @Bean
    @Primary
    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
        ObjectMapper mapper = builder.build();
        mapper.registerModule(new JavaTimeModule());
        mapper.registerModule(new MaybeJacksonModule());
        return mapper;
    }
}
