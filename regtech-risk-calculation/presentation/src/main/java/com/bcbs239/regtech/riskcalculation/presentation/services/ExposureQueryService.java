package com.bcbs239.regtech.riskcalculation.presentation.services;

import com.bcbs239.regtech.riskcalculation.domain.classification.ClassifiedExposure;
import com.bcbs239.regtech.riskcalculation.domain.classification.EconomicSector;
import com.bcbs239.regtech.riskcalculation.domain.classification.ExposureClassifier;
import com.bcbs239.regtech.riskcalculation.domain.exposure.ExposureRecording;
import com.bcbs239.regtech.riskcalculation.domain.persistence.ExposureRepository;
import com.bcbs239.regtech.riskcalculation.domain.persistence.MitigationRepository;
import com.bcbs239.regtech.riskcalculation.domain.protection.Mitigation;
import com.bcbs239.regtech.riskcalculation.domain.protection.ProtectedExposure;
import com.bcbs239.regtech.riskcalculation.domain.protection.RawMitigationData;
import com.bcbs239.regtech.riskcalculation.domain.shared.enums.GeographicRegion;
import com.bcbs239.regtech.riskcalculation.domain.valuation.EurAmount;
import com.bcbs239.regtech.riskcalculation.domain.valuation.ExchangeRateProvider;
import com.bcbs239.regtech.riskcalculation.presentation.dto.ClassifiedExposureDTO;
import com.bcbs239.regtech.riskcalculation.presentation.dto.PagedResponse;
import com.bcbs239.regtech.riskcalculation.presentation.dto.ProtectedExposureDTO;
import com.bcbs239.regtech.riskcalculation.presentation.mappers.ExposureMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Query service for retrieving exposure data.
 * Provides paginated read-only access to classified and protected exposures.
 * 
 * Requirements: 2.1, 2.3, 2.4
 */
@Service
@Transactional(readOnly = true)
public class ExposureQueryService {
    
    private final ExposureRepository exposureRepository;
    private final MitigationRepository mitigationRepository;
    private final ExposureMapper exposureMapper;
    private final ExchangeRateProvider exchangeRateProvider;
    private final ExposureClassifier exposureClassifier;
    
    public ExposureQueryService(
        ExposureRepository exposureRepository,
        MitigationRepository mitigationRepository,
        ExposureMapper exposureMapper,
        ExchangeRateProvider exchangeRateProvider
    ) {
        this.exposureRepository = exposureRepository;
        this.mitigationRepository = mitigationRepository;
        this.exposureMapper = exposureMapper;
        this.exchangeRateProvider = exchangeRateProvider;
        this.exposureClassifier = new ExposureClassifier();
    }
    
    /**
     * Retrieves classified exposures for a batch with pagination.
     * 
     * @param batchId the batch identifier
     * @param page the page number (0-indexed)
     * @param size the page size
     * @return paged response containing classified exposure DTOs
     */
    public PagedResponse<ClassifiedExposureDTO> getClassifiedExposures(
        String batchId,
        int page,
        int size
    ) {
        List<ExposureRecording> allExposures = exposureRepository.findByBatchId(batchId);
        
        // Convert to classified exposures
        List<ClassifiedExposure> classifiedExposures = allExposures.stream()
            .map(this::toClassifiedExposure)
            .collect(Collectors.toList());
        
        // Apply pagination
        int start = page * size;
        int end = Math.min(start + size, classifiedExposures.size());
        
        List<ClassifiedExposure> pagedExposures = classifiedExposures.subList(
            Math.min(start, classifiedExposures.size()),
            end
        );
        
        // Convert to DTOs
        List<ClassifiedExposureDTO> dtos = pagedExposures.stream()
            .map(exposureMapper::toClassifiedDTO)
            .collect(Collectors.toList());
        
        return PagedResponse.<ClassifiedExposureDTO>builder()
            .content(dtos)
            .page(page)
            .size(size)
            .totalElements((long) classifiedExposures.size())
            .totalPages((int) Math.ceil((double) classifiedExposures.size() / size))
            .build();
    }
    
    /**
     * Retrieves classified exposures filtered by economic sector with pagination.
     * 
     * @param batchId the batch identifier
     * @param sector the economic sector to filter by
     * @param page the page number (0-indexed)
     * @param size the page size
     * @return paged response containing filtered classified exposure DTOs
     */
    public PagedResponse<ClassifiedExposureDTO> getClassifiedExposuresBySector(
        String batchId,
        String sector,
        int page,
        int size
    ) {
        List<ExposureRecording> allExposures = exposureRepository.findByBatchId(batchId);
        
        // Convert to classified exposures and filter by sector
        EconomicSector targetSector = EconomicSector.valueOf(sector.toUpperCase());
        List<ClassifiedExposure> classifiedExposures = allExposures.stream()
            .map(this::toClassifiedExposure)
            .filter(exposure -> exposure.sector() == targetSector)
            .collect(Collectors.toList());
        
        // Apply pagination
        int start = page * size;
        int end = Math.min(start + size, classifiedExposures.size());
        
        List<ClassifiedExposure> pagedExposures = classifiedExposures.subList(
            Math.min(start, classifiedExposures.size()),
            end
        );
        
        // Convert to DTOs
        List<ClassifiedExposureDTO> dtos = pagedExposures.stream()
            .map(exposureMapper::toClassifiedDTO)
            .collect(Collectors.toList());
        
        return PagedResponse.<ClassifiedExposureDTO>builder()
            .content(dtos)
            .page(page)
            .size(size)
            .totalElements((long) classifiedExposures.size())
            .totalPages((int) Math.ceil((double) classifiedExposures.size() / size))
            .build();
    }
    
    /**
     * Retrieves protected exposures for a batch with pagination.
     * 
     * @param batchId the batch identifier
     * @param page the page number (0-indexed)
     * @param size the page size
     * @return paged response containing protected exposure DTOs
     */
    public PagedResponse<ProtectedExposureDTO> getProtectedExposures(
        String batchId,
        int page,
        int size
    ) {
        List<ExposureRecording> allExposures = exposureRepository.findByBatchId(batchId);
        
        // Convert to protected exposures
        List<ProtectedExposure> protectedExposures = allExposures.stream()
            .map(exposure -> toProtectedExposure(exposure, batchId))
            .collect(Collectors.toList());
        
        // Apply pagination
        int start = page * size;
        int end = Math.min(start + size, protectedExposures.size());
        
        List<ProtectedExposure> pagedExposures = protectedExposures.subList(
            Math.min(start, protectedExposures.size()),
            end
        );
        
        // Convert to DTOs
        List<ProtectedExposureDTO> dtos = pagedExposures.stream()
            .map(exposureMapper::toProtectedDTO)
            .collect(Collectors.toList());
        
        return PagedResponse.<ProtectedExposureDTO>builder()
            .content(dtos)
            .page(page)
            .size(size)
            .totalElements((long) protectedExposures.size())
            .totalPages((int) Math.ceil((double) protectedExposures.size() / size))
            .build();
    }
    
    /**
     * Converts an ExposureRecording to a ClassifiedExposure.
     * 
     * @param exposure the exposure recording
     * @return the classified exposure
     */
    private ClassifiedExposure toClassifiedExposure(ExposureRecording exposure) {
        // Convert to EUR
        EurAmount eurAmount = convertToEur(exposure);
        
        // Classify by region and sector
        GeographicRegion region = exposureClassifier.classifyRegion(
            exposure.classification().countryCode()
        );
        EconomicSector sector = exposureClassifier.classifySector(
            exposure.classification().productType()
        );
        
        return ClassifiedExposure.of(
            exposure.id(),
            eurAmount,
            region,
            sector
        );
    }
    
    /**
     * Converts an ExposureRecording to a ProtectedExposure.
     * 
     * @param exposure the exposure recording
     * @param batchId the batch identifier
     * @return the protected exposure
     */
    private ProtectedExposure toProtectedExposure(ExposureRecording exposure, String batchId) {
        // Convert gross exposure to EUR
        EurAmount grossExposure = convertToEur(exposure);
        
        // Retrieve mitigations for this exposure
        List<RawMitigationData> rawMitigations = mitigationRepository.findByExposureId(exposure.id());
        
        // Convert raw mitigations to Mitigation entities
        List<Mitigation> mitigations = rawMitigations.stream()
            .map(raw -> Mitigation.fromRawData(raw, exchangeRateProvider))
            .collect(Collectors.toList());
        
        // Calculate protected exposure
        return ProtectedExposure.calculate(
            exposure.id(),
            grossExposure,
            mitigations
        );
    }
    
    /**
     * Converts an exposure amount to EUR.
     * 
     * @param exposure the exposure recording
     * @return the EUR amount
     */
    private EurAmount convertToEur(ExposureRecording exposure) {
        String currency = exposure.exposureAmount().currencyCode();
        BigDecimal amount = exposure.exposureAmount().amount();
        
        if ("EUR".equals(currency)) {
            return EurAmount.of(amount);
        }
        
        try {
            var rate = exchangeRateProvider.getRate(currency, "EUR");
            BigDecimal eurValue = amount.multiply(rate.rate());
            return EurAmount.of(eurValue);
        } catch (Exception e) {
            // If conversion fails, return zero
            return EurAmount.zero();
        }
    }
}
