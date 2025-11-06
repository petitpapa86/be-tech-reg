package com.bcbs239.regtech.core.domain.specifications;

import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.core.domain.shared.ErrorDetail;

/**
 * Specification that negates another specification using logical NOT.
 * This specification is satisfied when the wrapped specification is not satisfied.
 *
 * @param <T> The type of object this specification can evaluate
 */
public class NotSpecification<T> implements Specification<T> {
    private final Specification<T> specification;

    public NotSpecification(Specification<T> specification) {
        this.specification = specification;
    }

    @Override
    public Result<Void> isSatisfiedBy(T candidate) {
        Result<Void> result = specification.isSatisfiedBy(candidate);

        // If the wrapped specification is satisfied, this NOT specification fails
        if (result.isSuccess()) {
            return Result.failure(ErrorDetail.of("NOT_SPECIFICATION_FAILED", ErrorType.BUSINESS_RULE_ERROR,
                "Negated specification should not be satisfied", "not.specification.failed"));
        }

        // If the wrapped specification fails, this NOT specification succeeds
        return Result.success();
    }
}

