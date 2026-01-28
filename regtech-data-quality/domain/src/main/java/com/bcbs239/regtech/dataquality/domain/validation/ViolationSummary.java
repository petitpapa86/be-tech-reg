package com.bcbs239.regtech.dataquality.domain.validation;

/**
 * Summary of violations grouped by severity levels according to BCBS 239.
 */
public class ViolationSummary {
    private final int total;
    private final int critical;
    private final int high;
    private final int medium;
    private final int low;

    public ViolationSummary(int total, int critical, int high, int medium, int low) {
        this.total = total;
        this.critical = critical;
        this.high = high;
        this.medium = medium;
        this.low = low;
    }

    public int getTotal() { return total; }
    public int getCritical() { return critical; }
    public int getHigh() { return high; }
    public int getMedium() { return medium; }
    public int getLow() { return low; }
}
