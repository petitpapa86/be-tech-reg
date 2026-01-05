package com.bcbs239.regtech.iam.domain.bankprofile.valueobject;

import com.bcbs239.regtech.core.domain.shared.Maybe;
import lombok.Value;

/**
 * Website URL value object
 * Optional field - uses Maybe pattern
 */
@Value
public class WebsiteUrl {
    String value;
    
    private static final String URL_REGEX = "https?://.+";
    
    private WebsiteUrl(String value) {
        this.value = value;
    }
    
    public static Maybe<WebsiteUrl> of(String value) {
        if (value == null || value.isBlank()) {
            return Maybe.none();
        }
        
        String trimmed = value.trim();
        
        if (!trimmed.matches(URL_REGEX)) {
            return Maybe.none();
        }
        
        return Maybe.some(new WebsiteUrl(trimmed));
    }
    
    @Override
    public String toString() {
        return value;
    }
}
