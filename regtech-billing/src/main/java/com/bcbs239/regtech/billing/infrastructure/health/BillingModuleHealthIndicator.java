package com.bcbs239.regtech.billing.infrastructure.health;

import com.bcbs239.regtech.billing.domain.valueobjects.BillingAccountStatus;
import com.bcbs239.regtech.billing.infrastructure.repositories.JpaBillingAccountRepository;
import com.bcbs239.regtech.billing.infrastructure.stripe.StripeService;
import com.bcbs239.regtech.core.health.ModuleHealthIndicator;
import com.bcbs239.regtech.core.shared.Result;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Health indicator for the Billing module.
 * Monitors database connectivity, Stripe API connectivity, and billing account status.
 */
@Component
public class BillingModuleHealthIndicator implements ModuleHealthIndicator {

    @PersistenceContext
    private EntityManager entityManager;
    
    private final StripeService stripeService;
    private final JpaBillingAccountRepository billingAccountRepository;

    public BillingModuleHealthIndicator(StripeService stripeService,
                                      JpaBillingAccountRepository billingAccountRepository) {
        this.stripeService = stripeService;
        this.billingAccountRepository = billingAccountRepository;
    }

    @Override
    public String getModuleName() {
        return "billing";
    }

    @Override
    public Health health() {
        Map<String, Object> details = new HashMap<>();
        boolean allHealthy = true;
        
        // Check database connectivity
        HealthCheckResult dbHealth = checkDatabaseConnectivity();
        details.put("database", dbHealth.details());
        if (!dbHealth.isHealthy()) {
            allHealthy = false;
        }
        
        // Check Stripe API connectivity
        HealthCheckResult stripeHealth = checkStripeConnectivity();
        details.put("stripe", stripeHealth.details());
        if (!stripeHealth.isHealthy()) {
            allHealthy = false;
        }
        
        // Check billing account status monitoring
        HealthCheckResult accountHealth = checkBillingAccountStatus();
        details.put("billingAccounts", accountHealth.details());
        if (!accountHealth.isHealthy()) {
            allHealthy = false;
        }
        
        details.put("timestamp", Instant.now().toString());
        
        return allHealthy ? 
            Health.up().withDetails(details).build() : 
            Health.down().withDetails(details).build();
    }

    /**
     * Check database connectivity by executing a simple query
     */
    private HealthCheckResult checkDatabaseConnectivity() {
        try {
            // Execute a simple query to test database connectivity
            Long count = entityManager.createQuery(
                "SELECT COUNT(ba) FROM BillingAccountEntity ba", Long.class)
                .getSingleResult();
                
            Map<String, Object> details = new HashMap<>();
            details.put("status", "UP");
            details.put("totalAccounts", count);
            details.put("message", "Database connection successful");
            
            return new HealthCheckResult(true, details);
            
        } catch (Exception e) {
            Map<String, Object> details = new HashMap<>();
            details.put("status", "DOWN");
            details.put("error", e.getMessage());
            details.put("message", "Database connection failed");
            
            return new HealthCheckResult(false, details);
        }
    }

    /**
     * Check Stripe API connectivity by attempting to retrieve account information
     */
    private HealthCheckResult checkStripeConnectivity() {
        try {
            // Test Stripe connectivity by creating a test customer (this will validate API key)
            Result<com.bcbs239.regtech.billing.infrastructure.stripe.StripeCustomer> testResult = 
                stripeService.createCustomer("health-check@test.com", "Health Check Test");
            
            Map<String, Object> details = new HashMap<>();
            
            if (testResult.isSuccess()) {
                details.put("status", "UP");
                details.put("message", "Stripe API connection successful");
                details.put("apiKeyValid", true);
            } else {
                details.put("status", "DOWN");
                details.put("message", "Stripe API connection failed");
                details.put("error", testResult.getError().map(e -> e.getMessage()).orElse("Unknown error"));
                details.put("apiKeyValid", false);
            }
            
            return new HealthCheckResult(testResult.isSuccess(), details);
            
        } catch (Exception e) {
            Map<String, Object> details = new HashMap<>();
            details.put("status", "DOWN");
            details.put("error", e.getMessage());
            details.put("message", "Stripe API connectivity check failed");
            details.put("apiKeyValid", false);
            
            return new HealthCheckResult(false, details);
        }
    }

    /**
     * Check billing account status monitoring by querying account status distribution
     */
    private HealthCheckResult checkBillingAccountStatus() {
        try {
            // Query billing account status distribution
            Map<BillingAccountStatus, Long> statusCounts = new HashMap<>();
            
            for (BillingAccountStatus status : BillingAccountStatus.values()) {
                Long count = entityManager.createQuery(
                    "SELECT COUNT(ba) FROM BillingAccountEntity ba WHERE ba.status = :status", Long.class)
                    .setParameter("status", status)
                    .getSingleResult();
                statusCounts.put(status, count);
            }
            
            // Calculate health metrics
            long totalAccounts = statusCounts.values().stream().mapToLong(Long::longValue).sum();
            long activeAccounts = statusCounts.getOrDefault(BillingAccountStatus.ACTIVE, 0L);
            long suspendedAccounts = statusCounts.getOrDefault(BillingAccountStatus.SUSPENDED, 0L);
            long pastDueAccounts = statusCounts.getOrDefault(BillingAccountStatus.PAST_DUE, 0L);
            
            Map<String, Object> details = new HashMap<>();
            details.put("status", "UP");
            details.put("totalAccounts", totalAccounts);
            details.put("activeAccounts", activeAccounts);
            details.put("suspendedAccounts", suspendedAccounts);
            details.put("pastDueAccounts", pastDueAccounts);
            details.put("statusDistribution", statusCounts);
            
            // Consider unhealthy if more than 50% of accounts are suspended or past due
            boolean isHealthy = totalAccounts == 0 || 
                (suspendedAccounts + pastDueAccounts) < (totalAccounts * 0.5);
            
            if (!isHealthy) {
                details.put("warning", "High percentage of accounts in problematic status");
            }
            
            details.put("message", "Billing account status monitoring operational");
            
            return new HealthCheckResult(isHealthy, details);
            
        } catch (Exception e) {
            Map<String, Object> details = new HashMap<>();
            details.put("status", "DOWN");
            details.put("error", e.getMessage());
            details.put("message", "Billing account status monitoring failed");
            
            return new HealthCheckResult(false, details);
        }
    }

    /**
     * Internal record to represent health check results
     */
    private record HealthCheckResult(boolean isHealthy, Map<String, Object> details) {}
}