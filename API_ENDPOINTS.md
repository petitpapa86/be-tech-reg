# RegTech Application API Endpoints - JSON Request Examples

Below is a comprehensive list of all API endpoints in the RegTech application, organized by module and controller. I've included JSON request examples for endpoints that accept request bodies, along with the HTTP method, path, and any required headers or query parameters.

## 🔐 IAM Module - User Management

### POST `/api/v1/users/register`
**Purpose**: Register a new user with Stripe payment information

```json
{
  "email": "john.doe@example.com",
  "password": "SecurePass123!",
  "firstName": "John",
  "lastName": "Doe",
  "bankId": "bank_123456789",
  "paymentMethodId": "pm_1234567890abcdef",
  "phone": "+1234567890",
  "address": {
    "line1": "123 Main Street",
    "line2": "Apt 4B",
    "city": "New York",
    "state": "NY",
    "postalCode": "10001",
    "country": "US"
  }
}
```

---

## 💳 Billing Module - Payment Processing

### POST `/api/v1/billing/process-payment`
**Purpose**: Process payment information during user registration

```json
{
  "paymentMethodId": "pm_1234567890abcdef",
  "correlationId": "user-reg-2024-001"
}
```

---

## 📋 Billing Module - Subscription Management

### GET `/api/v1/subscriptions/{subscriptionId}`
**Purpose**: Get subscription details by ID
- **Path Parameter**: `subscriptionId` (string)
- **No JSON body required**

### POST `/api/v1/subscriptions/{subscriptionId}/cancel`
**Purpose**: Cancel a subscription
- **Path Parameter**: `subscriptionId` (string)
- **Optional JSON body**:

```json
{
  "cancellationDate": "2024-12-31"
}
```

---

## 🪝 Billing Module - Webhook Processing

### POST `/api/v1/billing/webhooks/stripe`
**Purpose**: Process Stripe webhook events
- **Headers**: `Stripe-Signature: t=1234567890,v1=signature_here`
- **Raw JSON body** (Stripe webhook payload):

```json
{
  "id": "evt_1234567890",
  "object": "event",
  "api_version": "2020-08-27",
  "created": 1234567890,
  "data": {
    "object": {
      "id": "in_1234567890",
      "object": "invoice",
      "status": "paid",
      "customer": "cus_1234567890",
      "amount_due": 50000,
      "amount_paid": 50000,
      "currency": "eur"
    }
  },
  "livemode": false,
  "pending_webhooks": 1,
  "request": {
    "id": "req_1234567890",
    "idempotency_key": null
  },
  "type": "invoice.payment_succeeded"
}
```

### GET `/api/v1/billing/webhooks/stripe/health`
**Purpose**: Health check for webhook endpoint
- **No JSON body required**

---

## ⏰ Billing Module - Scheduling Operations

### POST `/api/v1/billing/scheduling/monthly-billing/trigger-current`
**Purpose**: Manually trigger monthly billing for current month
- **Authorization**: Requires `BILLING_ADMIN` role
- **No JSON body required**

### POST `/api/v1/billing/scheduling/monthly-billing/trigger-previous`
**Purpose**: Manually trigger monthly billing for previous month
- **Authorization**: Requires `BILLING_ADMIN` role
- **No JSON body required**

### POST `/api/v1/billing/scheduling/monthly-billing/trigger/{year}/{month}`
**Purpose**: Manually trigger monthly billing for specific month
- **Path Parameters**: `year` (int), `month` (int)
- **Authorization**: Requires `BILLING_ADMIN` role
- **No JSON body required**

### POST `/api/v1/billing/scheduling/dunning-process/trigger`
**Purpose**: Manually trigger dunning process
- **Authorization**: Requires `BILLING_ADMIN` role
- **No JSON body required**

### GET `/api/v1/billing/scheduling/dunning-process/statistics`
**Purpose**: Get dunning process statistics
- **Authorization**: Requires `BILLING_ADMIN` or `BILLING_VIEWER` role
- **No JSON body required**

### POST `/api/v1/billing/scheduling/dunning-process/resolve/{invoiceId}`
**Purpose**: Resolve dunning cases for specific invoice
- **Path Parameter**: `invoiceId` (string)
- **Query Parameter**: `reason` (string, default: "Manual resolution")
- **Authorization**: Requires `BILLING_ADMIN` role
- **No JSON body required**

### GET `/api/v1/billing/scheduling/status`
**Purpose**: Get scheduling status and configuration
- **Authorization**: Requires `BILLING_ADMIN` or `BILLING_VIEWER` role
- **No JSON body required**

---

## 📊 Billing Module - Monitoring & Audit

### GET `/api/v1/billing/monitoring/audit/saga/{sagaId}`
**Purpose**: Get audit trail for specific saga
- **Path Parameter**: `sagaId` (string)
- **No JSON body required**

### GET `/api/v1/billing/monitoring/audit/user/{userId}`
**Purpose**: Get audit trail for specific user
- **Path Parameter**: `userId` (string)
- **No JSON body required**

### GET `/api/v1/billing/monitoring/audit/billing-account/{billingAccountId}`
**Purpose**: Get audit trail for specific billing account
- **Path Parameter**: `billingAccountId` (string)
- **No JSON body required**

### GET `/api/v1/billing/monitoring/audit/recent`
**Purpose**: Get recent saga events
- **Query Parameters**:
  - `sagaType` (string, default: "monthly-billing")
  - `hours` (int, default: 24)
- **No JSON body required**

### GET `/api/v1/billing/monitoring/audit/statistics`
**Purpose**: Get saga statistics
- **Query Parameters**:
  - `sagaType` (string, default: "monthly-billing")
  - `days` (int, default: 7)
- **No JSON body required**

### GET `/api/v1/billing/monitoring/audit/compliance-report`
**Purpose**: Generate compliance report
- **Query Parameters**:
  - `sagaType` (string, default: "monthly-billing")
  - `days` (int, default: 30)
- **No JSON body required**

### GET `/api/v1/billing/monitoring/metrics/summary`
**Purpose**: Get performance metrics summary
- **No JSON body required**

### GET `/api/v1/billing/monitoring/metrics/health`
**Purpose**: Get health status based on metrics
- **No JSON body required**

### GET `/api/v1/billing/monitoring/metrics/operation/{operationType}`
**Purpose**: Get detailed metrics for operation type
- **Path Parameter**: `operationType` (string)
- **No JSON body required**

### POST `/api/v1/billing/monitoring/metrics/reset`
**Purpose**: Reset performance metrics
- **No JSON body required**

### GET `/api/v1/billing/monitoring/audit/billing-calculations/{sagaId}`
**Purpose**: Get billing calculation audit trail
- **Path Parameter**: `sagaId` (string)
- **No JSON body required**

### GET `/api/v1/billing/monitoring/metrics/saga-performance`
**Purpose**: Get saga performance metrics
- **Query Parameters**:
  - `sagaType` (string, default: "monthly-billing")
  - `hours` (int, default: 24)
- **No JSON body required**

### GET `/api/v1/billing/monitoring/audit/detailed`
**Purpose**: Get detailed audit trail with filtering
- **Query Parameters**:
  - `sagaId` (string, optional)
  - `userId` (string, optional)
  - `billingAccountId` (string, optional)
  - `eventType` (string, optional)
  - `hours` (int, default: 24)
- **No JSON body required**

### GET `/api/v1/billing/monitoring/audit/compliance-report/detailed`
**Purpose**: Get detailed compliance report
- **Query Parameters**:
  - `sagaType` (string, default: "monthly-billing")
  - `days` (int, default: 30)
- **No JSON body required**

---

## 📋 API Usage Notes

### Authentication & Authorization
- Most billing endpoints require authentication
- Scheduling endpoints require `BILLING_ADMIN` role
- Monitoring endpoints require `BILLING_ADMIN` or `BILLING_VIEWER` roles

### Common Headers
```http
Content-Type: application/json
Authorization: Bearer <your-jwt-token>
```

### Response Format
All endpoints return responses in this format:
```json
{
  "success": true,
  "data": { ... },
  "message": "Operation completed successfully",
  "timestamp": "2024-10-09T12:00:00Z"
}
```

### Error Response Format
```json
{
  "success": false,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Field 'email': must not be empty",
    "messageKey": "validation.field.error",
    "fieldErrors": [
      {
        "field": "email",
        "code": "NOT_BLANK",
        "message": "must not be empty"
      }
    ]
  },
  "timestamp": "2024-10-09T12:00:00Z"
}
```

### Testing the Endpoints
The application is currently running on the default Spring Boot port (8080). You can test these endpoints using tools like:
- **curl**
- **Postman**
- **HTTPie**
- **Insomnia**

Example curl command:
```bash
curl -X POST http://localhost:8080/api/v1/users/register \
  -H "Content-Type: application/json" \
  -d @user-registration.json
```</content>
<parameter name="filePath">c:\Users\alseny\Desktop\react projects\regtech\API_ENDPOINTS.md