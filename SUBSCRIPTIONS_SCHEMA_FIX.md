# Subscriptions Table Schema Fix

## Problem Summary

The `billing.subscriptions` table schema (created by V20 migration) doesn't match the current `SubscriptionEntity` JPA entity, causing constraint violations when creating subscriptions during user registration.

### Error Message
```
ERRORE: il valore nullo nella colonna "plan_id" della relazione "subscriptions" viola il vincolo non nullo
(ERROR: null value in column "plan_id" of relation "subscriptions" violates not-null constraint)
```

### Root Cause

**Database Schema (V20 - OUTDATED):**
```sql
CREATE TABLE billing.subscriptions (
    id VARCHAR(36) PRIMARY KEY,
    billing_account_id VARCHAR(36) NOT NULL,
    stripe_subscription_id VARCHAR(255),
    plan_id VARCHAR(100) NOT NULL,  -- ❌ DOESN'T EXIST IN ENTITY
    status VARCHAR(20) NOT NULL,
    current_period_start TIMESTAMP,  -- ❌ DOESN'T EXIST IN ENTITY
    current_period_end TIMESTAMP,    -- ❌ DOESN'T EXIST IN ENTITY
    cancel_at_period_end BOOLEAN,    -- ❌ DOESN'T EXIST IN ENTITY
    ...
);
```

**JPA Entity (SubscriptionEntity - CURRENT):**
```java
@Entity
@Table(name = "subscriptions", schema = "billing")
public class SubscriptionEntity {
    @Id
    private String id;
    
    @Column(name = "billing_account_id", nullable = true)  // ✅ Nullable
    private String billingAccountId;
    
    @Column(name = "stripe_subscription_id", nullable = false)
    private String stripeSubscriptionId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "tier", nullable = false)  // ✅ Uses tier enum
    private SubscriptionTier tier;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SubscriptionStatus status;
    
    @Column(name = "start_date", nullable = false)  // ✅ LocalDate
    private LocalDate startDate;
    
    @Column(name = "end_date")  // ✅ LocalDate
    private LocalDate endDate;
    
    // No plan_id, current_period_start, current_period_end, cancel_at_period_end
}
```

### Impact

When `UserRegisteredEventHandler` creates a default subscription via `BillingAccount.withDefaults()`:
1. Creates `Subscription` with `tier=STARTER` and `stripeSubscriptionId="default"`
2. Tries to save via `subscriptionRepository.save(subscription)`
3. Hibernate generates INSERT with columns from entity (tier, start_date, end_date)
4. Database rejects INSERT because `plan_id` column is NOT NULL but not provided

## Solution

Created migration **V21__Update_subscriptions_schema.sql** that:
1. Drops the old `billing.subscriptions` table (CASCADE to handle foreign keys)
2. Recreates table with correct schema matching `SubscriptionEntity`
3. Recreates indexes
4. Adds documentation comments

### New Schema (V21)
```sql
CREATE TABLE billing.subscriptions (
    id VARCHAR(36) PRIMARY KEY,
    billing_account_id VARCHAR(36),  -- Now nullable
    stripe_subscription_id VARCHAR(255) NOT NULL,
    tier VARCHAR(20) NOT NULL,  -- Replaces plan_id
    status VARCHAR(20) NOT NULL,
    start_date DATE NOT NULL,  -- Replaces current_period_start
    end_date DATE,  -- Replaces current_period_end
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    FOREIGN KEY (billing_account_id) REFERENCES billing.billing_accounts(id) ON DELETE CASCADE
);
```

## How to Apply the Fix

### Option 1: Manual SQL Execution (Recommended for Development)

1. **Open your database client** (DBeaver, pgAdmin, psql)

2. **Connect to your PostgreSQL database**

3. **Execute the V21 migration SQL:**
   ```sql
   -- Copy and paste the entire content of:
   -- regtech-app/src/main/resources/db/migration/billing/V21__Update_subscriptions_schema.sql
   ```

4. **Update Flyway history** to record V21 as applied:
   ```sql
   INSERT INTO flyway_schema_history (
       installed_rank, 
       version, 
       description, 
       type, 
       script, 
       checksum, 
       installed_by, 
       installed_on, 
       execution_time, 
       success
   ) VALUES (
       (SELECT COALESCE(MAX(installed_rank), 0) + 1 FROM flyway_schema_history),
       '21',
       'Update subscriptions schema',
       'SQL',
       'V21__Update_subscriptions_schema.sql',
       NULL,
       CURRENT_USER,
       CURRENT_TIMESTAMP,
       0,
       true
   );
   ```

5. **Verify the schema:**
   ```sql
   -- Check subscriptions table structure
   \d billing.subscriptions
   
   -- Or in DBeaver/pgAdmin, view table structure
   SELECT column_name, data_type, is_nullable, column_default
   FROM information_schema.columns
   WHERE table_schema = 'billing' 
     AND table_name = 'subscriptions'
   ORDER BY ordinal_position;
   ```

6. **Restart your application** and test user registration

### Option 2: Flyway Clean and Migrate (⚠️ DESTROYS ALL DATA)

**WARNING: This will delete ALL data in your database!**

Only use in development environments where data loss is acceptable.

```bash
# Windows CMD
mvnw flyway:clean flyway:migrate

# Windows PowerShell
.\mvnw flyway:clean flyway:migrate

# Linux/Mac
./mvnw flyway:clean flyway:migrate
```

### Option 3: Let Application Auto-Apply (May Not Work)

If Flyway is configured to auto-migrate on startup:
1. Restart the application
2. Flyway should detect and apply V21 automatically
3. Check logs for migration success

**Note:** This may fail if there are existing subscriptions with data that can't be migrated.

## Verification Steps

After applying the migration:

1. **Check table schema:**
   ```sql
   SELECT column_name, data_type, is_nullable
   FROM information_schema.columns
   WHERE table_schema = 'billing' AND table_name = 'subscriptions'
   ORDER BY ordinal_position;
   ```

   Expected columns:
   - id (varchar, NOT NULL)
   - billing_account_id (varchar, NULL)
   - stripe_subscription_id (varchar, NOT NULL)
   - tier (varchar, NOT NULL)
   - status (varchar, NOT NULL)
   - start_date (date, NOT NULL)
   - end_date (date, NULL)
   - created_at (timestamp, NOT NULL)
   - updated_at (timestamp, NOT NULL)
   - version (bigint, NOT NULL)

2. **Check Flyway history:**
   ```sql
   SELECT version, description, installed_on, success
   FROM flyway_schema_history
   ORDER BY installed_rank DESC
   LIMIT 5;
   ```

   Should show V21 as successfully applied.

3. **Test user registration:**
   - Register a new user via the API
   - Check application logs for successful subscription creation
   - Verify subscription record in database:
     ```sql
     SELECT * FROM billing.subscriptions ORDER BY created_at DESC LIMIT 1;
     ```

## Related Files

- **Migration:** `regtech-app/src/main/resources/db/migration/billing/V21__Update_subscriptions_schema.sql`
- **Entity:** `regtech-billing/infrastructure/src/main/java/com/bcbs239/regtech/billing/infrastructure/database/entities/SubscriptionEntity.java`
- **Domain:** `regtech-billing/domain/src/main/java/com/bcbs239/regtech/billing/domain/subscriptions/Subscription.java`
- **Handler:** `regtech-billing/application/src/main/java/com/bcbs239/regtech/billing/application/integration/UserRegisteredEventHandler.java`
- **Old Schema:** `regtech-app/src/main/resources/db/migration/billing/V20__create_billing_tables.sql`

## Next Steps After Fix

Once V21 is applied and user registration works:

1. ✅ Users can register successfully
2. ✅ Default STARTER subscription is created
3. ✅ Billing account is created with PENDING_VERIFICATION status
4. ✅ PaymentVerificationSaga starts to verify payment method
5. ⚠️ **Still need to apply V6 migration** for outbox_messages schema fix (see APPLY_MIGRATION_NOW.md)

## Notes

- **Data Loss:** V21 drops and recreates the subscriptions table, so any existing subscription data will be lost
- **Development Only:** This approach is suitable for development. For production, you'd need a data migration strategy
- **Cascade Effect:** Dropping subscriptions table will also affect any tables with foreign keys to it (invoices table has subscription_id FK)
- **Tier Values:** Valid tier values are: STARTER, PROFESSIONAL, ENTERPRISE
- **Status Values:** Valid status values are: PENDING, ACTIVE, PAST_DUE, PAUSED, CANCELLED
