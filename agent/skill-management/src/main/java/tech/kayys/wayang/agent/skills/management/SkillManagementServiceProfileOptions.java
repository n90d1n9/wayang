package tech.kayys.wayang.agent.skills.management;

import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

/**
 * Tunable defaults used when expanding a named skill-management service profile.
 */
public record SkillManagementServiceProfileOptions(
        Path baseDirectory,
        String objectPrefix,
        int maxEvents,
        boolean initializeJdbcSchema,
        SkillLifecycleStateReconcileOptions lifecycleStateReconcileOptions) {

    static final String PREFIX = "wayang.skills.profile.";
    static final String ENV_PREFIX = "WAYANG_SKILLS_PROFILE_";

    private static final Path DEFAULT_BASE_DIRECTORY = Path.of(".wayang", "skills");
    private static final String DEFAULT_OBJECT_PREFIX = "wayang/skills";

    public SkillManagementServiceProfileOptions {
        baseDirectory = baseDirectory == null ? DEFAULT_BASE_DIRECTORY : baseDirectory;
        objectPrefix = normalizeObjectPrefix(objectPrefix);
        maxEvents = SkillManagementEventRetention.normalizeCapacityOrDefault(
                maxEvents,
                InMemorySkillManagementEventSink.DEFAULT_MAX_EVENTS);
        lifecycleStateReconcileOptions = lifecycleStateReconcileOptions == null
                ? SkillLifecycleStateReconcileOptions.inspectOnly()
                : lifecycleStateReconcileOptions;
    }

    public static SkillManagementServiceProfileOptions defaults() {
        return new SkillManagementServiceProfileOptions(
                DEFAULT_BASE_DIRECTORY,
                DEFAULT_OBJECT_PREFIX,
                InMemorySkillManagementEventSink.DEFAULT_MAX_EVENTS,
                true,
                SkillLifecycleStateReconcileOptions.inspectOnly());
    }

    public static SkillManagementServiceProfileOptions fromMap(Map<String, ?> values) {
        return fromNormalizedMap(SkillStoreConfigValues.flattenAndNormalize(values));
    }

    static SkillManagementServiceProfileOptions fromNormalizedMap(Map<String, String> values) {
        SkillManagementServiceProfileOptions defaults = defaults();
        SkillStoreConfigValues.ScopedValues scoped =
                new SkillStoreConfigValues.ScopedValues(values, PREFIX);
        Path baseDirectory = scoped.get(
                        "baseDirectory",
                        "base-directory",
                        "base.directory",
                        "directory",
                        "path")
                .map(Path::of)
                .orElse(defaults.baseDirectory());
        String objectPrefix = scoped.get(
                        "objectPrefix",
                        "object-prefix",
                        "object.prefix",
                        "prefix")
                .orElse(defaults.objectPrefix());
        int maxEvents = scoped.get(
                        "maxEvents",
                        "max-events",
                        "max.events",
                        "retention",
                        "limit")
                .map(value -> integer(value, "Invalid profile max event count"))
                .orElse(defaults.maxEvents());
        boolean initializeJdbcSchema = scoped.get(
                        "initializeJdbcSchema",
                        "initialize-jdbc-schema",
                        "jdbc.initializeSchema",
                        "jdbc.initialize-schema",
                        "initializeSchema",
                        "initialize-schema")
                .map(SkillStoreConfigValues::booleanValue)
                .orElse(defaults.initializeJdbcSchema());
        return new SkillManagementServiceProfileOptions(
                baseDirectory,
                objectPrefix,
                maxEvents,
                initializeJdbcSchema,
                defaults.lifecycleStateReconcileOptions());
    }

    public SkillManagementServiceProfileOptions withBaseDirectory(Path baseDirectory) {
        return new SkillManagementServiceProfileOptions(
                baseDirectory,
                objectPrefix,
                maxEvents,
                initializeJdbcSchema,
                lifecycleStateReconcileOptions);
    }

    public SkillManagementServiceProfileOptions withObjectPrefix(String objectPrefix) {
        return new SkillManagementServiceProfileOptions(
                baseDirectory,
                objectPrefix,
                maxEvents,
                initializeJdbcSchema,
                lifecycleStateReconcileOptions);
    }

    public SkillManagementServiceProfileOptions withMaxEvents(int maxEvents) {
        return new SkillManagementServiceProfileOptions(
                baseDirectory,
                objectPrefix,
                maxEvents,
                initializeJdbcSchema,
                lifecycleStateReconcileOptions);
    }

    public SkillManagementServiceProfileOptions withInitializeJdbcSchema(boolean initializeJdbcSchema) {
        return new SkillManagementServiceProfileOptions(
                baseDirectory,
                objectPrefix,
                maxEvents,
                initializeJdbcSchema,
                lifecycleStateReconcileOptions);
    }

    public SkillManagementServiceProfileOptions withLifecycleStateReconcileOptions(
            SkillLifecycleStateReconcileOptions lifecycleStateReconcileOptions) {
        return new SkillManagementServiceProfileOptions(
                baseDirectory,
                objectPrefix,
                maxEvents,
                initializeJdbcSchema,
                lifecycleStateReconcileOptions);
    }

    Path definitionDirectory() {
        return baseDirectory.resolve("definitions");
    }

    Path lifecycleDirectory() {
        return baseDirectory.resolve("lifecycle");
    }

    Path eventDirectory() {
        return baseDirectory.resolve("events");
    }

    Path artifactDirectory() {
        return baseDirectory.resolve("artifacts");
    }

    String definitionObjectPrefix() {
        return objectKey("definitions");
    }

    String lifecycleObjectPrefix() {
        return objectKey("lifecycle");
    }

    String eventObjectPrefix() {
        return objectKey("events");
    }

    String artifactObjectPrefix() {
        return objectKey("artifacts");
    }

    private String objectKey(String suffix) {
        return objectPrefix + "/" + suffix;
    }

    private static String normalizeObjectPrefix(String objectPrefix) {
        String resolved = objectPrefix == null || objectPrefix.isBlank()
                ? DEFAULT_OBJECT_PREFIX
                : objectPrefix.trim();
        while (resolved.endsWith("/")) {
            resolved = resolved.substring(0, resolved.length() - 1);
        }
        return Objects.requireNonNull(resolved, "objectPrefix");
    }

    private static int integer(String value, String errorMessage) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException(errorMessage + ": " + value, error);
        }
    }
}
