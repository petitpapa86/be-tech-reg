package com.bcbs239.regtech.iam.domain.bankprofile.valueobject;

import com.bcbs239.regtech.core.domain.shared.Maybe;
import lombok.Value;

/**
 * Phone number value object
 * Optional field - uses Maybe pattern
 */
@Value
public class PhoneNumber {
    String value;
    
    private PhoneNumber(String value) {
        this.value = value;
    }
    
    public static Maybe<PhoneNumber> of(String value) {
        if (value == null || value.isBlank()) {
            return Maybe.empty();
        }
        
        String trimmed = value.trim();
        
        if (trimmed.length() > 50) {
            return Maybe.empty();
        }
        
        return Maybe.of(new PhoneNumber(trimmed));
    }
    
    @Override
    public String toString() {
        return value;
    }
}
