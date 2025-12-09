package com.bcbs239.regtech.riskcalculation.presentation.mappers;

import com.bcbs239.regtech.riskcalculation.domain.classification.ClassifiedExposure;
import com.bcbs239.regtech.riskcalculation.domain.exposure.ExposureClassification;
import com.bcbs239.regtech.riskcalculation.domain.protection.Mitigation;
import com.bcbs239.regtech.riskcalculation.domain.protection.ProtectedExposure;
import com.bcbs239.regtech.riskcalculation.presentation.dto.ClassifiedExposureDTO;
import com.bcbs239.regtech.riskcalculation.presentation.dto.ExposureClassificationDTO;
import com.bcbs239.regtech.riskcalculation.presentation.dto.MitigationDTO;
import com.bcbs239.regtech.riskcalculation.presentation.dto.ProtectedExposureDTO;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper for converting Exposure domain objects to DTOs.
 * Stateless component that provides null-safe conversion logic.
 * 
 * Requirements: 4.1, 4.2, 4.3, 4.4, 4.5
 */
@Component("exposureDtoMapper")
public class ExposureMapper {
    
    /**
     * Converts a ClassifiedExposure domain object to ClassifiedExposureDTO.
     * 
     * @param classifiedExposure the domain object to convert
     * @return the converted DTO
     * @throws MappingException if the domain object is null or contains invalid data
     */
    public ClassifiedExposureDTO toClassifiedDTO(ClassifiedExposure classifiedExposure) {
        if (classifiedExposure == null) {
            throw new MappingException("ClassifiedExposure cannot be null");
        }
        
        try {
            return ClassifiedExposureDTO.builder()
                .exposureId(classifiedExposure.exposureId().value())
                .netExposureEur(classifiedExposure.netExposure().value())
                .geographicRegion(classifiedExposure.region().name())
                .economicSector(classifiedExposure.sector().name())
                .classification(null) // Classification metadata not available in ClassifiedExposure
                .build();
        } catch (Exception e) {
            throw new MappingException("Failed to map ClassifiedExposure to DTO: " + e.getMessage(), e);
        }
    }
    
    /**
     * Converts a ClassifiedExposure with ExposureClassification to ClassifiedExposureDTO.
     * 
     * @param classifiedExposure the domain object to convert
     * @param classification the exposure classification metadata
     * @return the converted DTO
     * @throws MappingException if the domain object is null or contains invalid data
     */
    public ClassifiedExposureDTO toClassifiedDTO(
        ClassifiedExposure classifiedExposure,
        ExposureClassification classification
    ) {
        if (classifiedExposure == null) {
            throw new MappingException("ClassifiedExposure cannot be null");
        }
        
        try {
            return ClassifiedExposureDTO.builder()
                .exposureId(classifiedExposure.exposureId().value())
                .netExposureEur(classifiedExposure.netExposure().value())
                .geographicRegion(classifiedExposure.region().name())
                .economicSector(classifiedExposure.sector().name())
                .classification(toExposureClassificationDTO(classification))
                .build();
        } catch (Exception e) {
            throw new MappingException("Failed to map ClassifiedExposure to DTO: " + e.getMessage(), e);
        }
    }
    
    /**
     * Converts a ProtectedExposure domain object to ProtectedExposureDTO.
     * 
     * @param protectedExposure the domain object to convert
     * @return the converted DTO
     * @throws MappingException if the domain object is null or contains invalid data
     */
    public ProtectedExposureDTO toProtectedDTO(ProtectedExposure protectedExposure) {
        if (protectedExposure == null) {
            throw new MappingException("ProtectedExposure cannot be null");
        }
        
        try {
            List<MitigationDTO> mitigationDTOs = protectedExposure.getMitigations() != null ?
                protectedExposure.getMitigations().stream()
                    .map(this::toMitigationDTO)
                    .collect(Collectors.toList()) :
                Collections.emptyList();
            
            return ProtectedExposureDTO.builder()
                .exposureId(protectedExposure.getExposureId().value())
                .grossExposureEur(protectedExposure.getGrossExposure().value())
                .netExposureEur(protectedExposure.getNetExposure().value())
                .totalMitigationEur(protectedExposure.getTotalMitigation().value())
                .mitigations(mitigationDTOs)
                .hasMitigations(protectedExposure.hasMitigations())
                .fullyCovered(protectedExposure.isFullyCovered())
                .build();
        } catch (Exception e) {
            throw new MappingException("Failed to map ProtectedExposure to DTO: " + e.getMessage(), e);
        }
    }
    
    /**
     * Converts an ExposureClassification value object to ExposureClassificationDTO.
     * 
     * @param classification the domain value object
     * @return the DTO, or null if input is null
     */
    public ExposureClassificationDTO toExposureClassificationDTO(ExposureClassification classification) {
        if (classification == null) {
            return null;
        }
        
        try {
            return ExposureClassificationDTO.builder()
                .productType(classification.productType())
                .instrumentType(classification.instrumentType().name())
                .balanceSheetType(classification.balanceSheetType().name())
                .countryCode(classification.countryCode())
                .build();
        } catch (Exception e) {
            throw new MappingException("Failed to map ExposureClassification to DTO: " + e.getMessage(), e);
        }
    }
    
    /**
     * Converts a Mitigation entity to MitigationDTO.
     * 
     * @param mitigation the domain entity
     * @return the DTO
     * @throws MappingException if mitigation is null
     */
    public MitigationDTO toMitigationDTO(Mitigation mitigation) {
        if (mitigation == null) {
            throw new MappingException("Mitigation cannot be null");
        }
        
        try {
            return MitigationDTO.builder()
                .type(mitigation.getType().name())
                .valueEur(mitigation.getEurValue().value())
                .build();
        } catch (Exception e) {
            throw new MappingException("Failed to map Mitigation to DTO: " + e.getMessage(), e);
        }
    }
    
    /**
     * Converts a list of ClassifiedExposure objects to DTOs.
     * 
     * @param exposures the list of domain objects
     * @return the list of DTOs
     * @throws MappingException if the list is null
     */
    public List<ClassifiedExposureDTO> toClassifiedDTOList(List<ClassifiedExposure> exposures) {
        if (exposures == null) {
            throw new MappingException("Exposures list cannot be null");
        }
        
        try {
            return exposures.stream()
                .map(this::toClassifiedDTO)
                .collect(Collectors.toList());
        } catch (Exception e) {
            throw new MappingException("Failed to map ClassifiedExposure list to DTOs: " + e.getMessage(), e);
        }
    }
    
    /**
     * Converts a list of ProtectedExposure objects to DTOs.
     * 
     * @param exposures the list of domain objects
     * @return the list of DTOs
     * @throws MappingException if the list is null
     */
    public List<ProtectedExposureDTO> toProtectedDTOList(List<ProtectedExposure> exposures) {
        if (exposures == null) {
            throw new MappingException("Exposures list cannot be null");
        }
        
        try {
            return exposures.stream()
                .map(this::toProtectedDTO)
                .collect(Collectors.toList());
        } catch (Exception e) {
            throw new MappingException("Failed to map ProtectedExposure list to DTOs: " + e.getMessage(), e);
        }
    }
}
