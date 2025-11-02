package com.bcbs239.regtech.ingestion.domain.specification;

import java.util.function.Predicate;

/**
 * Base interface for the Specification pattern.
 * Allows combining specifications with logical operators.
 */
public interface Specification<T> extends Predicate<T> {
    
    /**
     * Combine this specification with another using logical AND.
     */
    default Specification<T> and(Specification<T> other) {
        return new AndSpecification<>(this, other);
    }
    
    /**
     * Combine this specification with another using logical OR.
     */
    default Specification<T> or(Specification<T> other) {
        return new OrSpecification<>(this, other);
    }
    
    /**
     * Negate this specification.
     */
    default Specification<T> not() {
        return new NotSpecification<>(this);
    }
    
    /**
     * Check if the specification is satisfied by the given candidate.
     */
    boolean isSatisfiedBy(T candidate);
    
    /**
     * Default implementation of Predicate.test() that delegates to isSatisfiedBy().
     */
    @Override
    default boolean test(T candidate) {
        return isSatisfiedBy(candidate);
    }
}