package com.bcbs239.regtech.core.saga;

import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.core.shared.Result;
import jakarta.persistence.EntityManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.time.Instant;
import java.util.function.Function;
import java.util.function.Supplier;

@Configuration
@EnableAsync
public class SagaConfiguration {

    @Bean
    @Primary
    public TaskExecutor sagaTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("saga-");
        executor.initialize();
        return executor;
    }

    @Bean
    public Function<AbstractSaga<?>, Result<SagaId>> sagaSaver(EntityManager entityManager) {
        return saga -> {
            try {
                entityManager.persist(saga);
                return Result.success(saga.getId());
            } catch (Exception e) {
                return Result.failure(new ErrorDetail("SAGA_SAVE_ERROR", "Failed to save saga: " + e.getMessage()));
            }
        };
    }

    @Bean
    public Function<SagaId, AbstractSaga<?>> sagaLoader(EntityManager entityManager) {
        return sagaId -> entityManager.find(AbstractSaga.class, sagaId);
    }

    @Bean
    public Supplier<Instant> currentTimeSupplier() {
        return Instant::now;
    }
}