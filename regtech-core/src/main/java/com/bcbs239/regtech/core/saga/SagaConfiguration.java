package com.bcbs239.regtech.core.saga;

import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.core.shared.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.core.task.VirtualThreadTaskExecutor;

import java.time.Instant;
import java.util.function.Function;
import java.util.function.Supplier;

@Configuration
@EnableAsync
public class SagaConfiguration {

    @Bean
    @Primary
    public TaskExecutor sagaTaskExecutor() {
        return new VirtualThreadTaskExecutor();
    }

    @Bean
    public Function<AbstractSaga<?>, Result<SagaId>> sagaSaver(EntityManager entityManager, ObjectMapper objectMapper) {
        return JpaSagaRepository.sagaSaver(entityManager, objectMapper);
    }

    @Bean
    public Function<SagaId, AbstractSaga<?>> sagaLoader(EntityManager entityManager, ObjectMapper objectMapper) {
        return JpaSagaRepository.sagaLoader(entityManager, objectMapper);
    }

    @Bean
    public Supplier<Instant> currentTimeSupplier() {
        return Instant::now;
    }
}