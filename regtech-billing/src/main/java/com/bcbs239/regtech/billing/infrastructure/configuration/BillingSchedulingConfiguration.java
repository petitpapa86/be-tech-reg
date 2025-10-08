package com.bcbs239.regtech.billing.infrastructure.configuration;

import com.bcbs239.regtech.billing.application.sagas.MonthlyBillingSaga;
import com.bcbs239.regtech.billing.application.sagas.MonthlyBillingSagaData;
import com.bcbs239.regtech.billing.infrastructure.monitoring.BillingPerformanceMetricsService;
import com.bcbs239.regtech.billing.infrastructure.monitoring.BillingSagaAuditService;
import com.bcbs239.regtech.billing.infrastructure.monitoring.MonitoredSagaWrapper;
import com.bcbs239.regtech.billing.infrastructure.scheduling.DunningActionExecutor;
import com.bcbs239.regtech.billing.infrastructure.scheduling.DunningNotificationService;
import com.bcbs239.regtech.billing.infrastructure.scheduling.DunningProcessScheduler;
import com.bcbs239.regtech.billing.infrastructure.scheduling.MonthlyBillingScheduler;
import com.bcbs239.regtech.billing.infrastructure.repositories.JpaBillingAccountRepository;
import com.bcbs239.regtech.billing.infrastructure.repositories.JpaDunningCaseRepository;
import com.bcbs239.regtech.billing.infrastructure.repositories.JpaInvoiceRepository;
import com.bcbs239.regtech.billing.infrastructure.repositories.JpaSubscriptionRepository;
import com.bcbs239.regtech.core.saga.Saga;
import com.bcbs239.regtech.core.saga.SagaOrchestrator;
import com.fasterxml.jackson.databind.ObjectMapper;
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
     * Configure monitored monthly billing saga for scheduler use.
     * Wraps the saga with monitoring and audit logging capabilities.
     */
    @Bean
    public Saga<MonthlyBillingSagaData> monitoredMonthlyBillingSaga(
            MonthlyBillingSaga monthlyBillingSaga,
            BillingSagaAuditService auditService,
            BillingPerformanceMetricsService metricsService,
            ObjectMapper objectMapper) {
        
        return MonitoredSagaWrapper.wrap(
            monthlyBillingSaga,
            auditService,
            metricsService,
            objectMapper
        );
    }

    /**
     * Configure monthly billing scheduler.
     * Conditionally enabled based on configuration property.
     */
    @Bean
    @ConditionalOnProperty(
        name = "regtech.billing.scheduling.monthly-billing.enabled", 
        havingValue = "true", 
        matchIfMissing = true
    )
    public MonthlyBillingScheduler monthlyBillingScheduler(
            SagaOrchestrator sagaOrchestrator,
            Saga<MonthlyBillingSagaData> monitoredMonthlyBillingSaga,
            JpaSubscriptionRepository subscriptionRepository,
            JpaBillingAccountRepository billingAccountRepository) {
        
        return new MonthlyBillingScheduler(
            sagaOrchestrator,
            monitoredMonthlyBillingSaga,
            subscriptionRepository,
            billingAccountRepository
        );
    }

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