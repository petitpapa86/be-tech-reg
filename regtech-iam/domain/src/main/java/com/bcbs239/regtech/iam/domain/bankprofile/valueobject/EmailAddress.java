package com.bcbs239.regtech.iam.domain.bankprofile.valueobject;

import com.bcbs239.regtech.core.domain.shared.Maybe;
import lombok.Value;

/**
 * Email address value object
 * Optional field - uses Maybe pattern
 */
@Value
public class EmailAddress {
    String value;
    
    private static final String EMAIL_REGEX = 
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
    
    private EmailAddress(String value) {
        this.value = value;
    }
    
    public static Maybe<EmailAddress> of(String value) {
        if (value == null || value.isBlank()) {
            return Maybe.empty();
        }
        
        String trimmed = value.trim().toLowerCase();
        
        if (!trimmed.matches(EMAIL_REGEX)) {
            return Maybe.empty();
        }
        
        return Maybe.of(new EmailAddress(trimmed));
    }
    
    @Override
    public String toString() {
        return value;
    }
}
