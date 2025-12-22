package com.bcbs239.regtech.dataquality.domain.rules;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

/**
 * Domain DTO for RuleParameter (not a JPA entity).
 */
public record RuleParameterDto(
    Long parameterId,
    String parameterName,
    String parameterValue,
    ParameterType parameterType,
    String dataType,
    String unit,
    BigDecimal minValue,
    BigDecimal maxValue,
    String description,
    Boolean isConfigurable
) {
    public <T> T getTypedValue(Class<T> type) {
        if (parameterValue == null) return null;
        try {
            if (type == BigDecimal.class) return type.cast(new BigDecimal(parameterValue));
            if (type == Integer.class) return type.cast(Integer.valueOf(parameterValue));
            if (type == Long.class) return type.cast(Long.valueOf(parameterValue));
            if (type == Double.class) return type.cast(Double.valueOf(parameterValue));
            if (type == Boolean.class) return type.cast(Boolean.valueOf(parameterValue));
            return type.cast(parameterValue);
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot convert parameter value to type " + type.getSimpleName(), e);
        }
    }

    public List<String> getListValue() {
        if (parameterValue == null || parameterValue.isEmpty()) return List.of();
        return Arrays.asList(parameterValue.split(","));
    }
}
