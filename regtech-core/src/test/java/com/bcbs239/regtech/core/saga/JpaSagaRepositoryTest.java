package com.bcbs239.regtech.core.saga;

import com.bcbs239.regtech.core.shared.Maybe;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;

import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = JpaSagaRepositoryTest.TestConfig.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:jpasagarepo_test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "spring.flyway.enabled=false"
})
class JpaSagaRepositoryTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackages = {"com.bcbs239.regtech.core.infrastructure.entities"})
    static class TestConfig {
        // Minimal Spring Boot configuration for JPA test slice
    }

    @Autowired
    private EntityManager entityManager;

    @Test
    @Transactional
    void saveAndLoadSaga_shouldReturnMaybeSome() {
        ObjectMapper objectMapper = new ObjectMapper();

        Function<AbstractSaga<?>, com.bcbs239.regtech.core.shared.Result<SagaId>> saver = JpaSagaRepository.sagaSaver(entityManager, objectMapper);
        Function<SagaId, Maybe<AbstractSaga<?>>> loader = JpaSagaRepository.sagaLoader(entityManager, objectMapper);

        SagaId id = SagaId.generate();
        // Using TestSaga from test package; data is a simple string per mapping in getSagaDataClassName
        com.bcbs239.regtech.core.sagav2.TestSaga saga = new com.bcbs239.regtech.core.sagav2.TestSaga(id, "test-data");

        // Save (will be in the test transaction)
        saver.apply(saga);

        // Flush and clear to simulate separate transaction/session
        entityManager.flush();
        entityManager.clear();

        Maybe<AbstractSaga<?>> maybe = loader.apply(id);
        assertThat(maybe).isNotNull();
        assertThat(maybe.isEmpty()).isFalse();

        AbstractSaga<?> loaded = maybe.getValue();
        assertThat(loaded).isNotNull();
        assertThat(loaded.getId().id()).isEqualTo(id.id());
        assertThat(loaded.getSagaType()).isEqualTo("TestSaga");
    }
}
