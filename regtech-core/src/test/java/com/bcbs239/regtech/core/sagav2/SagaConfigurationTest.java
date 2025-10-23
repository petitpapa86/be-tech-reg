package com.bcbs239.regtech.core.sagav2;

import com.bcbs239.regtech.core.shared.Result;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for SagaConfiguration bean functions.
 * Demonstrates functional programming principles:
 * - Functions as first-class citizens
 * - No mocking needed - uses real EntityManager
 * - Explicit dependencies
 * - Compiler-driven design with generics
 */
@DataJpaTest
@Import(SagaConfiguration.class)
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.driver-class-name=org.h2.Driver"
})
class SagaConfigurationTest {

    @Autowired
    private Function<AbstractSaga<?>, Result<SagaId>> sagaSaver;

    @Autowired
    private Function<SagaId, AbstractSaga<?>> sagaLoader;

    @Autowired
    private Supplier<Instant> currentTimeSupplier;

    @Test
    void currentTimeSupplier_shouldReturnCurrentTime() {
        // When
        Instant time1 = currentTimeSupplier.get();
        Instant time2 = currentTimeSupplier.get();

        // Then
        assertThat(time1).isNotNull();
        assertThat(time2).isNotNull();
        assertThat(time1).isBeforeOrEqualTo(time2);
    }

    @Test
    void sagaSaver_shouldPersistSagaSuccessfully() {
        // Given
        TestSaga saga = new TestSaga(SagaId.generate(), "test data");

        // When
        Result<SagaId> result = sagaSaver.apply(saga);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).contains(saga.getId());
    }

    @Test
    void sagaLoader_shouldLoadPersistedSaga() {
        // Given
        TestSaga originalSaga = new TestSaga(SagaId.generate(), "test data");
        Result<SagaId> saveResult = sagaSaver.apply(originalSaga);
        assertThat(saveResult.isSuccess()).isTrue();

        // When
        AbstractSaga<?> loadedSaga = sagaLoader.apply(originalSaga.getId());

        // Then
        assertThat(loadedSaga).isNotNull();
        assertThat(loadedSaga.getId()).isEqualTo(originalSaga.getId());
        assertThat(loadedSaga.getSagaType()).isEqualTo(originalSaga.getSagaType());
    }

    @Test
    void sagaLoader_shouldReturnNullForNonExistentSaga() {
        // Given
        SagaId nonExistentId = SagaId.generate();

        // When
        AbstractSaga<?> loadedSaga = sagaLoader.apply(nonExistentId);

        // Then
        assertThat(loadedSaga).isNull();
    }

    // Test implementation
    static class TestSaga extends AbstractSaga<String> {
        public TestSaga(SagaId id, String data) {
            super(id, "TestSaga", data);
        }

        @Override
        protected void updateStatus() {
            // Test implementation - do nothing
        }
    }
}