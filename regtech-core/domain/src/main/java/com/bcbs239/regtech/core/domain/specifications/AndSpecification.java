package com.bcbs239.regtech.core.domain.specifications;

import com.bcbs239.regtech.core.domain.core.Result;
import com.bcbs239.regtech.core.domain.errorhandling.ErrorDetail;

import java.util.ArrayList;
import java.util.List;

/**
 * Specification that combines two specifications using logical AND.
 * Both specifications must be satisfied for this specification to be satisfied.
 *
 * @param <T> The type of object this specification can evaluate
 */
public class AndSpecification<T> implements Specification<T> {
    private final Specification<T> left;
    private final Specification<T> right;

    public AndSpecification(Specification<T> left, Specification<T> right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public Result<Void> isSatisfiedBy(T candidate) {
        Result<Void> leftResult = left.isSatisfiedBy(candidate);
        Result<Void> rightResult = right.isSatisfiedBy(candidate);

        // If both are successful, return success
        if (leftResult.isSuccess() && rightResult.isSuccess()) {
            return Result.success();
        }

        // Collect all errors from both specifications
        List<ErrorDetail> allErrors = new ArrayList<>();
        if (leftResult.isFailure()) {
            allErrors.addAll(leftResult.getErrors());
        }
        if (rightResult.isFailure()) {
            allErrors.addAll(rightResult.getErrors());
        }

        return Result.failure(allErrors);
    }
}
