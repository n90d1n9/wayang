package tech.kayys.wayang.agent.skills.management;

import java.util.Set;

/**
 * Shared aliases for deployable skill store backend kinds.
 */
final class SkillStoreConfigKindAliases {

    private static final Set<String> DEFINITION_REGISTRY = Set.of("registry", "memory", "inmemory");
    private static final Set<String> LIFECYCLE_MEMORY = Set.of("memory", "inmemory", "volatile", "transient");
    private static final Set<String> EVENT_NONE = Set.of("", "none", "noop", "off", "disabled");
    private static final Set<String> EVENT_MEMORY = Set.of("memory", "inmemory", "local");
    private static final Set<String> FILESYSTEM = Set.of("filesystem", "file", "files", "fs", "local");
    private static final Set<String> OBJECT_STORAGE = Set.of(
            "object",
            "objectstore",
            "objectstorage",
            "cloud",
            "s3",
            "s3compat",
            "s3compatible",
            "rustfs",
            "minio");
    private static final Set<String> JDBC = Set.of("jdbc", "database", "db", "sql");
    private static final Set<String> CUSTOM = Set.of("custom", "external");
    private static final Set<String> HYBRID = Set.of("hybrid", "composite", "primaryfallback");
    private static final Set<String> MIRRORED = Set.of("mirror", "mirrored", "replica", "replicated", "dualwrite");
    private static final Set<String> EVENT_HYBRID = Set.of("fanout");

    private SkillStoreConfigKindAliases() {
    }

    static boolean isDefinitionRegistry(String value) {
        return matches(DEFINITION_REGISTRY, value);
    }

    static boolean isLifecycleMemory(String value) {
        return matches(LIFECYCLE_MEMORY, value);
    }

    static boolean isArtifactMemory(String value) {
        return matches(LIFECYCLE_MEMORY, value);
    }

    static boolean isEventNone(String value) {
        return matches(EVENT_NONE, value);
    }

    static boolean isEventMemory(String value) {
        return matches(EVENT_MEMORY, value);
    }

    static boolean isFilesystem(String value) {
        return matches(FILESYSTEM, value);
    }

    static boolean isObjectStorage(String value) {
        return matches(OBJECT_STORAGE, value);
    }

    static boolean isJdbc(String value) {
        return matches(JDBC, value);
    }

    static boolean isCustom(String value) {
        return matches(CUSTOM, value);
    }

    static boolean isHybrid(String value) {
        return matches(HYBRID, value);
    }

    static boolean isMirrored(String value) {
        return matches(MIRRORED, value);
    }

    static boolean isEventHybrid(String value) {
        return isHybrid(value) || matches(EVENT_HYBRID, value);
    }

    static boolean isEventMirrored(String value) {
        return isMirrored(value);
    }

    private static boolean matches(Set<String> aliases, String value) {
        return aliases.contains(SkillStoreConfigValues.normalize(value));
    }
}
