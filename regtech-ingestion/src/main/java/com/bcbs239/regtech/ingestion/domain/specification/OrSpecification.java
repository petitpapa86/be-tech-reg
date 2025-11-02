package com.bcbs239.regtech.ingestion.domain.specification;

/**
 * Specification that combines two specifications with logical OR.
 */
public class OrSpecification<T> implements Specification<T> {
    
    private final Specification<T> left;
    private final Specification<T> right;
    
    public OrSpecification(Specification<T> left, Specification<T> right) {
        this.left = left;
        this.right = right;
    }
    
    @Override
    public boolean isSatisfiedBy(T candidate) {
        return left.isSatisfiedBy(candidate) || right.isSatisfiedBy(candidate);
    }
}