package com.bcbs239.regtech.app.monitoring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Database health indicator for connection pool and query performance monitoring.
 * Provides comprehensive database health checks with response time measurement and caching.
 * 
 * Requirements: 4.2
 * - Monitor database connectivity and response times
 * - Validate connection pool status
 * - Cache health status to avoid performance impact
 */
@Component
public class DatabaseHealthIndicator implements HealthIndicator {
    
    private static final Logger logger = LoggerFactory.getLogger(DatabaseHealthIndicator.class);
    
    private static final String HEALTH_CHECK_QUERY = "SELECT 1";
    private static final Duration CACHE_TTL = Duration.ofSeconds(30);
    private static final Duration SLOW_QUERY_THRESHOLD = Duration.ofMillis(100);
    private static final Duration TIMEOUT_THRESHOLD = Duration.ofSeconds(5);
    
    private final DataSource dataSource;
    private final Map<String, CachedHealthResult> healthCache = new ConcurrentHashMap<>();
    
    public DatabaseHealthIndicator(DataSource dataSource) {
        this.dataSource = dataSource;
    }
    
    @Override
    public Health health() {
        String cacheKey = "database_health";
        CachedHealthResult cached = healthCache.get(cacheKey);
        
        // Return cached result if still valid
        if (cached != null && cached.isValid()) {
            logger.debug("Returning cached database health result");
            return cached.health;
        }
        
        // Perform fresh health check
        Health health = performHealthCheck();
        healthCache.put(cacheKey, new CachedHealthResult(health));
        
        return health;
    }
    
    /**
     * Performs the actual database health check.
     */
    private Health performHealthCheck() {
        Instant startTime = Instant.now();
        
        try (Connection connection = dataSource.getConnection()) {
            // Test basic connectivity
            if (connection.isClosed()) {
                return Health.down()
                    .withDetail("error", "Database connection is closed")
                    .withDetail("timestamp", startTime.toString())
                    .build();
            }
            
            // Test query execution and measure response time
            Instant queryStart = Instant.now();
            boolean querySuccess = executeHealthCheckQuery(connection);
            Duration queryDuration = Duration.between(queryStart, Instant.now());
            
            if (!querySuccess) {
                return Health.down()
                    .withDetail("error", "Health check query failed")
                    .withDetail("query", HEALTH_CHECK_QUERY)
                    .withDetail("timestamp", startTime.toString())
                    .build();
            }
            
            // Gather connection pool information
            Map<String, Object> connectionInfo = gatherConnectionInfo(connection);
            
            // Determine health status based on response time
            Health.Builder builder = queryDuration.compareTo(SLOW_QUERY_THRESHOLD) > 0 
                ? Health.up().withDetail("warning", "Slow database response time")
                : Health.up();
            
            return builder
                .withDetail("database", connectionInfo.get("databaseProductName"))
                .withDetail("version", connectionInfo.get("databaseProductVersion"))
                .withDetail("url", connectionInfo.get("url"))
                .withDetail("responseTime", queryDuration.toMillis() + "ms")
                .withDetail("slowQueryThreshold", SLOW_QUERY_THRESHOLD.toMillis() + "ms")
                .withDetail("connectionValid", true)
                .withDetail("timestamp", startTime.toString())
                .withDetails(connectionInfo)
                .build();
                
        } catch (SQLException e) {
            logger.error("Database health check failed", e);
            
            return Health.down()
                .withDetail("error", "Database connection failed")
                .withDetail("message", e.getMessage())
                .withDetail("sqlState", e.getSQLState())
                .withDetail("errorCode", e.getErrorCode())
                .withDetail("timestamp", startTime.toString())
                .build();
        } catch (Exception e) {
            logger.error("Unexpected error during database health check", e);
            
            return Health.down()
                .withDetail("error", "Unexpected database health check failure")
                .withDetail("message", e.getMessage())
                .withDetail("timestamp", startTime.toString())
                .build();
        }
    }
    
    /**
     * Executes the health check query with timeout protection.
     */
    private boolean executeHealthCheckQuery(Connection connection) {
        try (PreparedStatement statement = connection.prepareStatement(HEALTH_CHECK_QUERY)) {
            statement.setQueryTimeout((int) TIMEOUT_THRESHOLD.getSeconds());
            
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getInt(1) == 1;
            }
        } catch (SQLException e) {
            logger.warn("Health check query execution failed: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Gathers connection and database information.
     */
    private Map<String, Object> gatherConnectionInfo(Connection connection) {
        Map<String, Object> info = new HashMap<>();
        
        try {
            var metaData = connection.getMetaData();
            info.put("databaseProductName", metaData.getDatabaseProductName());
            info.put("databaseProductVersion", metaData.getDatabaseProductVersion());
            info.put("driverName", metaData.getDriverName());
            info.put("driverVersion", metaData.getDriverVersion());
            info.put("url", metaData.getURL());
            info.put("userName", metaData.getUserName());
            info.put("autoCommit", connection.getAutoCommit());
            info.put("readOnly", connection.isReadOnly());
            info.put("transactionIsolation", connection.getTransactionIsolation());
            
            // Connection pool information (if available)
            if (dataSource instanceof com.zaxxer.hikari.HikariDataSource hikariDataSource) {
                var poolMXBean = hikariDataSource.getHikariPoolMXBean();
                if (poolMXBean != null) {
                    info.put("poolSize", poolMXBean.getTotalConnections());
                    info.put("activeConnections", poolMXBean.getActiveConnections());
                    info.put("idleConnections", poolMXBean.getIdleConnections());
                    info.put("maxPoolSize", hikariDataSource.getMaximumPoolSize());
                    info.put("minIdle", hikariDataSource.getMinimumIdle());
                }
            }
            
        } catch (SQLException e) {
            logger.warn("Failed to gather database metadata: {}", e.getMessage());
            info.put("metadataError", e.getMessage());
        }
        
        return info;
    }
    
    /**
     * Cached health result with TTL.
     */
    private static class CachedHealthResult {
        private final Health health;
        private final Instant timestamp;
        
        public CachedHealthResult(Health health) {
            this.health = health;
            this.timestamp = Instant.now();
        }
        
        public boolean isValid() {
            return Duration.between(timestamp, Instant.now()).compareTo(CACHE_TTL) < 0;
        }
    }
}