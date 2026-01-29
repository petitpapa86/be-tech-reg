package com.bcbs239.regtech.dataquality.domain.model.reporting;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record StoredRecommendation(
    @JsonProperty("severity") String severity,
    @JsonProperty("color") String color,
    @JsonProperty("actionItems") List<String> actionItems,
    @JsonProperty("hasActions") boolean hasActions,
    @JsonProperty("icon") String icon,
    @JsonProperty("ruleId") String ruleId,
    @JsonProperty("priority") int priority,
    @JsonProperty("message") String message,
    @JsonProperty("locale") String locale
) {}
