package com.bcbs239.regtech.billing.application.policies.createstripecustomer;

import com.bcbs239.regtech.core.saga.SagaCommand;
import com.bcbs239.regtech.core.saga.SagaId;
import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.core.shared.Result;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public class CreateStripeCustomerCommand extends SagaCommand {

    private CreateStripeCustomerCommand(SagaId sagaId, String userEmail, String userName, String paymentMethodId) {
        super(sagaId, "CreateStripeCustomerCommand", Map.of(
            "userEmail", userEmail,
            "userName", userName,
            "paymentMethodId", paymentMethodId
        ), Instant.now());
    }

    public static Result<CreateStripeCustomerCommand> create(SagaId sagaId, String userEmail, String userName, String paymentMethodId) {
        if (paymentMethodId == null) {
            return Result.failure(new ErrorDetail("VALIDATION_ERROR", "paymentMethodId must not be null", "paymentMethodId.required"));
        }
        return Result.success(new CreateStripeCustomerCommand(sagaId, userEmail, userName, paymentMethodId));
    }

    public String getUserEmail() {
        return (String) payload().get("userEmail");
    }

    public String getUserName() {
        return (String) payload().get("userName");
    }

    public String getPaymentMethodId() {
        return (String) payload().get("paymentMethodId");
    }
}