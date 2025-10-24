package com.bcbs239.regtech.billing.domain.billing;

import lombok.Value;
import java.util.UUID;

@Value(staticConstructor = "of")
public class BillingAccountId {
    String value;
    
    public static BillingAccountId generate() {
        return BillingAccountId.of(UUID.randomUUID().toString());
    }
}
