package com.bcbs239.regtech.dataquality.rulesengine.engine;

import java.util.Map;

/**
 * Context for rule execution containing data and intermediate results.
 * 
 * <p>The context holds all information needed for rule evaluation and 
 * accumulates results during execution.</p>
 */
public interface RuleContext {
    
    /**
     * Gets a value from the context.
     * 
     * @param key The key
     * @return The value, or null if not present
     */
    Object get(String key);
    
    /**
     * Gets a value cast to a specific type.
     * 
     * @param key The key
     * @param type The target type
     * @param <T> Type parameter
     * @return The value cast to the specified type
     */
    <T> T get(String key, Class<T> type);
    
    /**
     * Puts a value into the context.
     * 
     * @param key The key
     * @param value The value
     */
    void put(String key, Object value);
    
    /**
     * Gets all data in the context.
     * 
     * @return Map of all context data
     */
    Map<String, Object> getAllData();
    
    /**
     * Checks if the context contains a key.
     * 
     * @param key The key to check
     * @return true if the key exists
     */
    boolean containsKey(String key);
    
    /**
     * Clears all data from the context.
     */
    void clear();
    
    /**
     * Gets the size of the context.
     * 
     * @return Number of entries in the context
     */
    int size();
}
