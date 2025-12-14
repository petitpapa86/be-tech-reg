package com.bcbs239.regtech.riskcalculation.infrastructure.cache;

/**
 * Tracks statistics for the ExchangeRateCache to monitor performance.
 * Used to measure cache hit ratio and overall cache effectiveness.
 */
public class CacheStatistics {
    
    private int hits = 0;
    private int misses = 0;
    
    /**
     * Records a cache hit (rate found in cache).
     */
    public void recordHit() {
        hits++;
    }
    
    /**
     * Records a cache miss (rate not found in cache, had to fetch from provider).
     */
    public void recordMiss() {
        misses++;
    }
    
    /**
     * Gets the total number of cache hits.
     * 
     * @return number of cache hits
     */
    public int getHits() {
        return hits;
    }
    
    /**
     * Gets the total number of cache misses.
     * 
     * @return number of cache misses
     */
    public int getMisses() {
        return misses;
    }
    
    /**
     * Gets the total number of cache requests (hits + misses).
     * 
     * @return total cache requests
     */
    public int getTotalRequests() {
        return hits + misses;
    }
    
    /**
     * Calculates the cache hit ratio as a percentage.
     * 
     * @return hit ratio between 0.0 and 1.0, or 0.0 if no requests made
     */
    public double getHitRatio() {
        int total = getTotalRequests();
        return total == 0 ? 0.0 : (double) hits / total;
    }
    
    /**
     * Gets the cache hit ratio as a percentage string.
     * 
     * @return formatted percentage (e.g., "95.5%")
     */
    public String getHitRatioPercentage() {
        return String.format("%.1f%%", getHitRatio() * 100);
    }
    
    /**
     * Resets all statistics to zero.
     */
    public void reset() {
        hits = 0;
        misses = 0;
    }
    
    @Override
    public String toString() {
        return String.format("CacheStatistics{hits=%d, misses=%d, hitRatio=%s}", 
            hits, misses, getHitRatioPercentage());
    }
}
