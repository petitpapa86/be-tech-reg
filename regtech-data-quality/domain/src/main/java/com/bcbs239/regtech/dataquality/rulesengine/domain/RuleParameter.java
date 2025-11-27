package com.bcbs239.regtech.dataquality.rulesengine.domain;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "rule_parameters", schema = "dataquality")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RuleParameter {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "parameter_id")
    private Long parameterId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rule_id", nullable = false)
    private BusinessRule rule;
    
    @Column(name = "parameter_name", nullable = false, length = 100)
    private String parameterName;
    
    @Column(name = "parameter_value", nullable = false, columnDefinition = "TEXT")
    private String parameterValue;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "parameter_type", nullable = false, length = 50)
    private ParameterType parameterType;
    
    @Column(name = "data_type", length = 20)
    private String dataType;
    
    @Column(length = 20)
    private String unit;
    
    @Column(name = "min_value", precision = 20, scale = 4)
    private BigDecimal minValue;
    
    @Column(name = "max_value", precision = 20, scale = 4)
    private BigDecimal maxValue;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "is_configurable", nullable = false)
    @Builder.Default
    private Boolean isConfigurable = true;
    
    @Column(name = "created_at")
    private Instant createdAt;
    
    @Column(name = "updated_at")
    private Instant updatedAt;

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

    public boolean isValueValid() {
        if (parameterValue == null) return false;
        if (parameterType == ParameterType.NUMERIC && minValue != null && maxValue != null) {
            try {
                BigDecimal value = new BigDecimal(parameterValue);
                return value.compareTo(minValue) >= 0 && value.compareTo(maxValue) <= 0;
            } catch (NumberFormatException e) { return false; }
        }
        return true;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
