package com.bcbs239.regtech.core.domain.specifications;

import com.bcbs239.regtech.core.domain.shared.Result;

/**
 * Base interface for the Specification pattern.
 * Specifications encapsulate business rules and can be composed using logical operators.
 *
 * @param <T> The type of object this specification can evaluate
 */
public interface Specification<T> {
    
    /**
     * Evaluates whether the candidate satisfies this specification
     *
     * @param candidate The object to evaluate
     * @return Result.success() if satisfied, Result.failure() with error details if not
     */
    Result<Void> isSatisfiedBy(T candidate);
    
    /**
     * Combines this specification with another using logical AND
     *
     * @param other The specification to combine with
     * @return A new specification that requires both this and other to be satisfied
     */
    default Specification<T> and(Specification<T> other) {
        return new AndSpecification<>(this, other);
    }
    
    /**
     * Combines this specification with another using logical OR
     *
     * @param other The specification to combine with
     * @return A new specification that requires either this or other to be satisfied
     */
    default Specification<T> or(Specification<T> other) {
        return new OrSpecification<>(this, other);
    }
    
    /**
     * Negates this specification using logical NOT
     *
     * @return A new specification that is satisfied when this specification is not satisfied
     */
    default Specification<T> not() {
        return new NotSpecification<>(this);
    }
}

