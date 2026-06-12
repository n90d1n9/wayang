package tech.kayys.wayang.agent.skills.management;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

/**
 * Expands named skill-management persistence profiles into service configs.
 */
public final class SkillManagementServiceProfiles {

    static final String PROFILE_PREFIX = "wayang.skills.";
    static final String PROFILE_ENV = "WAYANG_SKILLS_PROFILE";
    static final String SERVICE_PROFILE_ENV = "WAYANG_SKILLS_SERVICE_PROFILE";
    static final String PERSISTENCE_PROFILE_ENV = "WAYANG_SKILLS_PERSISTENCE_PROFILE";

    private static final List<SkillManagementServiceProfileDescriptor> PROFILES = List.of(
            descriptor(
                    SkillManagementServiceProfile.DEFAULT,
                    List.of("runtime", "registry", "memory", "ephemeral", "dev", "development"),
                    "Ephemeral runtime profile using registry definitions, memory state, disabled events, and memory artifacts."),
            descriptor(
                    SkillManagementServiceProfile.LOCAL_FILESYSTEM,
                    List.of("local", "filesystem", "file", "files", "disk"),
                    "Durable local-file profile using the configured skills base directory."),
            descriptor(
                    SkillManagementServiceProfile.OBJECT_STORAGE,
                    List.of("object", "s3", "rustfs", "cloud", "cloud-storage"),
                    "Durable S3/RustFS-compatible object-storage profile using the configured object prefix."),
            descriptor(
                    SkillManagementServiceProfile.JDBC,
                    List.of("database", "db", "sql", "postgres", "postgresql"),
                    "Durable database profile using JDBC-backed definition, lifecycle, event, and artifact tables."),
            descriptor(
                    SkillManagementServiceProfile.HYBRID_OBJECT_FILE,
                    List.of("hybrid", "object-file", "object-with-file-fallback", "cloud-file-fallback"),
                    "Durable hybrid profile that prefers object storage and falls back to local files."),
            descriptor(
                    SkillManagementServiceProfile.MIRRORED_OBJECT_FILE,
                    List.of("mirrored", "mirror", "replicated", "object-file-mirror", "cloud-file-mirror"),
                    "Durable mirrored profile that writes object storage and local files together."));

    private SkillManagementServiceProfiles() {
    }

    public static SkillManagementServiceConfig config(String profileName) {
        return config(profile(profileName), SkillManagementServiceProfileOptions.defaults());
    }

    public static SkillManagementServiceConfig config(SkillManagementServiceProfile profile) {
        return config(profile, SkillManagementServiceProfileOptions.defaults());
    }

    public static List<SkillManagementServiceProfileDescriptor> profiles() {
        return PROFILES;
    }

    public static SkillManagementServiceProfileDescriptor profileDescriptor(String profileName) {
        SkillManagementServiceProfile profile = profile(profileName);
        return profileDescriptor(profile);
    }

    public static SkillManagementServiceProfileDescriptor profileDescriptor(
            SkillManagementServiceProfile profile) {
        SkillManagementServiceProfile resolvedProfile = profile == null
                ? SkillManagementServiceProfile.DEFAULT
                : profile;
        return PROFILES.stream()
                .filter(descriptor -> descriptor.profile() == resolvedProfile)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown skill-management service profile: " + resolvedProfile));
    }

    public static SkillManagementServiceConfig config(
            SkillManagementServiceProfile profile,
            SkillManagementServiceProfileOptions options) {
        SkillManagementServiceProfile resolvedProfile = profile == null
                ? SkillManagementServiceProfile.DEFAULT
                : profile;
        SkillManagementServiceProfileOptions resolvedOptions = options == null
                ? SkillManagementServiceProfileOptions.defaults()
                : options;
        return switch (resolvedProfile) {
            case DEFAULT -> SkillManagementServiceConfig.of(
                    SkillDefinitionStoreConfig.registry(),
                    SkillLifecycleStateStoreConfig.memory(),
                    SkillManagementEventStoreConfig.none(),
                    SkillArtifactStoreConfig.memory(),
                    resolvedOptions.lifecycleStateReconcileOptions());
            case LOCAL_FILESYSTEM -> SkillManagementServiceConfig.of(
                    SkillDefinitionStoreConfig.fileSystem(resolvedOptions.definitionDirectory()),
                    SkillLifecycleStateStoreConfig.fileSystem(resolvedOptions.lifecycleDirectory()),
                    SkillManagementEventStoreConfig.fileSystem(
                            resolvedOptions.eventDirectory(),
                            resolvedOptions.maxEvents()),
                    SkillArtifactStoreConfig.fileSystem(resolvedOptions.artifactDirectory()),
                    resolvedOptions.lifecycleStateReconcileOptions());
            case OBJECT_STORAGE -> SkillManagementServiceConfig.of(
                    SkillDefinitionStoreConfig.objectStorage(resolvedOptions.definitionObjectPrefix()),
                    SkillLifecycleStateStoreConfig.objectStorage(resolvedOptions.lifecycleObjectPrefix()),
                    SkillManagementEventStoreConfig.objectStorage(
                            resolvedOptions.eventObjectPrefix(),
                            resolvedOptions.maxEvents()),
                    SkillArtifactStoreConfig.objectStorage(resolvedOptions.artifactObjectPrefix()),
                    resolvedOptions.lifecycleStateReconcileOptions());
            case JDBC -> SkillManagementServiceConfig.of(
                    SkillDefinitionStoreConfig.jdbc(
                            JdbcSkillDefinitionStore.DEFAULT_TABLE_NAME,
                            resolvedOptions.initializeJdbcSchema()),
                    SkillLifecycleStateStoreConfig.jdbc(
                            JdbcSkillLifecycleStateStore.DEFAULT_TABLE_NAME,
                            resolvedOptions.initializeJdbcSchema()),
                    SkillManagementEventStoreConfig.jdbc(
                            JdbcSkillManagementEventStore.DEFAULT_TABLE_NAME,
                            resolvedOptions.initializeJdbcSchema(),
                            resolvedOptions.maxEvents()),
                    SkillArtifactStoreConfig.jdbc(
                            JdbcSkillArtifactStore.DEFAULT_TABLE_NAME,
                            resolvedOptions.initializeJdbcSchema()),
                    resolvedOptions.lifecycleStateReconcileOptions());
            case HYBRID_OBJECT_FILE -> SkillManagementServiceConfig.of(
                    SkillDefinitionStoreConfig.hybrid(
                            SkillDefinitionStoreConfig.objectStorage(resolvedOptions.definitionObjectPrefix()),
                            SkillDefinitionStoreConfig.fileSystem(resolvedOptions.definitionDirectory())),
                    SkillLifecycleStateStoreConfig.hybrid(
                            SkillLifecycleStateStoreConfig.objectStorage(resolvedOptions.lifecycleObjectPrefix()),
                            SkillLifecycleStateStoreConfig.fileSystem(resolvedOptions.lifecycleDirectory())),
                    SkillManagementEventStoreConfig.hybrid(
                            SkillManagementEventStoreConfig.objectStorage(
                                    resolvedOptions.eventObjectPrefix(),
                                    resolvedOptions.maxEvents()),
                            SkillManagementEventStoreConfig.fileSystem(
                                    resolvedOptions.eventDirectory(),
                                    resolvedOptions.maxEvents())),
                    SkillArtifactStoreConfig.hybrid(
                            SkillArtifactStoreConfig.objectStorage(resolvedOptions.artifactObjectPrefix()),
                            SkillArtifactStoreConfig.fileSystem(resolvedOptions.artifactDirectory())),
                    resolvedOptions.lifecycleStateReconcileOptions());
            case MIRRORED_OBJECT_FILE -> SkillManagementServiceConfig.of(
                    SkillDefinitionStoreConfig.mirrored(
                            SkillDefinitionStoreConfig.objectStorage(resolvedOptions.definitionObjectPrefix()),
                            SkillDefinitionStoreConfig.fileSystem(resolvedOptions.definitionDirectory())),
                    SkillLifecycleStateStoreConfig.mirrored(
                            SkillLifecycleStateStoreConfig.objectStorage(resolvedOptions.lifecycleObjectPrefix()),
                            SkillLifecycleStateStoreConfig.fileSystem(resolvedOptions.lifecycleDirectory())),
                    SkillManagementEventStoreConfig.mirrored(
                            SkillManagementEventStoreConfig.objectStorage(
                                    resolvedOptions.eventObjectPrefix(),
                                    resolvedOptions.maxEvents()),
                            SkillManagementEventStoreConfig.fileSystem(
                                    resolvedOptions.eventDirectory(),
                                    resolvedOptions.maxEvents())),
                    SkillArtifactStoreConfig.mirrored(
                            SkillArtifactStoreConfig.objectStorage(resolvedOptions.artifactObjectPrefix()),
                            SkillArtifactStoreConfig.fileSystem(resolvedOptions.artifactDirectory())),
                    resolvedOptions.lifecycleStateReconcileOptions());
        };
    }

    public static SkillManagementServiceConfig fromProperties(Properties properties) {
        return fromMap(SkillStoreConfigValues.fromProperties(properties));
    }

    public static SkillManagementServiceConfig fromMap(Map<String, ?> values) {
        return fromNormalizedMap(SkillStoreConfigValues.flattenAndNormalize(values));
    }

    public static SkillManagementServiceConfig fromEnvironment(Map<String, String> environment) {
        return fromMap(environmentValues(environment));
    }

    public static SkillManagementServiceProfile profile(String profileName) {
        String normalized = SkillStoreConfigValues.normalize(profileName);
        return PROFILES.stream()
                .filter(descriptor -> matches(descriptor, normalized))
                .map(SkillManagementServiceProfileDescriptor::profile)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown skill-management service profile: " + profileName));
    }

    static Optional<String> profileName(Map<String, String> values) {
        SkillStoreConfigValues.ScopedValues scoped =
                new SkillStoreConfigValues.ScopedValues(values, PROFILE_PREFIX);
        return scoped.get(
                "profile",
                "runtime.profile",
                "service.profile",
                "persistence.profile");
    }

    static boolean hasProfile(Map<String, String> values) {
        return profileName(values).isPresent();
    }

    static SkillManagementServiceConfig fromNormalizedMap(Map<String, String> values) {
        return config(
                profile(profileName(values).orElse("default")),
                SkillManagementServiceProfileOptions.fromNormalizedMap(values));
    }

    static Map<String, Object> environmentValues(Map<String, String> environment) {
        Map<String, Object> values = SkillStoreConfigValues.fromEnvironment(
                environment,
                SkillManagementServiceProfileOptions.ENV_PREFIX,
                SkillManagementServiceProfileOptions.PREFIX);
        putEnvironmentAlias(values, environment, PROFILE_ENV, "wayang.skills.profile");
        putEnvironmentAlias(values, environment, SERVICE_PROFILE_ENV, "wayang.skills.service.profile");
        putEnvironmentAlias(values, environment, PERSISTENCE_PROFILE_ENV, "wayang.skills.persistence.profile");
        return values;
    }

    private static void putEnvironmentAlias(
            Map<String, Object> values,
            Map<String, String> environment,
            String environmentKey,
            String propertyKey) {
        String value = environment.get(environmentKey);
        if (value != null) {
            values.put(propertyKey, value);
        }
    }

    private static SkillManagementServiceProfileDescriptor descriptor(
            SkillManagementServiceProfile profile,
            List<String> aliases,
            String description) {
        return new SkillManagementServiceProfileDescriptor(
                profile,
                profile.label(),
                aliases,
                description);
    }

    private static boolean matches(
            SkillManagementServiceProfileDescriptor descriptor,
            String normalized) {
        return normalized.isBlank()
                || normalized.equals(SkillStoreConfigValues.normalize(descriptor.label()))
                || normalized.equals(SkillStoreConfigValues.normalize(descriptor.profile().name()))
                || descriptor.aliases().stream()
                        .map(SkillStoreConfigValues::normalize)
                        .anyMatch(normalized::equals);
    }
}
