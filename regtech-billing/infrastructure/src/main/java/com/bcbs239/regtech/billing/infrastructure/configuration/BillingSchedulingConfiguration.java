package com.bcbs239.regtech.billing.infrastructure.configuration;

// import com.bcbs239.regtech.billing.infrastructure.observability.BillingPerformanceMetricsService;
// import com.bcbs239.regtech.billing.infrastructure.observability.BillingSagaAuditService;

import com.bcbs239.regtech.billing.infrastructure.database.repositories.JpaBillingAccountRepository;
import com.bcbs239.regtech.billing.infrastructure.database.repositories.JpaDunningCaseRepository;
import com.bcbs239.regtech.billing.infrastructure.database.repositories.JpaInvoiceRepository;
import com.bcbs239.regtech.billing.infrastructure.jobs.DunningActionExecutor;
import com.bcbs239.regtech.billing.infrastructure.jobs.DunningNotificationService;
import com.bcbs239.regtech.billing.infrastructure.jobs.DunningProcessScheduler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * Configuration for billing-related scheduled jobs and automation.
 * Enables scheduling and configures schedulers with proper dependencies.
 */
@Configuration
@EnableScheduling
public class BillingSchedulingConfiguration {

    /**
     * Configure task scheduler for billing operations.
     * Uses a dedicated thread pool for billing-related scheduled tasks.
     */
    @Bean("billingTaskScheduler")
    public ThreadPoolTaskScheduler billingTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(5);
        scheduler.setThreadNamePrefix("billing-scheduler-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(30);
        return scheduler;
    }

    /**
     * Configure monthly billing scheduler.
     * Conditionally enabled based on configuration property.
     */

    /**
     * Configure dunning notification service.
     */
    @Bean
    public DunningNotificationService dunningNotificationService() {
        return new DunningNotificationService();
    }

    /**
     * Configure dunning action executor.
     */
    @Bean
    public DunningActionExecutor dunningActionExecutor(
            JpaInvoiceRepository invoiceRepository,
            JpaBillingAccountRepository billingAccountRepository,
            DunningNotificationService notificationService) {
        
        return new DunningActionExecutor(
            invoiceRepository,
            billingAccountRepository,
            notificationService
        );
    }

    /**
     * Configure dunning process scheduler.
     * Conditionally enabled based on configuration property.
     */
    @Bean
    @ConditionalOnProperty(
        name = "regtech.billing.scheduling.dunning-process.enabled", 
        havingValue = "true", 
        matchIfMissing = true
    )
    public DunningProcessScheduler dunningProcessScheduler(
            JpaInvoiceRepository invoiceRepository,
            JpaDunningCaseRepository dunningCaseRepository,
            DunningActionExecutor dunningActionExecutor) {
        
        return new DunningProcessScheduler(
            invoiceRepository,
            dunningCaseRepository,
            dunningActionExecutor
        );
    }
}
