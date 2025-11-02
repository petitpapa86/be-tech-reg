package com.bcbs239.regtech.ingestion.domain.specification;

/**
 * Specification that negates another specification.
 */
public class NotSpecification<T> implements Specification<T> {
    
    private final Specification<T> specification;
    
    public NotSpecification(Specification<T> specification) {
        this.specification = specification;
    }
    
    @Override
    public boolean isSatisfiedBy(T candidate) {
        return !specification.isSatisfiedBy(candidate);
    }
}