package com.bcbs239.regtech.dataquality.rulesengine.engine;

import java.util.Map;

public interface RuleContext {
    Object get(String key);
    <T> T get(String key, Class<T> type);
    void put(String key, Object value);
    Map<String, Object> getAllData();
    boolean containsKey(String key);
    void clear();
    int size();
}
