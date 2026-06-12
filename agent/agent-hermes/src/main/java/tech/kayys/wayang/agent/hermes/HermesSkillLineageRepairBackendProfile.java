package tech.kayys.wayang.agent.hermes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Describes a repair backend without binding Hermes to a concrete store implementation.
 */
public record HermesSkillLineageRepairBackendProfile(
        String backendId,
        String storageFamily,
        String adapterMode,
        boolean mutationSupported,
        List<String> supportedCommands,
        List<String> aliases,
        Map<String, Object> metadata) {

    public HermesSkillLineageRepairBackendProfile {
        backendId = HermesSkillLineageRepairBackend.normalizeBackendId(backendId);
        storageFamily = HermesText.oneLineOr(storageFamily, backendId);
        adapterMode = HermesText.oneLineOr(adapterMode, mutationSupported ? "configured-mutation" : "preview-only");
        supportedCommands = supportedCommands == null ? defaultCommands() : List.copyOf(supportedCommands.stream()
                .filter(command -> command != null && !command.isBlank())
                .map(HermesText::oneLine)
                .distinct()
                .toList());
        aliases = aliases == null ? List.of() : List.copyOf(aliases.stream()
                .filter(alias -> alias != null && !alias.isBlank())
                .map(HermesSkillLineageRepairBackend::normalizeBackendId)
                .distinct()
                .toList());
        metadata = HermesMetadata.copy(metadata);
    }

    public static HermesSkillLineageRepairBackendProfile configured(
            String backendId,
            boolean mutationSupported) {
        String normalized = HermesSkillLineageRepairBackend.normalizeBackendId(backendId);
        return new HermesSkillLineageRepairBackendProfile(
                normalized,
                familyFor(normalized),
                mutationSupported ? "configured-mutation" : "preview-only",
                mutationSupported,
                defaultCommands(),
                aliasesFor(normalized),
                Map.of());
    }

    public boolean supportsCommand(String command) {
        return supportedCommands.contains(HermesText.oneLine(command));
    }

    public Map<String, Object> toMetadata() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("backendId", backendId);
        values.put("storageFamily", storageFamily);
        values.put("adapterMode", adapterMode);
        values.put("mutationSupported", mutationSupported);
        values.put("supportedCommands", supportedCommands);
        values.put("aliases", aliases);
        values.put("metadata", metadata);
        return Map.copyOf(values);
    }

    static List<String> defaultCommands() {
        return List.of(
                "restore-lineage-root-definition",
                "reconcile-learned-skill-store-indexes",
                "review-refined-skill-quality",
                "bootstrap-learned-skill-library",
                "review-learned-skill-lineage");
    }

    static String familyFor(String backendId) {
        return switch (HermesSkillLineageRepairBackend.normalizeBackendId(backendId)) {
            case "database", "postgres", "postgresql", "mysql", "sqlite" -> "database";
            case "file-system", "filesystem", "local-files", "local-file" -> "file-system";
            case "object-storage", "s3", "minio", "rustfs", "r2", "gcs", "azure-blob" -> "object-storage";
            default -> "custom";
        };
    }

    static List<String> aliasesFor(String backendId) {
        return switch (familyFor(backendId)) {
            case "database" -> List.of("postgres", "postgresql", "mysql", "sqlite");
            case "file-system" -> List.of("filesystem", "local-files", "local-file");
            case "object-storage" -> List.of("object-storage", "s3", "minio", "rustfs", "r2", "gcs", "azure-blob");
            default -> List.of();
        };
    }
}
