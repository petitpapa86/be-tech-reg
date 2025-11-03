package com.bcbs239.regtech.modules.dataquality.presentation.monitoring;

import com.bcbs239.regtech.modules.dataquality.application.services.QualityValidationEngine;
import com.bcbs239.regtech.modules.dataquality.application.services.S3StorageService;
import com.bcbs239.regtech.modules.dataquality.domain.report.IQualityReportRepository;
import com.bcbs239.regtech.modules.dataquality.presentation.monitoring.QualityHealthChecker.HealthCheckResult;
import com.bcbs239.regtech.modules.dataquality.presentation.monitoring.QualityHealthChecker.ModuleHealthResult;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for QualityHealthChecker.
 */
@ExtendWith(MockitoExtension.class)
class QualityHealthCheckerTest {
    
    @Mock
    private IQualityReportRepository qualityReportRepository;
    
    @Mock
    private S3StorageService s3StorageService;
    
    @Mock
    private QualityValidationEngine validationEngine;
    
    private QualityHealthChecker healthChecker;
    
    @BeforeEach
    void setUp() {
        healthChecker = new QualityHealthChecker(
            qualityReportRepository,
            s3StorageService,
            validationEngine
        );
    }
    
    @Test
    void shouldCheckDatabaseHealth() {
        // When
        HealthCheckResult result = healthChecker.checkDatabaseHealth();
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo("UP");
        assertThat(result.isHealthy()).isTrue();
    }
    
    @Test
    void shouldCheckS3Health() {
        // When
        HealthCheckResult result = healthChecker.checkS3Health();
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo("UP");
        assertThat(result.isHealthy()).isTrue();
    }
    
    @Test
    void shouldCheckValidationEngineHealth() {
        // When
        HealthCheckResult result = healthChecker.checkValidationEngineHealth();
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo("UP");
        assertThat(result.isHealthy()).isTrue();
    }
    
    @Test
    void shouldCheckModuleHealth() {
        // When
        ModuleHealthResult result = healthChecker.checkModuleHealth();
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.overallStatus()).isEqualTo("UP");
        assertThat(result.isHealthy()).isTrue();
        assertThat(result.databaseHealth()).isNotNull();
        assertThat(result.s3Health()).isNotNull();
        assertThat(result.validationHealth()).isNotNull();
    }
    
    @Test
    void shouldReturnDownWhenRepositoryIsNull() {
        // Given
        QualityHealthChecker checkerWithNullRepo = new QualityHealthChecker(
            null, s3StorageService, validationEngine
        );
        
        // When
        HealthCheckResult result = checkerWithNullRepo.checkDatabaseHealth();
        
        // Then
        assertThat(result.status()).isEqualTo("DOWN");
        assertThat(result.isHealthy()).isFalse();
    }
}