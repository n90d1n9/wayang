package tech.kayys.wayang.agent.skills.management;

/**
 * Shared key aliases for deployable skill-management store configuration.
 */
final class SkillStoreConfigKeys {

    private SkillStoreConfigKeys() {
    }

    static String storeKind(SkillStoreConfigValues.ScopedValues scoped, String defaultKind) {
        return scoped.get("kind", "type", "backend").orElse(defaultKind);
    }

    static String mode(SkillStoreConfigValues.ScopedValues scoped, String defaultMode) {
        return scoped.get("mode", "strategy", "policy").orElse(defaultMode);
    }

    static boolean hasMode(SkillStoreConfigValues.ScopedValues scoped) {
        return scoped.get("mode", "strategy", "policy").isPresent();
    }

    static boolean dryRun(SkillStoreConfigValues.ScopedValues scoped, boolean defaultValue) {
        return scoped.get("dryRun", "dry-run", "preview", "planOnly", "plan-only")
                .map(SkillStoreConfigValues::booleanValue)
                .orElse(defaultValue);
    }

    static String directory(SkillStoreConfigValues.ScopedValues scoped, String message) {
        return SkillStoreConfigValues.required(
                scoped,
                message,
                "directory",
                "filesystem.directory",
                "path");
    }

    static String objectPrefix(SkillStoreConfigValues.ScopedValues scoped, String defaultPrefix) {
        return scoped.get(
                "objectPrefix",
                "object-prefix",
                "object.prefix",
                "prefix")
                .orElse(defaultPrefix);
    }

    static String jdbcTableName(SkillStoreConfigValues.ScopedValues scoped, String defaultTableName) {
        return scoped.get(
                "jdbcTableName",
                "jdbc.tableName",
                "jdbc.table-name",
                "jdbc.table",
                "tableName",
                "table")
                .orElse(defaultTableName);
    }

    static boolean initializeJdbcSchema(SkillStoreConfigValues.ScopedValues scoped) {
        return SkillStoreConfigValues.booleanValue(scoped.get(
                "initializeJdbcSchema",
                "jdbc.initializeSchema",
                "jdbc.initialize-schema",
                "jdbc.initialize.schema",
                "initializeSchema",
                "initialize-schema")
                .orElse("true"));
    }

    static String customStoreName(SkillStoreConfigValues.ScopedValues scoped, String message) {
        return SkillStoreConfigValues.required(
                scoped,
                message,
                "customStoreName",
                "custom-store",
                "custom.name",
                "name");
    }

    static int eventMaxEvents(
            SkillStoreConfigValues.ScopedValues scoped,
            int defaultMaxEvents,
            String errorMessage) {
        return integer(
                scoped.get(
                        "maxEvents",
                        "max-events",
                        "max.events",
                        "retention",
                        "limit")
                        .orElse(String.valueOf(defaultMaxEvents)),
                errorMessage);
    }

    static int keepLatestEvents(
            SkillStoreConfigValues.ScopedValues scoped,
            int defaultKeepLatestEvents,
            String errorMessage) {
        return integer(
                scoped.get(
                        "keepLatestEvents",
                        "keep-latest-events",
                        "keepLatest",
                        "keep-latest",
                        "maxEvents",
                        "max-events",
                        "retention",
                        "limit")
                        .orElse(String.valueOf(defaultKeepLatestEvents)),
                errorMessage);
    }

    static void requirePrimaryFallback(SkillStoreConfigValues.ScopedValues scoped, String message) {
        if (!scoped.hasChild("primary") || !scoped.hasChild("fallback")) {
            throw new IllegalArgumentException(message);
        }
    }

    static String childPrefix(String prefix, String childName) {
        return SkillStoreConfigValues.normalizePrefix(prefix) + childName + ".";
    }

    private static int integer(String value, String errorMessage) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException(errorMessage + ": " + value, error);
        }
    }
}
