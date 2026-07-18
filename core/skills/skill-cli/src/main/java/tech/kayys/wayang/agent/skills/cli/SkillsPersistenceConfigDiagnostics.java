package tech.kayys.wayang.agent.skills.cli;

import tech.kayys.wayang.agent.skills.management.SkillArtifactStoreConfig;
import tech.kayys.wayang.agent.skills.management.SkillDefinitionStoreConfig;
import tech.kayys.wayang.agent.skills.management.SkillLifecycleStateReconcileOptions;
import tech.kayys.wayang.agent.skills.management.SkillLifecycleStateStoreConfig;
import tech.kayys.wayang.agent.skills.management.SkillManagementEventStoreConfig;
import tech.kayys.wayang.agent.skills.management.SkillManagementServiceConfig;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/**
 * Resolved config diagnostics for CLI status output.
 */
record SkillsPersistenceConfigDiagnostics(
        String lifecycleReconcile,
        boolean createMissingStates,
        boolean removeOrphanedStates,
        List<Store> stores) {

    SkillsPersistenceConfigDiagnostics {
        lifecycleReconcile = lifecycleReconcile == null || lifecycleReconcile.isBlank()
                ? "inspect-only"
                : lifecycleReconcile;
        stores = stores == null ? List.of() : stores.stream()
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    static SkillsPersistenceConfigDiagnostics from(SkillManagementServiceConfig config) {
        SkillManagementServiceConfig resolved = config == null
                ? SkillManagementServiceConfig.defaults()
                : config;
        SkillLifecycleStateReconcileOptions reconcile = resolved.lifecycleStateReconcileOptions();
        return new SkillsPersistenceConfigDiagnostics(
                reconcileMode(reconcile),
                reconcile.createMissingStates(),
                reconcile.removeOrphanedStates(),
                List.of(
                        definition("definition", resolved.definitionStore()),
                        lifecycle("lifecycle-state", resolved.lifecycleStateStore()),
                        event("event-history", resolved.eventStore()),
                        artifact("artifact", resolved.artifactStore())));
    }

    private static Store definition(String role, SkillDefinitionStoreConfig config) {
        return switch (config.kind()) {
            case REGISTRY -> store(role, "registry", "registry");
            case FILESYSTEM -> store(role, "filesystem", path(config.directory()));
            case OBJECT_STORAGE -> store(role, "object-storage", text(config.objectPrefix()));
            case JDBC -> store(role, "jdbc", text(config.jdbcTableName()), config.initializeJdbcSchema(), 0);
            case CUSTOM -> store(role, "custom", text(config.customStoreName()));
            case HYBRID, MIRRORED -> composed(
                    role,
                    label(config.kind()),
                    definition("primary", config.primary()),
                    definition("fallback", config.fallback()));
        };
    }

    private static Store lifecycle(String role, SkillLifecycleStateStoreConfig config) {
        return switch (config.kind()) {
            case MEMORY -> store(role, "memory", "memory");
            case FILESYSTEM -> store(role, "filesystem", path(config.directory()));
            case OBJECT_STORAGE -> store(role, "object-storage", text(config.objectPrefix()));
            case JDBC -> store(role, "jdbc", text(config.jdbcTableName()), config.initializeJdbcSchema(), 0);
            case CUSTOM -> store(role, "custom", text(config.customStoreName()));
            case HYBRID, MIRRORED -> composed(
                    role,
                    label(config.kind()),
                    lifecycle("primary", config.primary()),
                    lifecycle("fallback", config.fallback()));
        };
    }

    private static Store event(String role, SkillManagementEventStoreConfig config) {
        return switch (config.kind()) {
            case NONE -> store(role, "none", "none");
            case MEMORY -> store(role, "memory", "memory", false, config.maxEvents());
            case FILESYSTEM -> store(role, "filesystem", path(config.directory()), false, config.maxEvents());
            case OBJECT_STORAGE -> store(role, "object-storage", text(config.objectPrefix()), false, config.maxEvents());
            case JDBC -> store(role, "jdbc", text(config.jdbcTableName()), config.initializeJdbcSchema(),
                    config.maxEvents());
            case CUSTOM -> store(role, "custom", text(config.customStoreName()), false, config.maxEvents());
            case HYBRID, MIRRORED -> composed(
                    role,
                    label(config.kind()),
                    event("primary", config.primary()),
                    event("fallback", config.fallback()));
        };
    }

    private static Store artifact(String role, SkillArtifactStoreConfig config) {
        return switch (config.kind()) {
            case MEMORY -> store(role, "memory", "memory");
            case FILESYSTEM -> store(role, "filesystem", path(config.directory()));
            case OBJECT_STORAGE -> store(role, "object-storage", text(config.objectPrefix()));
            case JDBC -> store(role, "jdbc", text(config.jdbcTableName()), config.initializeJdbcSchema(), 0);
            case CUSTOM -> store(role, "custom", text(config.customStoreName()));
            case HYBRID, MIRRORED -> composed(
                    role,
                    label(config.kind()),
                    artifact("primary", config.primary()),
                    artifact("fallback", config.fallback()));
        };
    }

    private static Store composed(String role, String kind, Store primary, Store fallback) {
        return new Store(role, kind, "composed", false, 0, List.of(primary, fallback));
    }

    private static Store store(String role, String kind, String target) {
        return store(role, kind, target, false, 0);
    }

    private static Store store(
            String role,
            String kind,
            String target,
            boolean initializeJdbcSchema,
            int maxEvents) {
        return new Store(role, kind, target, initializeJdbcSchema, maxEvents, List.of());
    }

    private static String reconcileMode(SkillLifecycleStateReconcileOptions reconcile) {
        if (reconcile.createMissingStates() && reconcile.removeOrphanedStates()) {
            return "create-missing-and-remove-orphans";
        }
        if (reconcile.createMissingStates()) {
            return "create-missing";
        }
        return "inspect-only";
    }

    private static String label(Enum<?> value) {
        return value.name().toLowerCase(Locale.ROOT).replace('_', '-');
    }

    private static String path(Path path) {
        return path == null ? "" : path.toString();
    }

    private static String text(String value) {
        return value == null ? "" : value;
    }

    record Store(
            String role,
            String kind,
            String target,
            boolean initializeJdbcSchema,
            int maxEvents,
            List<Store> children) {

        Store {
            role = role == null || role.isBlank() ? "unknown" : role.trim();
            kind = kind == null || kind.isBlank() ? "unknown" : kind.trim();
            target = target == null ? "" : target.trim();
            maxEvents = Math.max(0, maxEvents);
            children = children == null ? List.of() : children.stream()
                    .filter(java.util.Objects::nonNull)
                    .toList();
        }
    }
}
