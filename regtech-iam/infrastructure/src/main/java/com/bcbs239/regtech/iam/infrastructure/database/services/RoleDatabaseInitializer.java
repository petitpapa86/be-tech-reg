package com.bcbs239.regtech.iam.infrastructure.database.services;

import com.bcbs239.regtech.iam.infrastructure.database.entities.RoleEntity;
import com.bcbs239.regtech.iam.infrastructure.database.entities.RolePermissionEntity;
import com.bcbs239.regtech.iam.infrastructure.database.repositories.SpringDataRolePermissionRepository;
import com.bcbs239.regtech.iam.infrastructure.database.repositories.SpringDataRoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Service for initializing and managing roles and permissions in the database.
 * Populates the database with the standard RegTech roles and their permissions.
 */
@Service
public class RoleDatabaseInitializer {

    private static final Logger logger = LoggerFactory.getLogger(RoleDatabaseInitializer.class);

    private final SpringDataRoleRepository roleRepository;
    private final SpringDataRolePermissionRepository rolePermissionRepository;

    public RoleDatabaseInitializer(
            SpringDataRoleRepository roleRepository,
            SpringDataRolePermissionRepository rolePermissionRepository) {
        this.roleRepository = roleRepository;
        this.rolePermissionRepository = rolePermissionRepository;
    }

    /**
     * Initialize the database with standard RegTech roles and permissions.
     * This method is idempotent - it can be called multiple times safely.
     */
    public void initializeRolesAndPermissions() {
        logger.info("Initializing RegTech roles and permissions in database...");

        // Check if roles are already initialized
        if (areRolesAlreadyInitialized()) {
            logger.info("Roles and permissions are already initialized, skipping initialization");
            return;
        }

        // Define all roles with their permissions
        Map<String, RoleDefinition> roles = createRoleDefinitions();

        for (Map.Entry<String, RoleDefinition> entry : roles.entrySet()) {
            String roleId = entry.getKey();
            RoleDefinition definition = entry.getValue();

            // Create role in its own transaction to avoid rollback issues
            createRoleIfNotExists(roleId, definition);
        }

        logger.info("Role and permission initialization completed");
    }

    /**
     * Create a role if it doesn't exist, in its own transaction.
     */
    @Transactional
    private void createRoleIfNotExists(String roleId, RoleDefinition definition) {
        if (roleId == null || definition == null) {
            logger.warn("Invalid roleId or definition provided, skipping");
            return;
        }

        // Check if role exists
        RoleEntity role = roleRepository.findById(roleId).orElse(null);
        if (role != null) {
            logger.debug("Role {} already exists, skipping creation", definition.name);
            return;
        }

        // Create role
        role = new RoleEntity(
            roleId,
            definition.name,
            definition.displayName,
            definition.description,
            definition.level
        );

        try {
            role = roleRepository.saveAndFlush(role);
            logger.info("Created role: {}", definition.name);
        } catch (Exception e) {
            // Handle race condition - role might have been created by another transaction
            logger.warn("Failed to create role {}: {}", definition.name, e.getMessage());
            logger.debug("Exception details", e);
            // Try to find the role again
            role = roleRepository.findById(roleId).orElse(null);
            if (role == null) {
                logger.warn("Could not create or find role: {} - skipping", definition.name);
                return;
            }
            logger.debug("Role {} was created by another transaction", definition.name);
        }

        // Update permissions for this role
        updateRolePermissions(role, definition.permissions);
    }

    /**
     * Check if roles are already initialized in the database.
     */
    private boolean areRolesAlreadyInitialized() {
        try {
            // Check if all required roles exist
            Map<String, RoleDefinition> roles = createRoleDefinitions();
            for (String roleId : roles.keySet()) {
                if (roleId != null && !roleRepository.findById(roleId).isPresent()) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            logger.debug("Error checking if roles are initialized: {}", e.getMessage());
            return false; // If we can't check, assume not initialized
        }
    }

    private void updateRolePermissions(RoleEntity role, Set<String> expectedPermissions) {
        try {
            // Get current permissions
            Set<String> currentPermissions = rolePermissionRepository.findPermissionsByRoleId(role.getId());

            // Check if permissions are already correct
            if (currentPermissions.equals(expectedPermissions)) {
                logger.debug("Permissions for role {} are already correct", role.getName());
                return;
            }

            // Find permissions to add
            Set<String> permissionsToAdd = new HashSet<>(expectedPermissions);
            permissionsToAdd.removeAll(currentPermissions);

            // Find permissions to remove
            Set<String> permissionsToRemove = new HashSet<>(currentPermissions);
            permissionsToRemove.removeAll(expectedPermissions);

            // Remove old permissions
            if (!permissionsToRemove.isEmpty()) {
                rolePermissionRepository.deleteByRoleId(role.getId());
                logger.debug("Removed {} old permissions for role {}", permissionsToRemove.size(), role.getName());
            }

            // Add new permissions
            for (String permission : permissionsToAdd) {
                String permissionId = "perm-" + role.getId() + "-" + permission.toLowerCase().replace("bcbs239_", "").replace(".", "-");
                RolePermissionEntity rolePermission = new RolePermissionEntity(permissionId, role, permission);
                rolePermissionRepository.save(rolePermission);
            }

            if (!permissionsToAdd.isEmpty()) {
                logger.debug("Added {} new permissions for role {}", permissionsToAdd.size(), role.getName());
            }
        } catch (Exception e) {
            logger.warn("Failed to update permissions for role {}: {}. Skipping permission update.", role.getName(), e.getMessage());
            // Continue - don't fail the entire initialization
        }
    }

    private Map<String, RoleDefinition> createRoleDefinitions() {
        Map<String, RoleDefinition> roles = new LinkedHashMap<>();

        // VIEWER
        roles.put("role-viewer", new RoleDefinition(
            "VIEWER", "Basic Viewer", "Can only view reports and data - read-only access for basic users", 1,
            Set.of("BCBS239_VIEW_REPORTS", "BCBS239_VIEW_VIOLATIONS")
        ));

        // DATA_ANALYST
        roles.put("role-data-analyst", new RoleDefinition(
            "DATA_ANALYST", "Data Analyst", "Can upload files and view reports - handles data processing and analysis", 2,
            Set.of("BCBS239_UPLOAD_FILES", "BCBS239_DOWNLOAD_FILES", "BCBS239_VIEW_REPORTS",
                   "BCBS239_GENERATE_REPORTS", "BCBS239_VIEW_VIOLATIONS", "BCBS239_VALIDATE_DATA")
        ));

        // AUDITOR
        roles.put("role-auditor", new RoleDefinition(
            "AUDITOR", "Auditor", "Read-only access with audit capabilities - monitors system and tracks submissions", 3,
            Set.of("BCBS239_VIEW_REPORTS", "BCBS239_EXPORT_REPORTS", "BCBS239_VIEW_VIOLATIONS",
                   "BCBS239_VIEW_AUDIT_LOGS", "BCBS239_TRACK_SUBMISSIONS", "BCBS239_MONITOR_SYSTEM")
        ));

        // RISK_MANAGER
        roles.put("role-risk-manager", new RoleDefinition(
            "RISK_MANAGER", "Risk Manager", "Can manage violations and generate reports - handles risk assessment and mitigation", 3,
            Set.of("BCBS239_UPLOAD_FILES", "BCBS239_DOWNLOAD_FILES", "BCBS239_DELETE_FILES",
                   "BCBS239_GENERATE_REPORTS", "BCBS239_VIEW_REPORTS", "BCBS239_EXPORT_REPORTS",
                   "BCBS239_SCHEDULE_REPORTS", "BCBS239_MANAGE_VIOLATIONS", "BCBS239_VIEW_VIOLATIONS",
                   "BCBS239_VALIDATE_DATA", "BCBS239_APPROVE_DATA", "BCBS239_REJECT_DATA", "BCBS239_MANAGE_TEMPLATES")
        ));

        // COMPLIANCE_OFFICER
        roles.put("role-compliance-officer", new RoleDefinition(
            "COMPLIANCE_OFFICER", "Compliance Officer", "Full compliance management capabilities - oversees regulatory compliance and reporting", 4,
            Set.of("BCBS239_UPLOAD_FILES", "BCBS239_DOWNLOAD_FILES", "BCBS239_DELETE_FILES",
                   "BCBS239_GENERATE_REPORTS", "BCBS239_VIEW_REPORTS", "BCBS239_EXPORT_REPORTS",
                   "BCBS239_SCHEDULE_REPORTS", "BCBS239_CONFIGURE_PARAMETERS", "BCBS239_MANAGE_TEMPLATES",
                   "BCBS239_CONFIGURE_WORKFLOWS", "BCBS239_MANAGE_VIOLATIONS", "BCBS239_APPROVE_VIOLATIONS",
                   "BCBS239_VIEW_VIOLATIONS", "BCBS239_VALIDATE_DATA", "BCBS239_APPROVE_DATA",
                   "BCBS239_REJECT_DATA", "BCBS239_SUBMIT_REGULATORY_REPORTS", "BCBS239_REVIEW_SUBMISSIONS",
                   "BCBS239_TRACK_SUBMISSIONS", "BCBS239_VIEW_AUDIT_LOGS")
        ));

        // BANK_ADMIN
        roles.put("role-bank-admin", new RoleDefinition(
            "BANK_ADMIN", "Bank Administrator", "Manages bank-specific configurations - administers bank-level settings and users", 4,
            Set.of("BCBS239_UPLOAD_FILES", "BCBS239_DOWNLOAD_FILES", "BCBS239_DELETE_FILES",
                   "BCBS239_GENERATE_REPORTS", "BCBS239_VIEW_REPORTS", "BCBS239_EXPORT_REPORTS",
                   "BCBS239_CONFIGURE_PARAMETERS", "BCBS239_MANAGE_TEMPLATES", "BCBS239_MANAGE_VIOLATIONS",
                   "BCBS239_VIEW_VIOLATIONS", "BCBS239_ADMINISTER_USERS", "BCBS239_ASSIGN_ROLES",
                   "BCBS239_MANAGE_BANK_CONFIG", "BCBS239_VIEW_AUDIT_LOGS")
        ));

        // HOLDING_COMPANY_USER
        roles.put("role-holding-company", new RoleDefinition(
            "HOLDING_COMPANY_USER", "Holding Company User", "Can view across multiple banks - access to consolidated data and reports", 4,
            Set.of("BCBS239_VIEW_REPORTS", "BCBS239_GENERATE_REPORTS", "BCBS239_EXPORT_REPORTS",
                   "BCBS239_VIEW_CROSS_BANK_DATA", "BCBS239_CONSOLIDATE_REPORTS", "BCBS239_VIEW_VIOLATIONS",
                   "BCBS239_TRACK_SUBMISSIONS")
        ));

        // SYSTEM_ADMIN
        roles.put("role-system-admin", new RoleDefinition(
            "SYSTEM_ADMIN", "System Administrator", "Full system access - complete administrative control over the entire system", 5,
            Set.of("BCBS239_UPLOAD_FILES", "BCBS239_DOWNLOAD_FILES", "BCBS239_DELETE_FILES",
                   "BCBS239_GENERATE_REPORTS", "BCBS239_VIEW_REPORTS", "BCBS239_EXPORT_REPORTS",
                   "BCBS239_SCHEDULE_REPORTS", "BCBS239_CONFIGURE_PARAMETERS", "BCBS239_MANAGE_TEMPLATES",
                   "BCBS239_CONFIGURE_WORKFLOWS", "BCBS239_MANAGE_VIOLATIONS", "BCBS239_APPROVE_VIOLATIONS",
                   "BCBS239_VIEW_VIOLATIONS", "BCBS239_ADMINISTER_USERS", "BCBS239_ASSIGN_ROLES",
                   "BCBS239_VIEW_AUDIT_LOGS", "BCBS239_VALIDATE_DATA", "BCBS239_APPROVE_DATA",
                   "BCBS239_REJECT_DATA", "BCBS239_MANAGE_SYSTEM_CONFIG", "BCBS239_MONITOR_SYSTEM",
                   "BCBS239_BACKUP_RESTORE", "BCBS239_SUBMIT_REGULATORY_REPORTS", "BCBS239_REVIEW_SUBMISSIONS",
                   "BCBS239_TRACK_SUBMISSIONS", "BCBS239_MANAGE_BANK_CONFIG", "BCBS239_VIEW_CROSS_BANK_DATA",
                   "BCBS239_CONSOLIDATE_REPORTS")
        ));

        return roles;
    }

    /**
     * Inner class to define a role with its properties.
     */
    private static class RoleDefinition {
        final String name;
        final String displayName;
        final String description;
        final int level;
        final Set<String> permissions;

        RoleDefinition(String name, String displayName, String description, int level, Set<String> permissions) {
            this.name = name;
            this.displayName = displayName;
            this.description = description;
            this.level = level;
            this.permissions = permissions;
        }
    }
}