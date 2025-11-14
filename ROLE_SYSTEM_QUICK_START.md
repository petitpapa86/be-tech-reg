# Quick Start: Database-Driven Role System

## 🚀 Deploy & Test

### 1. Run Database Migrations
```bash
# Start the application (Flyway will run migrations automatically)
mvn spring-boot:run -pl regtech-app
```

### 2. Verify Database Setup
```sql
-- Check roles are loaded
SELECT COUNT(*) FROM iam.roles;  -- Should return 8

-- Check permissions are loaded
SELECT COUNT(*) FROM iam.role_permissions;  -- Should return 96

-- Verify a specific role's permissions
SELECT r.name, COUNT(rp.permission) as permission_count
FROM iam.roles r
JOIN iam.role_permissions rp ON r.id = rp.role_id
GROUP BY r.name;
```

### 3. Test Application Startup
- Check logs for: `"Loaded X permissions for role Y from database"`
- Verify no enum fallback warnings (unless you have custom roles)

## 🔧 Next Phase: Full Migration (Optional)

When ready to remove enum dependencies:

### Update User Assignment
```java
// Instead of:
userRole.setRole(Bcbs239Role.SYSTEM_ADMIN);

// Use:
RoleEntity role = roleRepository.findByName("SYSTEM_ADMIN");
userRole.setRoleId(role.getId());
```

### Update Domain Objects
- Modify `UserRole` to work with role IDs
- Update `TenantContext` role resolution
- Remove enum imports gradually

## 📊 Role Hierarchy Reference

| Role | Level | Key Permissions |
|------|-------|-----------------|
| VIEWER | 1 | Read-only access to reports |
| DATA_ANALYST | 2 | Data analysis and reporting |
| AUDITOR | 3 | Audit trail access |
| RISK_MANAGER | 4 | Risk assessment tools |
| COMPLIANCE_OFFICER | 5 | Compliance monitoring |
| BANK_ADMIN | 6 | Bank-level administration |
| HOLDING_COMPANY_USER | 7 | Multi-bank oversight |
| SYSTEM_ADMIN | 8 | Full system access |

## 🆘 Troubleshooting

### Migration Fails
- Check Flyway history: `SELECT * FROM flyway_schema_history;`
- Verify database connection in `application.yml`

### Permissions Not Loading
- Check database connectivity
- Verify role names match exactly (case-sensitive)
- Check application logs for initialization errors

### Enum Fallback Active
- This is normal during migration
- Indicates database loading failed, falling back to enum
- Check database setup and connectivity

## 📞 Support

- **Full Documentation**: `ROLE_MIGRATION_SUMMARY.md`
- **Role Reference**: `ROLES_REFERENCE.md`
- **Database Schema**: Migration scripts in `regtech-iam/infrastructure/src/main/resources/db/migration/`</content>
<parameter name="filePath">c:\Users\alseny\Desktop\react projects\regtech\ROLE_SYSTEM_QUICK_START.md