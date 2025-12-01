package com.bcbs239.regtech.riskcalculation.domain.calculation;

import com.bcbs239.regtech.riskcalculation.domain.analysis.HHI;

/**
 * Value object representing concentration indices for portfolio analysis.
 * Contains Herfindahl-Hirschman Index (HHI) values for geographic and sector concentration.
 */
public class ConcentrationIndices {
    private final HHI geographicHHI;
    private final HHI sectorHHI;

    public ConcentrationIndices(HHI geographicHHI, HHI sectorHHI) {
        this.geographicHHI = geographicHHI;
        this.sectorHHI = sectorHHI;
    }

    public static ConcentrationIndices of(HHI geographicHHI, HHI sectorHHI) {
        return new ConcentrationIndices(geographicHHI, sectorHHI);
    }

    public HHI getGeographicHHI() {
        return geographicHHI;
    }

    public HHI getSectorHHI() {
        return sectorHHI;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConcentrationIndices that = (ConcentrationIndices) o;
        return geographicHHI.equals(that.geographicHHI) && sectorHHI.equals(that.sectorHHI);
    }

    @Override
    public int hashCode() {
        int result = geographicHHI.hashCode();
        result = 31 * result + sectorHHI.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "ConcentrationIndices{" +
                "geographicHHI=" + geographicHHI +
                ", sectorHHI=" + sectorHHI +
                '}';
    }
}
