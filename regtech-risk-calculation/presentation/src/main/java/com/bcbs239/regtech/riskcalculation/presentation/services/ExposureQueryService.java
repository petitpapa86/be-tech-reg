package com.bcbs239.regtech.riskcalculation.presentation.services;

import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.riskcalculation.application.calculation.RiskCalculationResult;
import com.bcbs239.regtech.riskcalculation.application.storage.ICalculationResultsStorageService;
import com.bcbs239.regtech.riskcalculation.domain.classification.ClassifiedExposure;
import com.bcbs239.regtech.riskcalculation.domain.classification.EconomicSector;
import com.bcbs239.regtech.riskcalculation.domain.protection.ProtectedExposure;
import com.bcbs239.regtech.riskcalculation.presentation.dto.ClassifiedExposureDTO;
import com.bcbs239.regtech.riskcalculation.presentation.dto.PagedResponse;
import com.bcbs239.regtech.riskcalculation.presentation.dto.ProtectedExposureDTO;
import com.bcbs239.regtech.riskcalculation.presentation.exceptions.BatchNotFoundException;
import com.bcbs239.regtech.riskcalculation.presentation.exceptions.CalculationNotCompletedException;
import com.bcbs239.regtech.riskcalculation.presentation.mappers.ExposureMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Query service for retrieving exposure data from JSON files.
 * Provides paginated read-only access to classified and protected exposures.
 * 
 * Requirements: 5.5, 9.3
 * Updated to use JSON file retrieval instead of database queries.
 */
@Service
@Transactional(readOnly = true)
@Slf4j
public class ExposureQueryService {
    
    private final ICalculationResultsStorageService storageService;
    private final ExposureMapper exposureMapper;
    
    public ExposureQueryService(
        ICalculationResultsStorageService storageService,
        ExposureMapper exposureMapper
    ) {
        this.storageService = storageService;
        this.exposureMapper = exposureMapper;
    }
    
    /**
     * Retrieves classified exposures for a batch with pagination.
     * Loads data from JSON file instead of database.
     * 
     * Requirement 5.5: Access exposure details by downloading and parsing JSON files
     * Requirement 9.3: Detailed queries trigger JSON file download and parsing
     * 
     * @param batchId the batch identifier
     * @param page the page number (0-indexed)
     * @param size the page size
     * @return paged response containing classified exposure DTOs
     * @throws BatchNotFoundException if batch results cannot be found
     * @throws CalculationNotCompletedException if calculation results are not available
     */
    public PagedResponse<ClassifiedExposureDTO> getClassifiedExposures(
        String batchId,
        int page,
        int size
    ) {
        log.debug("Retrieving classified exposures for batch: {} (page: {}, size: {})", batchId, page, size);
        
        // Retrieve calculation results from JSON file
        RiskCalculationResult result = retrieveCalculationResults(batchId);
        
        // Extract protected exposures from result
        List<ProtectedExposure> protectedExposures = result.calculatedExposures();
        
        // Note: ClassifiedExposure requires region and sector classification which is not stored in JSON
        // The JSON only contains ProtectedExposure data (amounts and mitigations)
        // For now, we'll return protected exposures as the detailed exposure data
        // This aligns with the file-first architecture where JSON is the single source of truth
        
        // Apply pagination
        int start = page * size;
        int end = Math.min(start + size, protectedExposures.size());
        
        List<ProtectedExposure> pagedExposures = protectedExposures.subList(
            Math.min(start, protectedExposures.size()),
            end
        );
        
        // Convert to DTOs - using protected exposure DTOs since classification data is not in JSON
        List<ClassifiedExposureDTO> dtos = pagedExposures.stream()
            .map(this::toClassifiedExposureDTO)
            .collect(Collectors.toList());
        
        log.debug("Retrieved {} classified exposures for batch: {}", dtos.size(), batchId);
        
        return PagedResponse.<ClassifiedExposureDTO>builder()
            .content(dtos)
            .page(page)
            .size(size)
            .totalElements((long) protectedExposures.size())
            .totalPages((int) Math.ceil((double) protectedExposures.size() / size))
            .build();
    }
    
    /**
     * Convert ProtectedExposure to ClassifiedExposureDTO.
     * Since classification data (region, sector) is not stored in JSON,
     * we create a simplified DTO with available data.
     * Classification fields are set to null as they are not available in the JSON format.
     */
    private ClassifiedExposureDTO toClassifiedExposureDTO(ProtectedExposure exposure) {
        return ClassifiedExposureDTO.builder()
            .exposureId(exposure.getExposureId().value())
            .netExposureEur(exposure.getNetExposure().value())
            .geographicRegion(null)  // Not available in JSON format
            .economicSector(null)     // Not available in JSON format
            .classification(null)     // Not available in JSON format
            .build();
    }
    
    /**
     * Retrieves classified exposures filtered by economic sector with pagination.
     * Loads data from JSON file instead of database.
     * 
     * Note: Sector classification is not stored in JSON files in the current format.
     * This method returns all exposures without sector filtering until the JSON format
     * is enhanced to include classification data.
     * 
     * Requirement 5.5: Access exposure details by downloading and parsing JSON files
     * Requirement 9.3: Detailed queries trigger JSON file download and parsing
     * 
     * @param batchId the batch identifier
     * @param sector the economic sector to filter by (currently not supported)
     * @param page the page number (0-indexed)
     * @param size the page size
     * @return paged response containing filtered classified exposure DTOs
     * @throws BatchNotFoundException if batch results cannot be found
     * @throws CalculationNotCompletedException if calculation results are not available
     */
    public PagedResponse<ClassifiedExposureDTO> getClassifiedExposuresBySector(
        String batchId,
        String sector,
        int page,
        int size
    ) {
        log.debug("Retrieving classified exposures by sector for batch: {} (sector: {}, page: {}, size: {})", 
            batchId, sector, page, size);
        
        // Retrieve calculation results from JSON file
        RiskCalculationResult result = retrieveCalculationResults(batchId);
        
        // Extract protected exposures from result
        List<ProtectedExposure> protectedExposures = result.calculatedExposures();
        
        // Note: Sector filtering is not currently supported as classification data is not in JSON
        // Log warning and return all exposures
        log.warn("Sector filtering requested but classification data not available in JSON for batch: {}", batchId);
        
        // Apply pagination
        int start = page * size;
        int end = Math.min(start + size, protectedExposures.size());
        
        List<ProtectedExposure> pagedExposures = protectedExposures.subList(
            Math.min(start, protectedExposures.size()),
            end
        );
        
        // Convert to DTOs
        List<ClassifiedExposureDTO> dtos = pagedExposures.stream()
            .map(this::toClassifiedExposureDTO)
            .collect(Collectors.toList());
        
        log.debug("Retrieved {} classified exposures for batch: {}", dtos.size(), batchId);
        
        return PagedResponse.<ClassifiedExposureDTO>builder()
            .content(dtos)
            .page(page)
            .size(size)
            .totalElements((long) protectedExposures.size())
            .totalPages((int) Math.ceil((double) protectedExposures.size() / size))
            .build();
    }
    
    /**
     * Retrieves protected exposures for a batch with pagination.
     * Loads data from JSON file instead of database.
     * 
     * Requirement 5.5: Access exposure details by downloading and parsing JSON files
     * Requirement 9.3: Detailed queries trigger JSON file download and parsing
     * 
     * @param batchId the batch identifier
     * @param page the page number (0-indexed)
     * @param size the page size
     * @return paged response containing protected exposure DTOs
     * @throws BatchNotFoundException if batch results cannot be found
     * @throws CalculationNotCompletedException if calculation results are not available
     */
    public PagedResponse<ProtectedExposureDTO> getProtectedExposures(
        String batchId,
        int page,
        int size
    ) {
        log.debug("Retrieving protected exposures for batch: {} (page: {}, size: {})", batchId, page, size);
        
        // Retrieve calculation results from JSON file
        RiskCalculationResult result = retrieveCalculationResults(batchId);
        
        // Extract protected exposures from result
        List<ProtectedExposure> protectedExposures = result.calculatedExposures();
        
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
        
        log.debug("Retrieved {} protected exposures for batch: {}", dtos.size(), batchId);
        
        return PagedResponse.<ProtectedExposureDTO>builder()
            .content(dtos)
            .page(page)
            .size(size)
            .totalElements((long) protectedExposures.size())
            .totalPages((int) Math.ceil((double) protectedExposures.size() / size))
            .build();
    }
    
    /**
     * Retrieves calculation results from JSON file storage.
     * 
     * Requirement 5.5: Provide methods to download and parse JSON files
     * Requirement 9.3: Detailed queries trigger JSON file download and parsing
     * 
     * @param batchId the batch identifier
     * @return the risk calculation result
     * @throws BatchNotFoundException if batch results cannot be found
     * @throws CalculationNotCompletedException if calculation results are not available
     */
    private RiskCalculationResult retrieveCalculationResults(String batchId) {
        Result<RiskCalculationResult> result = storageService.retrieveCalculationResults(batchId);
        
        if (result.isFailure()) {
            String errorMessage = result.getError()
                .map(error -> error.getMessage())
                .orElse("Unknown error");
            log.error("Failed to retrieve calculation results for batch {}: {}", batchId, errorMessage);
            
            // Determine appropriate exception based on error message
            if (errorMessage.contains("not found") || errorMessage.contains("does not exist")) {
                throw new BatchNotFoundException(batchId);
            } else {
                throw new CalculationNotCompletedException(
                    batchId,
                    com.bcbs239.regtech.riskcalculation.domain.analysis.ProcessingState.FAILED,
                    String.format("Calculation results not available for batch %s: %s", batchId, errorMessage)
                );
            }
        }
        
        return result.getValue()
            .orElseThrow(() -> new CalculationNotCompletedException(
                batchId,
                com.bcbs239.regtech.riskcalculation.domain.analysis.ProcessingState.FAILED,
                "Calculation results not available for batch " + batchId
            ));
    }
}
