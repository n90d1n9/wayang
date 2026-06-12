package tech.kayys.wayang.agent.hermes;

import tech.kayys.wayang.agent.skills.management.JdbcSkillArtifactStore;
import tech.kayys.wayang.agent.skills.management.JdbcSkillDefinitionStore;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;

/**
 * Runtime options for resolving concrete learned-skill persistence adapters.
 */
public record HermesLearnedSkillPersistenceAdapterResolverOptions(
        Path fileSystemRoot,
        Path fileSystemDefinitionDirectory,
        Path fileSystemArtifactDirectory,
        String objectStorageRootPrefix,
        String objectStorageDefinitionPrefix,
        String objectStorageArtifactPrefix,
        String jdbcDefinitionTableName,
        String jdbcArtifactTableName,
        boolean jdbcInitializeSchema) {

    public static final Path DEFAULT_FILE_SYSTEM_ROOT = Path.of("var/hermes/learned-skills");
    public static final String DEFAULT_OBJECT_STORAGE_ROOT_PREFIX = "hermes/learned-skills";

    public HermesLearnedSkillPersistenceAdapterResolverOptions {
        fileSystemRoot = fileSystemRoot == null ? DEFAULT_FILE_SYSTEM_ROOT : fileSystemRoot;
        fileSystemDefinitionDirectory = fileSystemDefinitionDirectory == null
                ? fileSystemRoot.resolve("definitions")
                : fileSystemDefinitionDirectory;
        fileSystemArtifactDirectory = fileSystemArtifactDirectory == null
                ? fileSystemRoot.resolve("artifacts")
                : fileSystemArtifactDirectory;
        objectStorageRootPrefix = HermesText.trimOr(
                objectStorageRootPrefix,
                DEFAULT_OBJECT_STORAGE_ROOT_PREFIX);
        objectStorageDefinitionPrefix = HermesText.trimOr(
                objectStorageDefinitionPrefix,
                objectStorageRootPrefix + "/definitions");
        objectStorageArtifactPrefix = HermesText.trimOr(
                objectStorageArtifactPrefix,
                objectStorageRootPrefix + "/artifacts");
        jdbcDefinitionTableName = HermesText.trimOr(
                jdbcDefinitionTableName,
                JdbcSkillDefinitionStore.DEFAULT_TABLE_NAME);
        jdbcArtifactTableName = HermesText.trimOr(
                jdbcArtifactTableName,
                JdbcSkillArtifactStore.DEFAULT_TABLE_NAME);
    }

    public HermesLearnedSkillPersistenceAdapterResolverOptions(
            Path fileSystemRoot,
            Path fileSystemDefinitionDirectory,
            Path fileSystemArtifactDirectory) {
        this(
                fileSystemRoot,
                fileSystemDefinitionDirectory,
                fileSystemArtifactDirectory,
                null,
                null,
                null,
                null,
                null,
                true);
    }

    public static HermesLearnedSkillPersistenceAdapterResolverOptions defaults() {
        return new HermesLearnedSkillPersistenceAdapterResolverOptions(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                true);
    }

    public static HermesLearnedSkillPersistenceAdapterResolverOptions fromHints(Map<String, String> hints) {
        String fileRoot = HermesSkillPersistenceHintKeys.fileRoot(hints);
        String objectRoot = HermesSkillPersistenceHintKeys.objectPrefix(hints);
        String definitionObjectPrefix = HermesSkillPersistenceHintKeys.definitionObjectPrefix(hints);
        String artifactObjectPrefix = HermesSkillPersistenceHintKeys.artifactObjectPrefix(hints);
        String definitionTable = HermesSkillPersistenceHintKeys.jdbcDefinitionTableName(hints);
        String artifactTable = HermesSkillPersistenceHintKeys.jdbcArtifactTableName(hints);
        String initializeSchema = HermesSkillPersistenceHintKeys.jdbcInitializeSchema(hints);
        return new HermesLearnedSkillPersistenceAdapterResolverOptions(
                fileRoot.isBlank() ? null : Path.of(fileRoot),
                null,
                null,
                objectRoot.isBlank() ? null : objectRoot,
                definitionObjectPrefix.isBlank() ? null : definitionObjectPrefix,
                artifactObjectPrefix.isBlank() ? null : artifactObjectPrefix,
                definitionTable.isBlank() ? null : definitionTable,
                artifactTable.isBlank() ? null : artifactTable,
                booleanHint(initializeSchema, true));
    }

    private static boolean booleanHint(String value, boolean fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "true", "yes", "y", "1", "on" -> true;
            case "false", "no", "n", "0", "off" -> false;
            default -> fallback;
        };
    }
}
