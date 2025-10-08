package com.bcbs239.regtech.billing.infrastructure.config;

import com.bcbs239.regtech.billing.application.commands.UsageMetrics;
import com.bcbs239.regtech.billing.application.sagas.MonthlyBillingSaga;
import com.bcbs239.regtech.billing.application.sagas.MonthlyBillingSagaData;
import com.bcbs239.regtech.billing.infrastructure.monitoring.BillingPerformanceMetricsService;
import com.bcbs239.regtech.billing.infrastructure.monitoring.BillingSagaAuditService;
import com.bcbs239.regtech.billing.infrastructure.monitoring.MonitoredSagaWrapper;
import com.bcbs239.regtech.billing.infrastructure.repositories.JpaBillingAccountRepository;
import com.bcbs239.regtech.billing.infrastructure.repositories.JpaInvoiceRepository;
import com.bcbs239.regtech.billing.infrastructure.repositories.JpaSubscriptionRepository;
import com.bcbs239.regtech.billing.infrastructure.stripe.StripeInvoice;
import com.bcbs239.regtech.billing.infrastructure.stripe.StripeService;
import com.bcbs239.regtech.core.events.CrossModuleEventBus;
import com.bcbs239.regtech.core.saga.Saga;
import com.bcbs239.regtech.core.saga.SagaClosures;
import com.bcbs239.regtech.core.shared.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Configuration for Monthly Billing Saga dependencies.
 * Provides closure-based dependency injection for better testability.
 */
@Configuration
public class MonthlyBillingSagaConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(MonthlyBillingSagaConfiguration.class);

    @Bean
    public MonthlyBillingSaga monthlyBillingSaga(
            JpaBillingAccountRepository billingAccountRepository,
            JpaSubscriptionRepository subscriptionRepository,
            JpaInvoiceRepository invoiceRepository,
            StripeService stripeService,
            CrossModuleEventBus eventBus) {

        return new MonthlyBillingSaga(
            sagaMessagePublisher(),
            sagaLogger(),
            usageMetricsQuery(),
            billingAccountRepository.billingAccountFinder(),
            subscriptionRepository.activeSubscriptionFinder(),
            stripeInvoiceCreator(stripeService),
            invoiceRepository.invoiceSaver(),
            eventPublisher(eventBus)
        );
    }

    @Bean
    public Saga<MonthlyBillingSagaData> monitoredMonthlyBillingSaga(
            MonthlyBillingSaga monthlyBillingSaga,
            BillingSagaAuditService auditService,
            BillingPerformanceMetricsService metricsService,
            ObjectMapper objectMapper) {

        return MonitoredSagaWrapper.wrap(monthlyBillingSaga, auditService, metricsService, objectMapper);
    }

    // Closure implementations

    @Bean
    public SagaClosures.MessagePublisher sagaMessagePublisher() {
        return message -> {
            logger.info("Publishing saga message: {} from {} to {}", 
                message.getType(), message.getSource(), message.getTarget());
            // In a real implementation, this would publish to a message bus
            // For now, we'll just log the message
        };
    }

    @Bean
    public SagaClosures.Logger sagaLogger() {
        return (level, message, args) -> {
            switch (level.toLowerCase()) {
                case "error" -> logger.error(message, args);
                case "warn" -> logger.warn(message, args);
                case "info" -> logger.info(message, args);
                case "debug" -> logger.debug(message, args);
                default -> logger.info(message, args);
            }
        };
    }

    @Bean
    public Function<MonthlyBillingSaga.UsageQuery, Result<UsageMetrics>> usageMetricsQuery() {
        return query -> {
            logger.info("Querying usage metrics for user {} in period {}", 
                query.userId(), query.billingPeriod());
            
            // Mock implementation - in production this would call the ingestion context
            try {
                // Simulate different usage patterns for testing
                int baseUsage = 7500; // Base usage under the limit
                int randomVariation = (int) (Math.random() * 5000); // 0-5000 additional
                int totalExposures = baseUsage + randomVariation;
                
                UsageMetrics metrics = UsageMetrics.of(
                    query.userId(),
                    query.billingPeriod(),
                    totalExposures,
                    150 + (int) (Math.random() * 100), // 150-250 documents
                    (long) (1024L * 1024L * (400 + Math.random() * 200)) // 400-600MB
                );
                
                logger.info("Retrieved usage metrics: {} exposures, {} documents", 
                    metrics.totalExposures(), metrics.documentsProcessed());
                
                return Result.success(metrics);
                
            } catch (Exception e) {
                logger.error("Failed to query usage metrics: {}", e.getMessage());
                return Result.failure(new com.bcbs239.regtech.core.shared.ErrorDetail(
                    "USAGE_METRICS_QUERY_FAILED",
                    "Failed to query usage metrics: " + e.getMessage(),
                    "billing.usage.metrics.query.failed"
                ));
            }
        };
    }

    @Bean
    public Function<MonthlyBillingSaga.InvoiceCreationData, Result<StripeInvoice>> stripeInvoiceCreator(
            StripeService stripeService) {
        return data -> {
            logger.info("Creating Stripe invoice for customer {} with amount {}", 
                data.customerId(), data.amount());
            
            return stripeService.createInvoice(data.customerId(), data.amount(), data.description());
        };
    }

    @Bean
    public Consumer<Object> eventPublisher(CrossModuleEventBus eventBus) {
        return event -> {
            logger.info("Publishing domain event: {}", event.getClass().getSimpleName());
            eventBus.publishEvent(event);
        };
    }
}