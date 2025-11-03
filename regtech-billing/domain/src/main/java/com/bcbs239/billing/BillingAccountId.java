package com.bcbs239.billing;

import lombok.Value;
import java.util.UUID;

@Value(staticConstructor = "of")
public class BillingAccountId {
    String value;
    
    public static BillingAccountId generate() {
        return BillingAccountId.of(UUID.randomUUID().toString());
    }
    
    public String getValue() {
        return value;
    }
}
