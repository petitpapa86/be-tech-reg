# CORS Configuration Verification Guide

## Overview

This guide provides instructions for verifying that CORS (Cross-Origin Resource Sharing) configuration is working correctly after the Spring Framework 7 migration.

## Spring Framework 7 Changes

**Key Behavior Change**: As of Spring Framework 7 (#31839), CORS pre-flight requests are **NOT rejected** when CORS configuration is empty.

- **Previous Behavior**: Empty CORS configuration would reject pre-flight OPTIONS requests
- **New Behavior**: Empty CORS configuration allows pre-flight OPTIONS requests to proceed
- **Our Implementation**: We provide explicit CORS configuration for security

## Configuration Files

### 1. CorsConfig.java
Location: `regtech-app/src/main/java/com/bcbs239/regtech/app/config/CorsConfig.java`

Provides explicit CORS configuration with:
- Allowed origins
- Allowed methods
- Allowed headers
- Exposed headers
- Credentials support
- Max age for pre-flight caching

### 2. SecurityConfig.java
Location: `regtech-app/src/main/java/com/bcbs239/regtech/app/config/SecurityConfig.java`

Integrates CORS configuration with Spring Security:
```java
.cors(cors -> cors.configurationSource(corsConfigurationSource))
```

### 3. application.yml
Location: `regtech-app/src/main/resources/application.yml`

Provides configurable CORS settings:
```yaml
cors:
  allowed-origins: ${CORS_ALLOWED_ORIGINS:http://localhost:3000,http://localhost:4200}
  allowed-methods: ${CORS_ALLOWED_METHODS:GET,POST,PUT,DELETE,OPTIONS,PATCH}
  allowed-headers: ${CORS_ALLOWED_HEADERS:*}
  exposed-headers: ${CORS_EXPOSED_HEADERS:X-Correlation-ID,Authorization}
  allow-credentials: ${CORS_ALLOW_CREDENTIALS:true}
  max-age: ${CORS_MAX_AGE:3600}
```

## Manual Verification

### Prerequisites
- Application running on http://localhost:8080
- curl or similar HTTP client

### Test 1: Pre-Flight Request (OPTIONS)

Test that pre-flight OPTIONS requests return appropriate CORS headers:

```bash
curl -X OPTIONS http://localhost:8080/api/health \
  -H "Origin: http://localhost:3000" \
  -H "Access-Control-Request-Method: GET" \
  -H "Access-Control-Request-Headers: Content-Type" \
  -v
```

**Expected Response Headers**:
```
HTTP/1.1 200 OK
Access-Control-Allow-Origin: http://localhost:3000
Access-Control-Allow-Methods: GET,POST,PUT,DELETE,OPTIONS,PATCH
Access-Control-Allow-Headers: Content-Type
Access-Control-Allow-Credentials: true
Access-Control-Max-Age: 3600
```

**Validates**: Requirements 12.1, 12.2, 12.4

### Test 2: Allowed Origin

Test that requests from allowed origins receive CORS headers:

```bash
curl -X GET http://localhost:8080/api/health \
  -H "Origin: http://localhost:3000" \
  -v
```

**Expected Response Headers**:
```
HTTP/1.1 200 OK
Access-Control-Allow-Origin: http://localhost:3000
Access-Control-Allow-Credentials: true
Access-Control-Expose-Headers: X-Correlation-ID,Authorization
```

**Validates**: Requirements 12.3, 12.4, 12.5

### Test 3: Disallowed Origin

Test that requests from disallowed origins do NOT receive CORS headers:

```bash
curl -X GET http://localhost:8080/api/health \
  -H "Origin: http://evil.com" \
  -v
```

**Expected Response Headers**:
```
HTTP/1.1 200 OK
(No Access-Control-Allow-Origin header)
```

**Validates**: Requirements 12.3, 12.5

### Test 4: Actual Request with Custom Headers

Test that actual requests with custom headers work:

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Origin: http://localhost:3000" \
  -H "Content-Type: application/json" \
  -H "X-Custom-Header: value" \
  -d '{"username":"test","password":"test"}' \
  -v
```

**Expected Response Headers**:
```
HTTP/1.1 200 OK (or appropriate status)
Access-Control-Allow-Origin: http://localhost:3000
Access-Control-Allow-Credentials: true
Access-Control-Expose-Headers: X-Correlation-ID,Authorization
```

**Validates**: Requirements 12.4, 12.5

### Test 5: Credentials Support

Test that credentials (cookies, auth headers) are allowed:

```bash
curl -X GET http://localhost:8080/api/health \
  -H "Origin: http://localhost:3000" \
  -H "Authorization: Bearer token123" \
  -H "Cookie: session=abc123" \
  -v
```

**Expected Response Headers**:
```
HTTP/1.1 200 OK
Access-Control-Allow-Origin: http://localhost:3000
Access-Control-Allow-Credentials: true
```

**Validates**: Requirement 12.5

## Automated Testing

### Unit Tests

Run unit tests that verify CORS configuration without full application context:

```bash
mvn test -Dtest=CorsConfigUnitTest
```

**Tests**:
- ✅ CORS configuration source is created
- ✅ Allowed origins are configured
- ✅ Allowed methods are configured
- ✅ Allowed headers are configured
- ✅ Exposed headers are configured
- ✅ Credentials are allowed
- ✅ Max age is configured
- ✅ Configuration applies to all paths

### Integration Tests

Run integration tests that verify CORS behavior with full application context:

```bash
mvn test -Dtest=CorsConfigurationTest
```

**Tests**:
- ✅ Pre-flight requests return CORS headers
- ✅ Allowed origins receive CORS headers
- ✅ Disallowed origins do not receive CORS headers
- ✅ Allowed methods are included in pre-flight response
- ✅ Exposed headers are included in response
- ✅ Credentials are allowed
- ✅ Pre-flight with custom headers is handled
- ✅ Actual requests include CORS headers

## Browser Testing

### Using Browser DevTools

1. Open browser DevTools (F12)
2. Navigate to Network tab
3. Make a request from a different origin (e.g., http://localhost:3000)
4. Check response headers for CORS headers

### Expected Behavior

**Successful CORS Request**:
- Request completes successfully
- Response headers include `Access-Control-Allow-Origin`
- No CORS errors in console

**Failed CORS Request**:
- Request blocked by browser
- Console shows CORS error: "Access to fetch at 'http://localhost:8080/api/...' from origin 'http://evil.com' has been blocked by CORS policy"

## Troubleshooting

### Issue: CORS headers not present

**Possible Causes**:
1. CORS configuration not loaded
2. Origin not in allowed list
3. Security filter chain not configured correctly

**Solution**:
1. Check application.yml for CORS configuration
2. Verify origin is in `cors.allowed-origins`
3. Verify SecurityConfig includes `.cors()` configuration

### Issue: Pre-flight requests fail

**Possible Causes**:
1. OPTIONS method not allowed
2. CORS configuration not applied to security filter chain
3. Custom security filter blocking OPTIONS requests

**Solution**:
1. Verify `OPTIONS` is in `cors.allowed-methods`
2. Check SecurityConfig has `.cors()` configuration
3. Review custom security filters

### Issue: Credentials not working

**Possible Causes**:
1. `allow-credentials` set to false
2. Allowed origin is `*` (wildcard not allowed with credentials)
3. Browser not sending credentials

**Solution**:
1. Set `cors.allow-credentials: true`
2. Use specific origins, not `*`
3. Ensure browser request includes `credentials: 'include'`

## Production Configuration

### Environment Variables

For production, configure CORS via environment variables:

```bash
export CORS_ALLOWED_ORIGINS="https://app.example.com,https://admin.example.com"
export CORS_ALLOWED_METHODS="GET,POST,PUT,DELETE"
export CORS_ALLOWED_HEADERS="Content-Type,Authorization,X-Correlation-ID"
export CORS_ALLOW_CREDENTIALS="true"
export CORS_MAX_AGE="3600"
```

### Security Best Practices

1. **Never use `*` for allowed origins in production**
2. **Limit allowed methods to only what's needed**
3. **Explicitly list allowed headers instead of using `*`**
4. **Only enable credentials if needed**
5. **Use appropriate max age for pre-flight caching**
6. **Regularly audit CORS configuration**

## Requirements Validation

| Requirement | Description | Verification Method |
|-------------|-------------|---------------------|
| 12.1 | Pre-flight requests not rejected when config is empty | Manual Test 1, Unit Tests |
| 12.2 | OPTIONS requests handled correctly | Manual Test 1, Integration Tests |
| 12.3 | CORS origin validation | Manual Tests 2 & 3, Integration Tests |
| 12.4 | Pre-flight responses include CORS headers | Manual Tests 1 & 4, Integration Tests |
| 12.5 | Cross-origin requests enforce policies | Manual Tests 2, 3, 5, Integration Tests |

## References

- Spring Framework 7 Issue #31839: CORS Pre-Flight requests behavior change
- Spring Security CORS Documentation: https://docs.spring.io/spring-security/reference/servlet/integrations/cors.html
- MDN CORS Documentation: https://developer.mozilla.org/en-US/docs/Web/HTTP/CORS
- W3C CORS Specification: https://www.w3.org/TR/cors/
