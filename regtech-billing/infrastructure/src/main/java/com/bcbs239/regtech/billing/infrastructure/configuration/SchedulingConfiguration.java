package com.bcbs239.regtech.billing.infrastructure.configuration;

import java.time.ZoneId;

/**
 * Type-safe configuration for scheduled job settings.
 * Defines cron expressions and timing for billing-related scheduled tasks.
 */
public record SchedulingConfiguration(
    MonthlyBillingSchedule monthlyBilling,
    DunningProcessSchedule dunningProcess
) {
    
    /**
     * Configuration for monthly billing schedule
     */
    public record MonthlyBillingSchedule(
        boolean enabled,
        String cron,
        String timezone
    ) {
        
        public boolean isEnabled() {
            return enabled;
        }
        
        public String getCron() {
            return cron;
        }
        
        public ZoneId getTimezone() {
            return ZoneId.of(timezone);
        }
        
        public void validate() {
            if (enabled) {
                if (cron == null || cron.isBlank()) {
                    throw new IllegalStateException("Monthly billing cron expression is required when enabled");
                }
                if (timezone == null || timezone.isBlank()) {
                    throw new IllegalStateException("Monthly billing timezone is required when enabled");
                }
                
                try {
                    ZoneId.of(timezone);
                } catch (Exception e) {
                    throw new IllegalStateException("Invalid timezone for monthly billing: " + timezone, e);
                }
            }
        }
    }
    
    /**
     * Configuration for dunning process schedule
     */
    public record DunningProcessSchedule(
        boolean enabled,
        String cron,
        String timezone,
        int threadPoolSize
    ) {
        
        public boolean isEnabled() {
            return enabled;
        }
        
        public String getCron() {
            return cron;
        }
        
        public ZoneId getTimezone() {
            return ZoneId.of(timezone);
        }
        
        public int getThreadPoolSize() {
            return threadPoolSize;
        }
        
        public void validate() {
            if (enabled) {
                if (cron == null || cron.isBlank()) {
                    throw new IllegalStateException("Dunning process cron expression is required when enabled");
                }
                if (timezone == null || timezone.isBlank()) {
                    throw new IllegalStateException("Dunning process timezone is required when enabled");
                }
                if (threadPoolSize <= 0) {
                    throw new IllegalStateException("Dunning process thread pool size must be positive");
                }
                
                try {
                    ZoneId.of(timezone);
                } catch (Exception e) {
                    throw new IllegalStateException("Invalid timezone for dunning process: " + timezone, e);
                }
                
                if (threadPoolSize > 20) {
                    throw new IllegalStateException("Dunning process thread pool size should not exceed 20");
                }
            }
        }
    }
    
    /**
     * Gets monthly billing schedule configuration
     */
    public MonthlyBillingSchedule getMonthlyBilling() {
        return monthlyBilling;
    }
    
    /**
     * Gets dunning process schedule configuration
     */
    public DunningProcessSchedule getDunningProcess() {
        return dunningProcess;
    }
    
    /**
     * Validates scheduling configuration
     */
    public void validate() {
        if (monthlyBilling != null) {
            monthlyBilling.validate();
        }
        if (dunningProcess != null) {
            dunningProcess.validate();
        }
    }
}

