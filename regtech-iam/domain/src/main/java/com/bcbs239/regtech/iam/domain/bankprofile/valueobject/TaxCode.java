package com.bcbs239.regtech.iam.domain.bankprofile.valueobject;

import com.bcbs239.regtech.core.domain.shared.Maybe;
import lombok.Value;

/**
 * Italian Tax Code (Codice Fiscale)
 * Format: 11 digits for legal entities
 * 
 * Optional field - uses Maybe pattern
 */
@Value
public class TaxCode {
    String value;
    
    private TaxCode(String value) {
        this.value = value;
    }
    
    public static Maybe<TaxCode> of(String value) {
        if (value == null || value.isBlank()) {
            return Maybe.empty();
        }
        
        String trimmed = value.trim();
        
        if (!trimmed.matches("\\d{11}")) {
            return Maybe.empty();
        }
        
        return Maybe.of(new TaxCode(trimmed));
    }
    
    @Override
    public String toString() {
        return value;
    }
}
