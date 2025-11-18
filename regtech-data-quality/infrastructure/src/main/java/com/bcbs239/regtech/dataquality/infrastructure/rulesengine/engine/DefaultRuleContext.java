package com.bcbs239.regtech.dataquality.rulesengine.engine;

import lombok.Builder;
import lombok.Singular;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default implementation of RuleContext.
 * 
 * <p>Thread-safe implementation using ConcurrentHashMap.</p>
 */
@Builder
public class DefaultRuleContext implements RuleContext {
    
    @Singular("data")
    private final Map<String, Object> contextData;
    
    /**
     * Creates a new empty context.
     */
    public DefaultRuleContext() {
        this.contextData = new ConcurrentHashMap<>();
    }
    
    /**
     * Creates a context with initial data.
     * 
     * @param initialData Initial data map
     */
    public DefaultRuleContext(Map<String, Object> initialData) {
        this.contextData = new ConcurrentHashMap<>(initialData != null ? initialData : Map.of());
    }
    
    @Override
    public Object get(String key) {
        return contextData.get(key);
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        Object value = contextData.get(key);
        if (value == null) {
            return null;
        }
        
        if (type.isInstance(value)) {
            return (T) value;
        }
        
        throw new ClassCastException(
            String.format("Cannot cast value for key '%s' from %s to %s", 
                key, value.getClass().getName(), type.getName()));
    }
    
    @Override
    public void put(String key, Object value) {
        contextData.put(key, value);
    }
    
    @Override
    public Map<String, Object> getAllData() {
        return new HashMap<>(contextData);
    }
    
    @Override
    public boolean containsKey(String key) {
        return contextData.containsKey(key);
    }
    
    @Override
    public void clear() {
        contextData.clear();
    }
    
    @Override
    public int size() {
        return contextData.size();
    }
    
    /**
     * Creates a new context from a map.
     * 
     * @param data The data map
     * @return New context instance
     */
    public static DefaultRuleContext of(Map<String, Object> data) {
        return new DefaultRuleContext(data);
    }
    
    /**
     * Creates an empty context.
     * 
     * @return New empty context instance
     */
    public static DefaultRuleContext empty() {
        return new DefaultRuleContext();
    }
}
