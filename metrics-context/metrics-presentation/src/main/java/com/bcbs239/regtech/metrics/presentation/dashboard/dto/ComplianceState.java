package com.bcbs239.regtech.metrics.presentation.dashboard.dto;

public class ComplianceState {
    public final Double overall;
    public final Double dataQuality;
    public final Double bcbs;
    public final Double completeness;

    public ComplianceState(Double overall, Double dataQuality, Double bcbs, Double completeness) {
        this.overall = overall;
        this.dataQuality = dataQuality;
        this.bcbs = bcbs;
        this.completeness = completeness;
    }
}
