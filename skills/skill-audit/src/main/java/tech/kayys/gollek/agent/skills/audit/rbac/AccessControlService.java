package tech.kayys.gollek.agent.skills.audit.rbac;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Role-Based Access Control (RBAC) service for skill operations.
 *
 * <p>Features:
 * <ul>
 *   <li>Role-based permissions</li>
 *   <li>Per-skill access control</li>
 *   <li>Dynamic role assignment</li>
 *   <li>Permission checking</li>
 * </ul>
 */
@ApplicationScoped
public class AccessControlService {

    private final Map<String, Set<RoleType>> userRoles;
    private final Map<RoleType, Set<PermissionType>> rolePermissions;
    private final Map<String, Set<String>> skillOwners;

    public AccessControlService() {
        this.userRoles = new ConcurrentHashMap<>();
        this.rolePermissions = new ConcurrentHashMap<>();
        this.skillOwners = new ConcurrentHashMap<>();
        initializeDefaultRoles();
    }

    private void initializeDefaultRoles() {
        // ADMIN - All permissions
        rolePermissions.put(RoleType.ADMIN, EnumSet.allOf(PermissionType.class));

        // DEVELOPER - CRUD on own skills
        rolePermissions.put(RoleType.DEVELOPER, EnumSet.of(
            PermissionType.SKILL_CREATE,
            PermissionType.SKILL_READ,
            PermissionType.SKILL_UPDATE,
            PermissionType.SKILL_DELETE,
            PermissionType.SKILL_ENABLE,
            PermissionType.SKILL_DISABLE
        ));

        // USER - Read and execute
        rolePermissions.put(RoleType.USER, EnumSet.of(
            PermissionType.SKILL_READ
        ));

        // VIEWER - Read only
        rolePermissions.put(RoleType.VIEWER, EnumSet.of(
            PermissionType.SKILL_READ
        ));

        // AUDITOR - Audit logs
        rolePermissions.put(RoleType.AUDITOR, EnumSet.of(
            PermissionType.SKILL_READ,
            PermissionType.AUDIT_READ
        ));
    }

    /**
     * Assign role to user.
     */
    public void assignRole(String userId, RoleType role) {
        userRoles.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet())
                .add(role);
    }

    /**
     * Remove role from user.
     */
    public void removeRole(String userId, RoleType role) {
        Set<RoleType> roles = userRoles.get(userId);
        if (roles != null) {
            roles.remove(role);
        }
    }

    /**
     * Get user roles.
     */
    public Set<RoleType> getUserRoles(String userId) {
        return Collections.unmodifiableSet(userRoles.getOrDefault(userId, Set.of()));
    }

    /**
     * Check if user has permission.
     */
    public boolean hasPermission(String userId, PermissionType permission) {
        Set<RoleType> roles = userRoles.getOrDefault(userId, Set.of());
        return roles.stream()
                .map(rolePermissions::get)
                .filter(Objects::nonNull)
                .anyMatch(permissions -> permissions.contains(permission));
    }

    /**
     * Check if user can create skill.
     */
    public boolean canCreateSkill(String userId) {
        return hasPermission(userId, PermissionType.SKILL_CREATE);
    }

    /**
     * Check if user can read skill.
     */
    public boolean canReadSkill(String userId, String skillId) {
        // Everyone with READ permission can read
        if (hasPermission(userId, PermissionType.SKILL_READ)) {
            return true;
        }
        // Or if they own the skill
        return isSkillOwner(userId, skillId);
    }

    /**
     * Check if user can update skill.
     */
    public boolean canUpdateSkill(String userId, String skillId) {
        if (hasPermission(userId, PermissionType.SKILL_UPDATE)) {
            // Admin can update any skill
            if (hasPermission(userId, PermissionType.ADMIN_ACCESS)) {
                return true;
            }
            // Developer can update own skills
            return isSkillOwner(userId, skillId);
        }
        return false;
    }

    /**
     * Check if user can delete skill.
     */
    public boolean canDeleteSkill(String userId, String skillId) {
        if (hasPermission(userId, PermissionType.SKILL_DELETE)) {
            // Admin can delete any skill
            if (hasPermission(userId, PermissionType.ADMIN_ACCESS)) {
                return true;
            }
            // Developer can delete own skills
            return isSkillOwner(userId, skillId);
        }
        return false;
    }

    /**
     * Check if user can switch repository.
     */
    public boolean canSwitchRepository(String userId) {
        return hasPermission(userId, PermissionType.REPOSITORY_SWITCH);
    }

    /**
     * Check if user can migrate repository.
     */
    public boolean canMigrateRepository(String userId) {
        return hasPermission(userId, PermissionType.REPOSITORY_MIGRATE);
    }

    /**
     * Set skill owner.
     */
    public void setSkillOwner(String skillId, String userId) {
        skillOwners.computeIfAbsent(skillId, k -> ConcurrentHashMap.newKeySet())
                   .add(userId);
    }

    /**
     * Check if user is skill owner.
     */
    public boolean isSkillOwner(String userId, String skillId) {
        Set<String> owners = skillOwners.get(skillId);
        return owners != null && owners.contains(userId);
    }

    /**
     * Get skill owners.
     */
    public Set<String> getSkillOwners(String skillId) {
        return Collections.unmodifiableSet(
            skillOwners.getOrDefault(skillId, Set.of())
        );
    }

    /**
     * Authorize operation.
     */
    public Uni<Void> authorize(String userId, PermissionType permission) {
        return Uni.createFrom().voidItem().invoke(() -> {
            if (!hasPermission(userId, permission)) {
                throw new AccessDeniedException(
                    "User " + userId + " does not have permission " + permission
                );
            }
        });
    }

    /**
     * Access denied exception.
     */
    public static class AccessDeniedException extends RuntimeException {
        public AccessDeniedException(String message) {
            super(message);
        }
    }
}
