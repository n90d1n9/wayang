package tech.kayys.wayang.agenticcommerce.wayang;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Operator-facing validation report for persistence configuration.
 */
public record AgenticCommerceWayangPersistenceConfigValidationReport(
        String storageKind,
        Map<String, Object> target,
        List<AgenticCommerceWayangPersistenceConfigValidationIssue> issues) {

    public AgenticCommerceWayangPersistenceConfigValidationReport {
        storageKind = AgenticCommerceWayangMaps.text(storageKind);
        target = AgenticCommerceWayangMaps.copy(target);
        issues = normalizeIssues(issues);
    }

    public static AgenticCommerceWayangPersistenceConfigValidationReport from(
            AgenticCommerceWayangPersistenceConfig config) {
        return from(config, AgenticCommerceWayangPersistenceStoreProviders.defaults());
    }

    public static AgenticCommerceWayangPersistenceConfigValidationReport from(
            AgenticCommerceWayangPersistenceConfig config,
            AgenticCommerceWayangPersistenceStoreProviders providers) {
        AgenticCommerceWayangPersistenceConfig resolved = Objects.requireNonNull(config, "config");
        AgenticCommerceWayangPersistenceStoreProviders resolvedProviders = providers == null
                ? AgenticCommerceWayangPersistenceStoreProviders.defaults()
                : providers;
        List<AgenticCommerceWayangPersistenceConfigValidationIssue> issues = new ArrayList<>();
        validate(resolved, resolvedProviders, "$", issues);
        return new AgenticCommerceWayangPersistenceConfigValidationReport(
                resolved.storageKind(),
                resolved.targetDescriptor().toMap(),
                issues);
    }

    public boolean valid() {
        return errorCount() == 0;
    }

    public int issueCount() {
        return issues.size();
    }

    public int errorCount() {
        return (int) issues.stream()
                .filter(AgenticCommerceWayangPersistenceConfigValidationIssue::error)
                .count();
    }

    public int warningCount() {
        return (int) issues.stream()
                .filter(AgenticCommerceWayangPersistenceConfigValidationIssue::warning)
                .count();
    }

    public List<String> errorCodes() {
        return issues.stream()
                .filter(AgenticCommerceWayangPersistenceConfigValidationIssue::error)
                .map(AgenticCommerceWayangPersistenceConfigValidationIssue::code)
                .toList();
    }

    public List<String> warningCodes() {
        return issues.stream()
                .filter(AgenticCommerceWayangPersistenceConfigValidationIssue::warning)
                .map(AgenticCommerceWayangPersistenceConfigValidationIssue::code)
                .toList();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("valid", valid());
        values.put("storageKind", storageKind);
        values.put("issueCount", issueCount());
        values.put("errorCount", errorCount());
        values.put("warningCount", warningCount());
        values.put("errorCodes", errorCodes());
        values.put("warningCodes", warningCodes());
        values.put("target", target);
        values.put("issues", issues.stream()
                .map(AgenticCommerceWayangPersistenceConfigValidationIssue::toMap)
                .toList());
        return Map.copyOf(values);
    }

    private static void validate(
            AgenticCommerceWayangPersistenceConfig config,
            AgenticCommerceWayangPersistenceStoreProviders providers,
            String path,
            List<AgenticCommerceWayangPersistenceConfigValidationIssue> issues) {
        if (providers.provider(config).isEmpty()) {
            issues.add(error(
                    "unsupported_storage_kind",
                    path,
                    "No persistence store provider is registered for storage kind " + config.storageKind(),
                    Map.of("storageKind", config.storageKind())));
        }
        if (config.objectStore()) {
            validateObjectStore(config.objectStoreConfig(), path, issues);
            return;
        }
        if (config.database()) {
            validateDatabase(config.databaseConfig(), path, issues);
            return;
        }
        if (config.hybrid()) {
            validateHybrid(config, providers, path, issues);
            return;
        }
        if (AgenticCommerceWayangPersistenceConfig.STORAGE_IN_MEMORY.equals(config.storageKind())) {
            issues.add(warning(
                    "in_memory_persistence_ephemeral",
                    path,
                    "In-memory persistence is ephemeral and should not be used as durable state.",
                    Map.of("storageKind", config.storageKind())));
        }
    }

    private static void validateObjectStore(
            AgenticCommerceObjectStoreConfig config,
            String path,
            List<AgenticCommerceWayangPersistenceConfigValidationIssue> issues) {
        String provider = config.provider();
        if (AgenticCommerceObjectStoreConfig.PROVIDER_OBJECT_STORE.equals(provider)) {
            issues.add(warning(
                    "object_store_provider_generic",
                    path + ".objectStore.provider",
                    "Generic object-store provider should be replaced with s3 or rustfs when known.",
                    config.toMap()));
        } else if (!AgenticCommerceObjectStoreConfig.PROVIDER_S3.equals(provider)
                && !AgenticCommerceObjectStoreConfig.PROVIDER_RUSTFS.equals(provider)) {
            issues.add(warning(
                    "object_store_provider_unknown",
                    path + ".objectStore.provider",
                    "Object-store provider is not one of the built-in provider aliases.",
                    config.toMap()));
        }
        if (AgenticCommerceObjectStoreConfig.PROVIDER_RUSTFS.equals(provider)
                && config.endpoint().isBlank()) {
            issues.add(warning(
                    "object_store_endpoint_missing",
                    path + ".objectStore.endpoint",
                    "RustFS object-store persistence usually needs an endpoint.",
                    config.toMap()));
        }
    }

    private static void validateDatabase(
            AgenticCommerceDatabasePersistenceConfig config,
            String path,
            List<AgenticCommerceWayangPersistenceConfigValidationIssue> issues) {
        String provider = config.provider();
        if (AgenticCommerceDatabasePersistenceConfig.PROVIDER_DATABASE.equals(provider)) {
            issues.add(warning(
                    "database_provider_generic",
                    path + ".database.provider",
                    "Generic database provider should be replaced with jdbc or postgres when known.",
                    config.toMap()));
        } else if (!AgenticCommerceDatabasePersistenceConfig.PROVIDER_JDBC.equals(provider)
                && !AgenticCommerceDatabasePersistenceConfig.PROVIDER_POSTGRES.equals(provider)) {
            issues.add(warning(
                    "database_provider_unknown",
                    path + ".database.provider",
                    "Database provider is not one of the built-in provider aliases.",
                    config.toMap()));
        }
    }

    private static void validateHybrid(
            AgenticCommerceWayangPersistenceConfig config,
            AgenticCommerceWayangPersistenceStoreProviders providers,
            String path,
            List<AgenticCommerceWayangPersistenceConfigValidationIssue> issues) {
        validate(config.primary(), providers, path + ".primary", issues);
        validate(config.fallback(), providers, path + ".fallback", issues);
        if (!config.mirrorWritesToFallback()) {
            issues.add(warning(
                    "hybrid_writes_not_mirrored",
                    path + ".mirrorWritesToFallback",
                    "Hybrid writes are not mirrored to fallback persistence.",
                    Map.of("mirrorWritesToFallback", false)));
        }
        if (config.primary().targetDescriptor().toMap().equals(config.fallback().targetDescriptor().toMap())) {
            issues.add(warning(
                    "hybrid_primary_fallback_same_target",
                    path,
                    "Hybrid primary and fallback resolve to the same persistence target.",
                    Map.of("target", config.primary().targetDescriptor().toMap())));
        }
        if (AgenticCommerceWayangPersistenceConfig.STORAGE_IN_MEMORY.equals(config.primary().storageKind())) {
            issues.add(warning(
                    "hybrid_primary_ephemeral",
                    path + ".primary",
                    "Hybrid primary persistence is ephemeral.",
                    config.primary().targetDescriptor().toMap()));
        }
        if (AgenticCommerceWayangPersistenceConfig.STORAGE_IN_MEMORY.equals(config.fallback().storageKind())) {
            issues.add(warning(
                    "hybrid_fallback_ephemeral",
                    path + ".fallback",
                    "Hybrid fallback persistence is ephemeral.",
                    config.fallback().targetDescriptor().toMap()));
        }
    }

    private static AgenticCommerceWayangPersistenceConfigValidationIssue error(
            String code,
            String path,
            String message,
            Map<String, Object> attributes) {
        return AgenticCommerceWayangPersistenceConfigValidationIssue.error(code, path, message, attributes);
    }

    private static AgenticCommerceWayangPersistenceConfigValidationIssue warning(
            String code,
            String path,
            String message,
            Map<String, Object> attributes) {
        return AgenticCommerceWayangPersistenceConfigValidationIssue.warning(code, path, message, attributes);
    }

    private static List<AgenticCommerceWayangPersistenceConfigValidationIssue> normalizeIssues(
            List<AgenticCommerceWayangPersistenceConfigValidationIssue> issues) {
        if (issues == null || issues.isEmpty()) {
            return List.of();
        }
        return issues.stream()
                .filter(Objects::nonNull)
                .toList();
    }
}
