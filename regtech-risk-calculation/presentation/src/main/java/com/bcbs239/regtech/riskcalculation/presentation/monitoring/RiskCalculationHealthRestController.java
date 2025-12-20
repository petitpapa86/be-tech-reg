package com.bcbs239.regtech.riskcalculation.presentation.monitoring;

import com.bcbs239.regtech.riskcalculation.presentation.monitoring.RiskCalculationHealthChecker.HealthCheckResult;
import com.bcbs239.regtech.riskcalculation.presentation.monitoring.RiskCalculationHealthChecker.ModuleHealthResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Spring MVC (annotation-based) health endpoints for the risk calculation module.
 *
 * These endpoints intentionally mirror the documented URLs:
 * - GET /api/v1/risk-calculation/health
 * - GET /api/v1/risk-calculation/health/database
 * - GET /api/v1/risk-calculation/health/file-storage
 * - GET /api/v1/risk-calculation/health/currency-conversion
 *
 * The module already has functional RouterFunction routes, but this controller ensures
 * the health checks remain reachable even if functional routing isn't registered.
 */
@RestController
@RequestMapping("/api/v1/risk-calculation/healthv1")
public class RiskCalculationHealthRestController {

    private final RiskCalculationHealthChecker healthChecker;

    public RiskCalculationHealthRestController(RiskCalculationHealthChecker healthChecker) {
        this.healthChecker = healthChecker;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getModuleHealth() {
        ModuleHealthResult healthResult = healthChecker.checkModuleHealth();
        int httpStatus = healthResult.isHealthy() ? 200 : 503;
        return ResponseEntity.status(httpStatus).body(healthResult.toResponseMap());
    }

    @GetMapping("/database")
    public ResponseEntity<Map<String, Object>> getDatabaseHealth() {
        return componentResponse("database", healthChecker.checkDatabaseHealth());
    }

    @GetMapping("/file-storage")
    public ResponseEntity<Map<String, Object>> getFileStorageHealth() {
        return componentResponse("file-storage", healthChecker.checkFileStorageHealth());
    }

    @GetMapping("/currency-conversion")
    public ResponseEntity<Map<String, Object>> getCurrencyConversionHealth() {
        return componentResponse("currency-conversion", healthChecker.checkCurrencyApiHealth());
    }

    private static ResponseEntity<Map<String, Object>> componentResponse(String componentName, HealthCheckResult result) {
        Map<String, Object> response = Map.of(
            "component", componentName,
            "status", result.status(),
            "message", result.message(),
            "timestamp", Instant.now().toString(),
            "details", result.details()
        );

        int httpStatus = result.isHealthy() ? 200 : 503;
        return ResponseEntity.status(httpStatus).body(response);
    }
}
