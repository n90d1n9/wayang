package tech.kayys.wayang.agent.skills.management;

import java.util.List;

/**
 * Central catalog for skill-management runtime configuration discoverability.
 */
public final class SkillManagementRuntimeConfigHints {

    private static final String PROFILE_SELECTOR_DESCRIPTION =
            "Selects the named skill-management persistence profile before role-level overrides are applied.";

    private SkillManagementRuntimeConfigHints() {
    }

    public static SkillManagementRuntimeConfigCatalog catalog() {
        return new SkillManagementRuntimeConfigCatalog(List.of(
                group("runtime-sources", "runtime config sources", runtimeSources()),
                group("profile-selectors", "profile selectors", profileSelectors()),
                group("profile-options", "profile option defaults", profileOptions()),
                group("object-storage-provider", "object storage provider settings", objectStorageProvider()),
                group("store-overrides", "role store override prefixes", storeOverrides()),
                group("store-option-suffixes", "store option suffixes", storeOptionSuffixes())));
    }

    public static List<SkillManagementRuntimeConfigHint> runtimeSources() {
        return List.of(
                hint(
                        "microprofile-config",
                        "When MicroProfile Config is present, matching skill-management properties are read from it.",
                        List.of(),
                        List.of(),
                        "",
                        List.of("Only keys under skill-management property or environment prefixes are considered.")),
                hint(
                        "environment-and-system-properties",
                        "When MicroProfile Config is unavailable, environment values are merged with Java system properties.",
                        List.of(),
                        List.of(),
                        "",
                        List.of("Java system properties override environment-derived values with the same key.")));
    }

    public static List<SkillManagementRuntimeConfigHint> profileSelectors() {
        return List.of(hint(
                "profile",
                PROFILE_SELECTOR_DESCRIPTION,
                List.of(
                        "wayang.skills.profile",
                        "wayang.skills.runtime.profile",
                        "wayang.skills.service.profile",
                        "wayang.skills.persistence.profile"),
                List.of(
                        SkillManagementServiceProfiles.PROFILE_ENV,
                        SkillManagementServiceProfiles.SERVICE_PROFILE_ENV,
                        SkillManagementServiceProfiles.PERSISTENCE_PROFILE_ENV),
                "default",
                List.of("Profile aliases include local, filesystem, object, s3, rustfs, cloud, database, db, hybrid, and mirrored.")));
    }

    public static List<SkillManagementRuntimeConfigHint> profileOptions() {
        SkillManagementServiceProfileOptions defaults = SkillManagementServiceProfileOptions.defaults();
        return List.of(
                profileOption(
                        "base-directory",
                        "Base directory used by local-filesystem, hybrid, and mirrored file-backed profile roles.",
                        List.of(
                                "wayang.skills.profile.base-directory",
                                "wayang.skills.profile.directory",
                                "wayang.skills.profile.path"),
                        "WAYANG_SKILLS_PROFILE_BASE_DIRECTORY",
                        defaults.baseDirectory().toString()),
                profileOption(
                        "object-prefix",
                        "Object key prefix used by object-storage, hybrid, and mirrored cloud-backed profile roles.",
                        List.of(
                                "wayang.skills.profile.object-prefix",
                                "wayang.skills.profile.prefix"),
                        "WAYANG_SKILLS_PROFILE_OBJECT_PREFIX",
                        defaults.objectPrefix()),
                profileOption(
                        "max-events",
                        "Retention capacity used by profile-generated event-history stores.",
                        List.of(
                                "wayang.skills.profile.max-events",
                                "wayang.skills.profile.retention",
                                "wayang.skills.profile.limit"),
                        "WAYANG_SKILLS_PROFILE_MAX_EVENTS",
                        String.valueOf(defaults.maxEvents())),
                profileOption(
                        "initialize-jdbc-schema",
                        "Controls whether JDBC-backed profile stores initialize their tables.",
                        List.of(
                                "wayang.skills.profile.initialize-jdbc-schema",
                                "wayang.skills.profile.jdbc.initialize-schema"),
                        "WAYANG_SKILLS_PROFILE_INITIALIZE_JDBC_SCHEMA",
                        String.valueOf(defaults.initializeJdbcSchema())));
    }

    public static List<SkillManagementRuntimeConfigHint> objectStorageProvider() {
        return SkillManagementObjectStorageProviderConfigHints.hints();
    }

    public static List<SkillManagementRuntimeConfigHint> storeOverrides() {
        return List.of(
                storeOverride(
                        "definition",
                        "Overrides the skill definition store role.",
                        SkillDefinitionStoreConfigs.PREFIX,
                        SkillDefinitionStoreConfigs.ENV_PREFIX,
                        "registry"),
                storeOverride(
                        "lifecycle-state",
                        "Overrides the skill lifecycle state store role.",
                        SkillLifecycleStateStoreConfigs.PREFIX,
                        SkillLifecycleStateStoreConfigs.ENV_PREFIX,
                        "memory"),
                storeOverride(
                        "event-history",
                        "Overrides the skill-management event history store role.",
                        SkillManagementEventStoreConfigs.PREFIX,
                        SkillManagementEventStoreConfigs.ENV_PREFIX,
                        "none"),
                storeOverride(
                        "artifact",
                        "Overrides the skill artifact store role.",
                        SkillArtifactStoreConfigs.PREFIX,
                        SkillArtifactStoreConfigs.ENV_PREFIX,
                        "memory"),
                hint(
                        "lifecycle-reconcile",
                        "Overrides lifecycle state reconciliation behavior after the store roles are resolved.",
                        List.of(SkillLifecycleStateReconcileConfigs.PREFIX),
                        List.of(SkillLifecycleStateReconcileConfigs.ENV_PREFIX),
                        "inspect-only",
                        List.of("Supported modes include inspect-only, create-missing, and sync.")));
    }

    public static List<SkillManagementRuntimeConfigHint> storeOptionSuffixes() {
        return List.of(
                suffix(
                        "kind",
                        "Selects the concrete store kind.",
                        List.of("kind", "type", "backend"),
                        "registry, memory, none, filesystem, object-storage, jdbc, custom, hybrid, mirrored"),
                suffix(
                        "directory",
                        "Filesystem target directory for filesystem stores.",
                        List.of("directory", "filesystem.directory", "path"),
                        ""),
                suffix(
                        "object-prefix",
                        "Object key prefix for S3/RustFS-compatible stores.",
                        List.of("object-prefix", "object.prefix", "prefix"),
                        ""),
                suffix(
                        "jdbc-table-name",
                        "JDBC table name for database-backed stores.",
                        List.of("jdbc.table-name", "jdbc.table", "table-name", "table"),
                        ""),
                suffix(
                        "initialize-jdbc-schema",
                        "Controls whether JDBC-backed stores initialize tables.",
                        List.of("initialize-jdbc-schema", "jdbc.initialize-schema", "initialize-schema"),
                        "true"),
                suffix(
                        "custom-name",
                        "Name of an externally supplied custom store.",
                        List.of("custom-store", "custom.name", "name"),
                        ""),
                suffix(
                        "primary-and-fallback",
                        "Nested child prefixes required by hybrid and mirrored stores.",
                        List.of("primary.*", "fallback.*"),
                        ""),
                suffix(
                        "max-events",
                        "Event-history retention capacity for memory, filesystem, object-storage, and JDBC event stores.",
                        List.of("max-events", "max.events", "retention", "limit"),
                        String.valueOf(InMemorySkillManagementEventSink.DEFAULT_MAX_EVENTS)));
    }

    private static SkillManagementRuntimeConfigGroup group(
            String name,
            String label,
            List<SkillManagementRuntimeConfigHint> hints) {
        return new SkillManagementRuntimeConfigGroup(name, label, hints);
    }

    private static SkillManagementRuntimeConfigHint profileOption(
            String name,
            String description,
            List<String> properties,
            String environment,
            String defaultValue) {
        return hint(
                name,
                description,
                properties,
                List.of(environment),
                defaultValue,
                List.of());
    }

    private static SkillManagementRuntimeConfigHint storeOverride(
            String name,
            String description,
            String propertyPrefix,
            String environmentPrefix,
            String defaultValue) {
        return hint(
                name,
                description,
                List.of(propertyPrefix),
                List.of(environmentPrefix),
                defaultValue,
                List.of("Set the kind/type/backend suffix to replace the role generated by a selected profile."));
    }

    private static SkillManagementRuntimeConfigHint suffix(
            String name,
            String description,
            List<String> suffixes,
            String defaultValue) {
        return hint(name, description, suffixes, List.of(), defaultValue, List.of());
    }

    private static SkillManagementRuntimeConfigHint hint(
            String name,
            String description,
            List<String> properties,
            List<String> environment,
            String defaultValue,
            List<String> notes) {
        return new SkillManagementRuntimeConfigHint(
                name,
                description,
                properties,
                environment,
                defaultValue,
                notes);
    }
}
