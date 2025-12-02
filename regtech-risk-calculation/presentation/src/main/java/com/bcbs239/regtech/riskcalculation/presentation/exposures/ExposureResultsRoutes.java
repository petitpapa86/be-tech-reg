package com.bcbs239.regtech.riskcalculation.presentation.exposures;

/**
 * NOTE: This class is no longer needed as ExposureResultsController uses @RestController
 * with @GetMapping annotations, which automatically handles routing through Spring MVC.
 * 
 * The controller is already configured with:
 * - @RequestMapping("/api/v1/risk-calculation/exposures")
 * - Individual @GetMapping annotations for each endpoint
 * 
 * Security and permissions should be configured through Spring Security configuration
 * rather than functional routing.
 * 
 * This file is kept for reference but can be deleted.
 * 
 * Requirements: 6.1, 6.2, 6.3
 */
@Deprecated
public class ExposureResultsRoutes {
    // This class is deprecated - routing is handled by @RestController annotations
}
