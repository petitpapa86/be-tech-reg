package com.bcbs239.regtech.billing.api.scheduling;

import com.bcbs239.regtech.billing.infrastructure.scheduling.DunningProcessScheduler;
import com.bcbs239.regtech.billing.infrastructure.scheduling.MonthlyBillingScheduler;
import com.bcbs239.regtech.core.shared.ApiResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for manually triggering billing scheduled jobs.
 * Provides endpoints for operations teams to trigger billing processes manually.
 */
@RestController
@RequestMapping("/api/v1/billing/scheduling")
@ConditionalOnBean({MonthlyBillingScheduler.class, DunningProcessScheduler.class})
public class BillingSchedulingController {

    private final MonthlyBillingScheduler monthlyBillingScheduler;
    private final DunningProcessScheduler dunningProcessScheduler;

    public BillingSchedulingController(
            MonthlyBillingScheduler monthlyBillingScheduler,
            DunningProcessScheduler dunningProcessScheduler) {
        this.monthlyBillingScheduler = monthlyBillingScheduler;
        this.dunningProcessScheduler = dunningProcessScheduler;
    }

    /**
     * Manually trigger monthly billing for the current month.
     * Useful for testing or when scheduled job fails.
     */
    @PostMapping("/monthly-billing/trigger-current")
    @PreAuthorize("hasRole('BILLING_ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> triggerCurrentMonthBilling() {
        
        MonthlyBillingScheduler.MonthlyBillingResult result = 
            monthlyBillingScheduler.triggerCurrentMonthBilling();
        
        Map<String, Object> response = createBillingResultResponse(result);
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Manually trigger monthly billing for the previous month.
     * Most common manual trigger scenario.
     */
    @PostMapping("/monthly-billing/trigger-previous")
    @PreAuthorize("hasRole('BILLING_ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> triggerPreviousMonthBilling() {
        
        MonthlyBillingScheduler.MonthlyBillingResult result = 
            monthlyBillingScheduler.triggerPreviousMonthBilling();
        
        Map<String, Object> response = createBillingResultResponse(result);
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Manually trigger monthly billing for a specific month.
     * Allows operations team to reprocess billing for any month.
     */
    @PostMapping("/monthly-billing/trigger/{year}/{month}")
    @PreAuthorize("hasRole('BILLING_ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> triggerMonthlyBilling(
            @PathVariable int year,
            @PathVariable int month) {
        
        try {
            YearMonth billingMonth = YearMonth.of(year, month);
            MonthlyBillingScheduler.MonthlyBillingResult result = 
                monthlyBillingScheduler.triggerMonthlyBilling(billingMonth);
            
            Map<String, Object> response = createBillingResultResponse(result);
            
            return ResponseEntity.ok(ApiResponse.success(response));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("INVALID_DATE", "Invalid year/month: " + year + "/" + month));
        }
    }

    /**
     * Manually trigger dunning process.
     * Processes overdue invoices and executes dunning actions.
     */
    @PostMapping("/dunning-process/trigger")
    @PreAuthorize("hasRole('BILLING_ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> triggerDunningProcess() {
        
        DunningProcessScheduler.DunningProcessResult result = 
            dunningProcessScheduler.triggerDunningProcess();
        
        Map<String, Object> response = createDunningResultResponse(result);
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get dunning process statistics.
     * Shows current state of dunning cases for monitoring.
     */
    @GetMapping("/dunning-process/statistics")
    @PreAuthorize("hasRole('BILLING_ADMIN') or hasRole('BILLING_VIEWER')")
    public ResponseEntity<ApiResponse<DunningProcessScheduler.DunningStatistics>> getDunningStatistics() {
        
        DunningProcessScheduler.DunningStatistics stats = 
            dunningProcessScheduler.getDunningStatistics();
        
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    /**
     * Resolve dunning cases for a specific invoice.
     * Used when payment is received outside normal processing.
     */
    @PostMapping("/dunning-process/resolve/{invoiceId}")
    @PreAuthorize("hasRole('BILLING_ADMIN')")
    public ResponseEntity<ApiResponse<String>> resolveDunningCase(
            @PathVariable String invoiceId,
            @RequestParam(defaultValue = "Manual resolution") String reason) {
        
        try {
            dunningProcessScheduler.resolveDunningCasesForInvoice(invoiceId, reason);
            
            return ResponseEntity.ok(ApiResponse.success(
                "Dunning case resolved for invoice: " + invoiceId));
                
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("RESOLUTION_FAILED", 
                    "Failed to resolve dunning case: " + e.getMessage()));
        }
    }

    /**
     * Get scheduling status and configuration.
     * Shows which schedulers are enabled and their configuration.
     */
    @GetMapping("/status")
    @PreAuthorize("hasRole('BILLING_ADMIN') or hasRole('BILLING_VIEWER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSchedulingStatus() {
        
        Map<String, Object> status = new HashMap<>();
        
        // Monthly billing scheduler status
        Map<String, Object> monthlyBillingStatus = new HashMap<>();
        monthlyBillingStatus.put("enabled", monthlyBillingScheduler != null);
        monthlyBillingStatus.put("cron", "0 0 0 1 * ?");
        monthlyBillingStatus.put("timezone", "UTC");
        monthlyBillingStatus.put("description", "Runs on first day of each month at midnight UTC");
        status.put("monthlyBilling", monthlyBillingStatus);
        
        // Dunning process scheduler status
        Map<String, Object> dunningProcessStatus = new HashMap<>();
        dunningProcessStatus.put("enabled", dunningProcessScheduler != null);
        dunningProcessStatus.put("cron", "0 0 9 * * ?");
        dunningProcessStatus.put("timezone", "UTC");
        dunningProcessStatus.put("description", "Runs daily at 9 AM UTC");
        status.put("dunningProcess", dunningProcessStatus);
        
        status.put("timestamp", java.time.Instant.now().toString());
        
        return ResponseEntity.ok(ApiResponse.success(status));
    }

    // Helper methods

    private Map<String, Object> createBillingResultResponse(MonthlyBillingScheduler.MonthlyBillingResult result) {
        Map<String, Object> response = new HashMap<>();
        response.put("billingPeriod", result.billingPeriod().getPeriodId());
        response.put("totalSubscriptions", result.totalSubscriptions());
        response.put("successfulSagas", result.successfulSagas());
        response.put("failedSagas", result.failedSagas());
        response.put("successRate", String.format("%.2f%%", result.getSuccessRate() * 100));
        response.put("hasFailures", result.hasFailures());
        response.put("timestamp", java.time.Instant.now().toString());
        return response;
    }

    private Map<String, Object> createDunningResultResponse(DunningProcessScheduler.DunningProcessResult result) {
        Map<String, Object> response = new HashMap<>();
        response.put("newCasesCreated", result.newCasesCreated());
        response.put("actionsExecuted", result.actionsExecuted());
        response.put("failures", result.failures());
        response.put("hasFailures", result.hasFailures());
        response.put("timestamp", java.time.Instant.now().toString());
        return response;
    }
}