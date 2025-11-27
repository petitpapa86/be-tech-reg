# CurrencyAPI Integration Guide

## Overview

The Risk Calculation module integrates with [CurrencyAPI](https://currencyapi.com/) to fetch real-time and historical exchange rates for currency conversion. This integration enables accurate conversion of exposure amounts to EUR for regulatory reporting.

## Features

- **Real-time Exchange Rates**: Fetch current exchange rates for immediate conversions
- **Historical Rates**: Access historical exchange rates for specific dates
- **Multiple Currencies**: Support for 150+ currencies worldwide
- **Automatic Retry**: Built-in retry logic for resilient API calls
- **Caching**: In-memory caching to reduce API calls and improve performance
- **Error Handling**: Comprehensive error handling with detailed error messages

## Configuration

### API Key Setup

1. Sign up for a free account at [currencyapi.com](https://currencyapi.com/)
2. Get your API key from the dashboard
3. Add the API key to your environment:

```bash
# .env file
CURRENCY_API_KEY=cur_live_uQYFLcNvMmlt34FHKxBLr7tRrEe7BV8R6GguneHl
```

### Application Configuration

The integration is configured in `application-risk-calculation.yml`:

```yaml
risk-calculation:
  currency:
    base-currency: EUR
    cache-enabled: true
    cache-ttl: 3600  # 1 hour
    
    api:
      api-key: ${CURRENCY_API_KEY}
      timeout: 30000  # 30 seconds
      retry-attempts: 3
      enabled: true
```

### Configuration Properties

| Property | Description | Default |
|----------|-------------|---------|
| `api-key` | CurrencyAPI API key | Required |
| `timeout` | Request timeout in milliseconds | 30000 |
| `retry-attempts` | Number of retry attempts on failure | 3 |
| `enabled` | Enable/disable the provider | true |

## Usage

### Basic Usage

The `CurrencyConversionService` automatically uses the CurrencyAPI provider:

```java
@Autowired
private CurrencyConversionService currencyConversionService;

public void convertAmount() {
    OriginalAmount amount = OriginalAmount.of(new BigDecimal("1000.00"));
    OriginalCurrency currency = OriginalCurrency.of("USD");
    LocalDate date = LocalDate.now();
    
    Result<AmountEur> result = currencyConversionService.convertToEur(amount, currency, date);
    
    if (result.isSuccess()) {
        AmountEur eurAmount = result.getValue().get();
        System.out.println("Converted amount: " + eurAmount.value() + " EUR");
    }
}
```

### Direct Provider Usage

You can also use the provider directly:

```java
@Autowired
private CurrencyApiExchangeRateProvider exchangeRateProvider;

public void getExchangeRate() {
    Result<ExchangeRate> result = exchangeRateProvider.getRate("USD", "EUR", LocalDate.now());
    
    if (result.isSuccess()) {
        ExchangeRate rate = result.getValue().get();
        System.out.println("Exchange rate: " + rate.rate());
    }
}
```

## API Endpoints Used

### Latest Rates
```
GET https://api.currencyapi.com/v3/latest
Parameters:
  - apikey: Your API key
  - base_currency: Source currency (e.g., USD)
  - currencies: Target currency (e.g., EUR)
```

### Historical Rates
```
GET https://api.currencyapi.com/v3/historical
Parameters:
  - apikey: Your API key
  - date: Date in YYYY-MM-DD format
  - base_currency: Source currency
  - currencies: Target currency
```

## Error Handling

The integration handles various error scenarios:

| Error Type | Description | Error Code |
|------------|-------------|------------|
| Connection Error | Failed to connect to API | `CURRENCY_API_CONNECTION_ERROR` |
| API Error | API returned error status | `CURRENCY_API_ERROR` |
| No Data | API returned empty data | `CURRENCY_API_NO_DATA` |
| Missing Rate | Requested currency not found | `CURRENCY_API_MISSING_RATE` |
| Parse Error | Failed to parse API response | `CURRENCY_API_PARSE_ERROR` |

## Caching Strategy

The integration implements two levels of caching:

1. **In-Memory Cache**: Fast access to recently used rates
2. **Spring Cache**: Configurable TTL-based caching using `@Cacheable`

Cache key format: `{fromCurrency}_{date}`

Example: `USD_2024-11-27`

## Rate Limits

CurrencyAPI free tier limits:
- 300 requests per month
- 1 request per second

For production use, consider upgrading to a paid plan.

## Testing

### Unit Tests

Run the integration tests:

```bash
mvn test -Dtest=CurrencyApiExchangeRateProviderTest
```

### Manual Testing

Test the integration using curl:

```bash
curl "https://api.currencyapi.com/v3/latest?apikey=YOUR_API_KEY&base_currency=USD&currencies=EUR"
```

## Monitoring

Monitor the integration health:

1. Check cache hit rate: `CurrencyConversionService.getCacheSize()`
2. Monitor API response times in logs
3. Track error rates for failed conversions

## Troubleshooting

### Common Issues

**Issue**: `CURRENCY_API_CONNECTION_ERROR`
- **Solution**: Check internet connectivity and API endpoint availability

**Issue**: `CURRENCY_API_ERROR` with 401 status
- **Solution**: Verify API key is correct and active

**Issue**: `CURRENCY_API_NO_DATA`
- **Solution**: Check if the requested currency is supported

**Issue**: Rate limit exceeded
- **Solution**: Implement request throttling or upgrade API plan

### Debug Logging

Enable debug logging for detailed information:

```yaml
logging:
  level:
    com.bcbs239.regtech.riskcalculation.infrastructure.external: DEBUG
```

## Production Considerations

1. **API Key Security**: Store API key in secure vault (AWS Secrets Manager, Azure Key Vault)
2. **Rate Limiting**: Implement request throttling to avoid hitting API limits
3. **Fallback Strategy**: Consider implementing a fallback to ECB rates if CurrencyAPI is unavailable
4. **Monitoring**: Set up alerts for API failures and high error rates
5. **Caching**: Use Redis for distributed caching in production

## Alternative Providers

If you need to switch providers, implement the `ExchangeRateProvider` interface:

```java
@Component
public class EcbExchangeRateProvider implements ExchangeRateProvider {
    @Override
    public Result<ExchangeRate> getRate(String from, String to, LocalDate date) {
        // Implementation for ECB API
    }
}
```

## References

- [CurrencyAPI Documentation](https://currencyapi.com/docs/)
- [CurrencyAPI Pricing](https://currencyapi.com/pricing)
- [Supported Currencies](https://currencyapi.com/docs/currencies)

## Support

For issues related to:
- **CurrencyAPI**: Contact support@currencyapi.com
- **Integration**: Create an issue in the project repository
