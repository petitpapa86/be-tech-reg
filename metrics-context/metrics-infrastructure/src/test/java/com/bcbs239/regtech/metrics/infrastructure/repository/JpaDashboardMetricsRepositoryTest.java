package com.bcbs239.regtech.metrics.infrastructure.repository;

import com.bcbs239.regtech.metrics.domain.BankId;
import com.bcbs239.regtech.metrics.infrastructure.entity.DashboardMetricsEntity;
import com.bcbs239.regtech.metrics.infrastructure.entity.DashboardMetricsKey;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class JpaDashboardMetricsRepositoryTest {

    @Test
    public void addSamplesAndGet_mergesAndReturnsMedian() throws Exception {
        // Arrange
        SpringDataDashboardMetricsRepository mockRepo = mock(SpringDataDashboardMetricsRepository.class);
        JpaDashboardMetricsRepository repository = new JpaDashboardMetricsRepository(mockRepo);

        LocalDate periodStart = LocalDate.of(2026, 1, 1);
        String bank = "bank-1";

        DashboardMetricsEntity entity = new DashboardMetricsEntity();
        entity.setKey(new DashboardMetricsKey(bank, periodStart));

        when(mockRepo.findByKeyBankIdAndKeyPeriodStart(bank, periodStart))
                .thenReturn(Optional.of(entity));
        when(mockRepo.save(any(DashboardMetricsEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        double dqSample = 0.6;
        double cSample = 0.8;

        // Act
        var result = repository.addSamplesAndGet(BankId.of(bank), periodStart, dqSample, cSample);

        // Assert: medians should equal the single sample values
        assertNotNull(result);
        assertEquals(dqSample, result.getDataQualityScore(), 1e-9);
        assertEquals(cSample, result.getCompletenessScore(), 1e-9);

        // Verify digest bytes saved on the entity
        verify(mockRepo).save(any(DashboardMetricsEntity.class));
        // capture saved entity to inspect digest fields
        var captor = org.mockito.ArgumentCaptor.forClass(DashboardMetricsEntity.class);
        verify(mockRepo).save(captor.capture());
        DashboardMetricsEntity saved = captor.getValue();

        assertNotNull(saved.getDataQualityDigest(), "dataQualityDigest should be persisted");
        assertTrue(saved.getDataQualityDigest().length > 0, "dataQualityDigest should not be empty");

        assertNotNull(saved.getCompletenessDigest(), "completenessDigest should be persisted");
        assertTrue(saved.getCompletenessDigest().length > 0, "completenessDigest should not be empty");
    }
}
