package com.bcbs239.regtech.riskcalculation.application.calculation;

import com.bcbs239.regtech.riskcalculation.domain.classification.ClassifiedExposure;

import java.util.List;

/**
 * Functional interface for handling processed chunks of classified exposures.
 * This allows the ChunkProcessor to be flexible in how it handles each processed chunk.
 */
@FunctionalInterface
public interface ChunkHandler {
    
    /**
     * Handles a processed chunk of classified exposures.
     * 
     * @param classifiedExposures the list of classified exposures in this chunk
     */
    void handle(List<ClassifiedExposure> classifiedExposures);
}
