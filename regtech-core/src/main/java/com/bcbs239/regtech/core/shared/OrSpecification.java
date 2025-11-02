package com.bcbs239.regtech.core.shared;

import java.util.ArrayList;
import java.util.List;

/**
 * Specification that combines two specifications using logical OR.
 * Either specification must be satisfied for this specification to be satisfied.
 *
 * @param <T> The type of object this specification can evaluate
 */
public class OrSpecification<T> implements Specification<T> {
    private final Specification<T> left;
    private final Specification<T> right;

    public OrSpecification(Specification<T> left, Specification<T> right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public Result<Void> isSatisfiedBy(T candidate) {
        Result<Void> leftResult = left.isSatisfiedBy(candidate);
        Result<Void> rightResult = right.isSatisfiedBy(candidate);

        // If either is successful, return success
        if (leftResult.isSuccess() || rightResult.isSuccess()) {
            return Result.success();
        }

        // Both failed, collect all errors
        List<ErrorDetail> allErrors = new ArrayList<>();
        allErrors.addAll(leftResult.getErrors());
        allErrors.addAll(rightResult.getErrors());

        return Result.failure(allErrors);
    }
}