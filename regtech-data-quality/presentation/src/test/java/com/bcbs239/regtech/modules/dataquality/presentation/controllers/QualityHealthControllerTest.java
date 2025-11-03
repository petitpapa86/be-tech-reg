package com.bcbs239.regtech.modules.dataquality.presentation.controllers;

import com.bcbs239.regtech.modules.dataquality.application.services.QualityValidationEngine;
import com.bcbs239.regtech.modules.dataquality.application.services.S3StorageService;
import com.bcbs239.regtech.modules.dataquality.domain.report.IQualityReportRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for QualityHealthController.
 * Tests the functional endpoint mapping and basic controller behavior.
 */
@ExtendWith(MockitoExtension.class)
class QualityHealthControllerTest {
    
    @Mock
    private IQualityReportRepository qualityReportRepository;
    
    @Mock
    private S3StorageService s3StorageService;
    
    @Mock
    private QualityValidationEngine validationEngine;
    
    private QualityHealthController controller;
    
    @BeforeEach
    void setUp() {
        controller = new QualityHealthController(
            qualityReportRepository, 
            s3StorageService, 
            validationEngine
        );
    }
    
    @Test
    void shouldMapEndpoints() {
        // When
        RouterFunction<ServerResponse> routes = controller.mapEndpoints();
        
        // Then
        assertThat(routes).isNotNull();
    }
    
    @Test
    void shouldCreateControllerWithDependencies() {
        // Then
        assertThat(controller).isNotNull();
    }
}