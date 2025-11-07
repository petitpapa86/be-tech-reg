package com.bcbs239.regtech.billing.application.integration;

import com.bcbs239.regtech.billing.domain.accounts.BillingAccount;
import com.bcbs239.regtech.billing.domain.accounts.BillingAccountId;
import com.bcbs239.regtech.billing.domain.accounts.BillingAccountRepository;
import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Maybe;
import com.bcbs239.regtech.core.domain.shared.Result;
import org.springframework.stereotype.Component;

@Component
public class FinalizeBillingAccountCommandHandler {

    private final BillingAccountRepository billingAccountRepository;

    public FinalizeBillingAccountCommandHandler(BillingAccountRepository billingAccountRepository) {
        this.billingAccountRepository = billingAccountRepository;
    }

    public Result<Void> handle(FinalizeBillingAccountCommand command) {
        String billingAccountIdStr = (String) command.payload().get("billingAccountId");
        BillingAccountId billingAccountId = BillingAccountId.fromString(billingAccountIdStr).getValue().get();
        
        Maybe<BillingAccount> maybeAccount = billingAccountRepository.findById(billingAccountId);
        if (maybeAccount.isPresent()) {
            BillingAccount billingAccount = maybeAccount.getValue();
            billingAccount.finalizeAccount();
            Result<BillingAccountId> saveResult = billingAccountRepository.save(billingAccount);
            if (saveResult.isSuccess()) {
                return Result.success(null);
            } else {
                return Result.failure(saveResult.getError().get());
            }
        } else {
            return Result.failure(ErrorDetail.of("BILLING_ACCOUNT_NOT_FOUND", ErrorType.BUSINESS_RULE_ERROR, "Billing account not found: " + billingAccountIdStr, "billing.account.not.found"));
        }
    }
}

