package com.bcbs239.regtech.iam.domain.bankprofile.valueobject;

import com.bcbs239.regtech.core.domain.shared.Maybe;
import lombok.Value;

/**
 * Italian VAT Number (Partita IVA)
 * Format: IT + 11 digits
 * 
 * Optional field - uses Maybe pattern
 */
@Value
public class VatNumber {
    String value;
    
    private VatNumber(String value) {
        this.value = value;
    }
    
    /**
     * Smart constructor - returns Maybe&lt;VatNumber&gt;
     * Empty or invalid values return Maybe.empty()
     */
    public static Maybe<VatNumber> of(String value) {
        if (value == null || value.isBlank()) {
            return Maybe.empty();
        }
        
        String trimmed = value.trim().toUpperCase();
        
        if (!trimmed.matches("IT\\d{11}")) {
            return Maybe.empty();
        }
        
        return Maybe.of(new VatNumber(trimmed));
    }
    
    @Override
    public String toString() {
        return value;
    }
}
