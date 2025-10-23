package com.bcbs239.regtech.core.saga;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.TestPropertySource;

import com.bcbs239.regtech.core.shared.Result;
import java.time.Instant;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for SagaConfiguration bean functions.
 * Demonstrates functional programming principles:
 * - Functions as first-class citizens
 * - No mocking needed - uses real implementations
 * - Explicit dependencies
 * - Compiler-driven design with generics
 */
@SpringBootTest(classes = {SagaConfiguration.class, SagaConfigurationTest.TestConfig.class}, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:saga_test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.show-sql=false",
    "spring.flyway.enabled=false",
    "spring.main.allow-bean-definition-overriding=true"
})
class SagaConfigurationTest {

    @Autowired
    private Supplier<Instant> currentTimeSupplier;

    @Test
    void currentTimeSupplier_shouldProvideCurrentTime() {
        // Given
        Instant before = Instant.now();

        // When
        Instant currentTime = currentTimeSupplier.get();

        // Then
        Instant after = Instant.now();
        assertThat(currentTime).isNotNull();
        assertThat(currentTime).isAfterOrEqualTo(before);
        assertThat(currentTime).isBeforeOrEqualTo(after);
    }

    @Configuration
    static class TestConfig {
        // Override JPA-dependent beans with no-op implementations for functional testing
        @Bean
        public Function<AbstractSaga<?>, Result<SagaId>> sagaSaver() {
            return saga -> Result.success(saga.getId());
        }

        @Bean
        public Function<SagaId, AbstractSaga<?>> sagaLoader() {
            return _ -> null;
        }
    }
}