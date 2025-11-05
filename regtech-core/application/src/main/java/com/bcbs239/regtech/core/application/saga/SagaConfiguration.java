package com.bcbs239.regtech.core.application.saga;

import com.bcbs239.regtech.core.domain.saga.TimeoutScheduler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.VirtualThreadTaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;

import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
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
    public Supplier<Instant> currentTimeSupplier() {
        return Instant::now;
    }

    @Bean
    public ScheduledExecutorService scheduledExecutorService() {
        return Executors.newScheduledThreadPool(5);
    }

    @Bean
    public TimeoutScheduler timeoutScheduler(ScheduledExecutorService executor) {
        return new com.bcbs239.regtech.core.infrastructure.saga.TimeoutSchedulerService(executor);
    }
}

