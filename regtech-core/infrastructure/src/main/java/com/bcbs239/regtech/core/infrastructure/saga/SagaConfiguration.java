package com.bcbs239.regtech.core.infrastructure.saga;

import com.bcbs239.regtech.core.domain.core.Result;
import com.bcbs239.regtech.core.domain.core.Maybe;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.TaskExecutor;

import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Supplier;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.core.task.VirtualThreadTaskExecutor;

@Configuration
@EnableAsync
public class SagaConfiguration {

    @Bean
    @Primary
    public TaskExecutor sagaTaskExecutor() {
        return new VirtualThreadTaskExecutor();
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
