package com.bcbs239.regtech.core.saga;

import com.bcbs239.regtech.core.shared.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.TaskExecutor;

import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Function;
import java.util.function.Supplier;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.core.task.VirtualThreadTaskExecutor;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
@EnableAsync
public class SagaConfiguration {

    @Bean
    @Primary
    public TaskExecutor sagaTaskExecutor() {
        return new VirtualThreadTaskExecutor();
    }

    @Bean
    public Function<AbstractSaga<?>, Result<SagaId>> sagaSaver(EntityManager entityManager, ObjectMapper objectMapper, TransactionTemplate transactionTemplate) {
        Function<AbstractSaga<?>, Result<SagaId>> underlying = JpaSagaRepository.sagaSaver(entityManager, objectMapper);
        // Return a wrapper that executes the underlying saver inside a transaction
        return saga -> transactionTemplate.execute(status -> underlying.apply(saga));
    }

    @Bean
    public Function<SagaId, AbstractSaga<?>> sagaLoader(EntityManager entityManager, ObjectMapper objectMapper) {
        return JpaSagaRepository.sagaLoader(entityManager, objectMapper);
    }

    @Bean
    public Supplier<Instant> currentTimeSupplier() {
        return Instant::now;
    }

    @Bean
    public ScheduledExecutorService scheduledExecutorService() {
        return Executors.newScheduledThreadPool(5);
    }

    @Bean
    public SagaClosures.TimeoutScheduler timeoutScheduler(ScheduledExecutorService executor) {
        return SagaClosures.timeoutScheduler(executor);
    }
}