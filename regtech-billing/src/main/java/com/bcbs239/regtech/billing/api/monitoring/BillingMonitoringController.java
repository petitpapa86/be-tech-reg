package com.bcbs239.regtech.billing.api.monitoring;

import com.bcbs239.regtech.billing.infrastructure.entities.SagaAuditLogEntity;
import com.bcbs239.regtech.billing.infrastructure.monitoring.BillingPerformanceMetricsService;
import com.bcbs239.regtech.billing.infrastructure.monitoring.BillingSagaAuditService;
import com.bcbs239.regtech.core.shared.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller for billing monitoring and audit endpoints.
 * Provides access to audit trails, performance metrics, and compliance reports.
 */
@RestController
@RequestMapping("/api/v1/billing/monitoring")
public class BillingMonitoringController {

    private final BillingSagaAuditService auditService;
    private final BillingPerformanceMetricsService metricsService;

    public BillingMonitoringController(BillingSagaAuditService auditService,
                                     BillingPerformanceMetricsService metricsService) {
        this.auditService = auditService;
        this.metricsService = metricsService;
    }

    /**
     * Get audit trail for a specific saga
     */
    @GetMapping("/audit/saga/{sagaId}")
    public ResponseEntity<ApiResponse<List<SagaAuditLogEntity>>> getSagaAuditTrail(
            @PathVariable String sagaId) {
        
        List<SagaAuditLogEntity> auditTrail = auditService.getSagaAuditTrail(sagaId);
        
        return ResponseEntity.ok(ApiResponse.success(auditTrail));
    }

    /**
     * Get audit trail for a specific user
     */
    @GetMapping("/audit/user/{userId}")
    public ResponseEntity<ApiResponse<List<SagaAuditLogEntity>>> getUserAuditTrail(
            @PathVariable String userId) {
        
        List<SagaAuditLogEntity> auditTrail = auditService.getUserAuditTrail(userId);
        
        return ResponseEntity.ok(ApiResponse.success(auditTrail));
    }

    /**
     * Get audit trail for a specific billing account
     */
    @GetMapping("/audit/billing-account/{billingAccountId}")
    public ResponseEntity<ApiResponse<List<SagaAuditLogEntity>>> getBillingAccountAuditTrail(
            @PathVariable String billingAccountId) {
        
        List<SagaAuditLogEntity> auditTrail = auditService.getBillingAccountAuditTrail(billingAccountId);
        
        return ResponseEntity.ok(ApiResponse.success(auditTrail));
    }

    /**
     * Get recent saga events for monitoring
     */
    @GetMapping("/audit/recent")
    public ResponseEntity<ApiResponse<List<SagaAuditLogEntity>>> getRecentSagaEvents(
            @RequestParam(defaultValue = "monthly-billing") String sagaType,
            @RequestParam(defaultValue = "24") int hours) {
        
        List<SagaAuditLogEntity> recentEvents = auditService.getRecentSagaEvents(sagaType, hours);
        
        return ResponseEntity.ok(ApiResponse.success(recentEvents));
    }

    /**
     * Get saga statistics for monitoring dashboard
     */
    @GetMapping("/audit/statistics")
    public ResponseEntity<ApiResponse<List<Object[]>>> getSagaStatistics(
            @RequestParam(defaultValue = "monthly-billing") String sagaType,
            @RequestParam(defaultValue = "7") int days) {
        
        List<Object[]> statistics = auditService.getSagaStatistics(sagaType, days);
        
        return ResponseEntity.ok(ApiResponse.success(statistics));
    }

    /**
     * Generate compliance report for a specific period
     */
    @GetMapping("/audit/compliance-report")
    public ResponseEntity<ApiResponse<Map<String, Object>>> generateComplianceReport(
            @RequestParam(defaultValue = "monthly-billing") String sagaType,
            @RequestParam(defaultValue = "30") int days) {
        
        Map<String, Object> report = auditService.generateComplianceReport(sagaType, days);
        
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    /**
     * Get performance metrics summary
     */
    @GetMapping("/metrics/summary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPerformanceMetricsSummary() {
        
        Map<String, Object> summary = metricsService.getPerformanceMetricsSummary();
        
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    /**
     * Get health status based on performance metrics
     */
    @GetMapping("/metrics/health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getHealthStatus() {
        
        Map<String, Object> health = metricsService.getHealthStatus();
        
        return ResponseEntity.ok(ApiResponse.success(health));
    }

    /**
     * Get detailed performance metrics for a specific operation type
     */
    @GetMapping("/metrics/operation/{operationType}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getOperationMetrics(
            @PathVariable String operationType) {
        
        // This would typically return detailed metrics for the specific operation
        // For now, return the general summary with a note about the operation type
        Map<String, Object> summary = metricsService.getPerformanceMetricsSummary();
        summary.put("requestedOperationType", operationType);
        summary.put("note", "Detailed operation-specific metrics would be implemented here");
        
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    /**
     * Reset performance metrics (for testing/maintenance)
     */
    @PostMapping("/metrics/reset")
    public ResponseEntity<ApiResponse<String>> resetMetrics() {
        
        metricsService.resetMetrics();
        
        return ResponseEntity.ok(ApiResponse.success("Performance metrics reset successfully"));
    }

    /**
     * Get billing calculation audit trail for a specific saga
     */
    @GetMapping("/audit/billing-calculations/{sagaId}")
    public ResponseEntity<ApiResponse<List<SagaAuditLogEntity>>> getBillingCalculationAudit(
            @PathVariable String sagaId) {
        
        List<SagaAuditLogEntity> auditTrail = auditService.getSagaAuditTrail(sagaId)
            .stream()
            .filter(log -> "BILLING_CALCULATION".equals(log.getEventType()) || 
                          "INVOICE_AMOUNT_DETERMINATION".equals(log.getEventType()))
            .toList();
        
        return ResponseEntity.ok(ApiResponse.success(auditTrail));
    }

    /**
     * Get saga performance metrics for a specific time period
     */
    @GetMapping("/metrics/saga-performance")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSagaPerformanceMetrics(
            @RequestParam(defaultValue = "monthly-billing") String sagaType,
            @RequestParam(defaultValue = "24") int hours) {
        
        Map<String, Object> performanceData = new HashMap<>();
        
        // Get recent saga events for performance analysis
        List<SagaAuditLogEntity> recentEvents = auditService.getRecentSagaEvents(sagaType, hours);
        
        // Calculate performance statistics
        long totalExecutions = recentEvents.stream()
            .filter(event -> event.getEventType().contains("SAGA_COMPLETED"))
            .count();
        
        long successfulExecutions = recentEvents.stream()
            .filter(event -> "SAGA_COMPLETED_SUCCESS".equals(event.getEventType()))
            .count();
        
        long failedExecutions = recentEvents.stream()
            .filter(event -> "SAGA_COMPLETED_FAILURE".equals(event.getEventType()))
            .count();
        
        performanceData.put("sagaType", sagaType);
        performanceData.put("timePeriodHours", hours);
        performanceData.put("totalExecutions", totalExecutions);
        performanceData.put("successfulExecutions", successfulExecutions);
        performanceData.put("failedExecutions", failedExecutions);
        
        if (totalExecutions > 0) {
            double successRate = (double) successfulExecutions / totalExecutions * 100;
            performanceData.put("successRate", String.format("%.2f%%", successRate));
        } else {
            performanceData.put("successRate", "N/A");
        }
        
        // Add current performance metrics
        Map<String, Object> currentMetrics = metricsService.getPerformanceMetricsSummary();
        performanceData.put("currentMetrics", currentMetrics);
        
        return ResponseEntity.ok(ApiResponse.success(performanceData));
    }

    /**
     * Get detailed audit trail with filtering options
     */
    @GetMapping("/audit/detailed")
    public ResponseEntity<ApiResponse<List<SagaAuditLogEntity>>> getDetailedAuditTrail(
            @RequestParam(required = false) String sagaId,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String billingAccountId,
            @RequestParam(required = false) String eventType,
            @RequestParam(defaultValue = "24") int hours) {
        
        List<SagaAuditLogEntity> auditTrail;
        
        if (sagaId != null) {
            auditTrail = auditService.getSagaAuditTrail(sagaId);
        } else if (userId != null) {
            auditTrail = auditService.getUserAuditTrail(userId);
        } else if (billingAccountId != null) {
            auditTrail = auditService.getBillingAccountAuditTrail(billingAccountId);
        } else {
            auditTrail = auditService.getRecentSagaEvents("monthly-billing", hours);
        }
        
        // Filter by event type if specified
        if (eventType != null && !eventType.isEmpty()) {
            auditTrail = auditTrail.stream()
                .filter(log -> eventType.equals(log.getEventType()))
                .toList();
        }
        
        return ResponseEntity.ok(ApiResponse.success(auditTrail));
    }

    /**
     * Get compliance report with detailed breakdown
     */
    @GetMapping("/audit/compliance-report/detailed")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDetailedComplianceReport(
            @RequestParam(defaultValue = "monthly-billing") String sagaType,
            @RequestParam(defaultValue = "30") int days) {
        
        Map<String, Object> detailedReport = auditService.generateComplianceReport(sagaType, days);
        
        // Add additional compliance-specific information
        List<SagaAuditLogEntity> recentEvents = auditService.getRecentSagaEvents(sagaType, days * 24);
        
        // Count different types of audit events
        Map<String, Long> eventTypeCounts = recentEvents.stream()
            .collect(java.util.stream.Collectors.groupingBy(
                SagaAuditLogEntity::getEventType,
                java.util.stream.Collectors.counting()
            ));
        
        detailedReport.put("eventTypeCounts", eventTypeCounts);
        
        // Add billing calculation audit counts
        long billingCalculationEvents = recentEvents.stream()
            .filter(event -> "BILLING_CALCULATION".equals(event.getEventType()))
            .count();
        
        long invoiceAmountEvents = recentEvents.stream()
            .filter(event -> "INVOICE_AMOUNT_DETERMINATION".equals(event.getEventType()))
            .count();
        
        long statusChangeEvents = recentEvents.stream()
            .filter(event -> "BILLING_ACCOUNT_STATUS_CHANGE".equals(event.getEventType()))
            .count();
        
        detailedReport.put("billingCalculationAudits", billingCalculationEvents);
        detailedReport.put("invoiceAmountAudits", invoiceAmountEvents);
        detailedReport.put("statusChangeAudits", statusChangeEvents);
        
        // Calculate audit coverage percentage
        long totalSagaCompletions = recentEvents.stream()
            .filter(event -> event.getEventType().contains("SAGA_COMPLETED"))
            .count();
        
        if (totalSagaCompletions > 0) {
            double auditCoverage = (double) billingCalculationEvents / totalSagaCompletions * 100;
            detailedReport.put("auditCoverage", String.format("%.2f%%", auditCoverage));
        } else {
            detailedReport.put("auditCoverage", "N/A");
        }
        
        return ResponseEntity.ok(ApiResponse.success(detailedReport));
    }
}