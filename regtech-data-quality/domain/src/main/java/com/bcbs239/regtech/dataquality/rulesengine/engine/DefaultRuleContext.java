package com.bcbs239.regtech.dataquality.rulesengine.engine;

import java.util.HashMap;
import java.util.Map;

public class DefaultRuleContext implements RuleContext {
    private final Map<String, Object> data;

    public DefaultRuleContext(Map<String, Object> data) {
        this.data = new HashMap<>(data);
    }

    @Override
    public Object get(String key) {
        return data.get(key);
    }

    @Override
    public <T> T get(String key, Class<T> type) {
        Object value = data.get(key);
        return value != null && type.isInstance(value) ? type.cast(value) : null;
    }

    @Override
    public void put(String key, Object value) { data.put(key, value); }

    @Override
    public Map<String, Object> getAllData() { return new HashMap<>(data); }

    @Override
    public boolean containsKey(String key) { return data.containsKey(key); }

    @Override
    public void clear() { data.clear(); }

    @Override
    public int size() { return data.size(); }
}
