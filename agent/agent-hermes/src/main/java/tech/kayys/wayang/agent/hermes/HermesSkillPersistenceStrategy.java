package tech.kayys.wayang.agent.hermes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Typed view over configurable learned-skill persistence hints.
 */
public record HermesSkillPersistenceStrategy(
        String definitionStore,
        String artifactStore,
        String fallbackStore,
        List<String> cloudStores,
        Map<String, String> hints) {

    public HermesSkillPersistenceStrategy {
        definitionStore = HermesText.trimOr(
                definitionStore,
                HermesSkillPersistenceHintKeys.defaultDefinitionStore());
        artifactStore = HermesText.trimOr(
                artifactStore,
                HermesSkillPersistenceHintKeys.defaultArtifactStore());
        fallbackStore = HermesText.trimOr(
                fallbackStore,
                HermesSkillPersistenceHintKeys.defaultFallbackStore());
        cloudStores = HermesText.distinctTrimmedList(cloudStores);
        hints = hints == null ? Map.of() : Map.copyOf(hints);
    }

    public static Map<String, String> defaultHints() {
        return HermesSkillPersistenceHintKeys.defaultHints();
    }

    public static HermesSkillPersistenceStrategy defaults() {
        return fromHints(defaultHints());
    }

    public static HermesSkillPersistenceStrategy fromHints(Map<String, String> hints) {
        Map<String, String> merged = new LinkedHashMap<>(defaultHints());
        if (hints != null) {
            hints.forEach((key, value) -> {
                if (key != null && value != null && !value.isBlank()) {
                    merged.put(key.trim(), value.trim());
                }
            });
        }

        String definitionStore = HermesSkillPersistenceHintKeys.definitionStore(merged);
        String artifactStore = HermesSkillPersistenceHintKeys.artifactStore(merged);
        String fallbackStore = HermesSkillPersistenceHintKeys.fallbackStore(merged);
        return new HermesSkillPersistenceStrategy(
                definitionStore,
                artifactStore,
                fallbackStore,
                HermesSkillPersistenceCloudStores.fromHints(
                        merged,
                        definitionStore,
                        artifactStore,
                        fallbackStore),
                merged);
    }

    public boolean usesHybridPersistence() {
        return HermesSkillPersistenceStoreClassifier.isHybrid(definitionStore)
                || HermesSkillPersistenceStoreClassifier.isHybrid(artifactStore)
                || HermesSkillPersistenceStoreClassifier.isHybrid(fallbackStore)
                || !HermesSkillPersistenceStoreClassifier.sameStore(definitionStore, artifactStore)
                || !HermesSkillPersistenceStoreClassifier.normalize(fallbackStore).isBlank();
    }

    public boolean usesDatabaseDefinitions() {
        return HermesSkillPersistenceStoreClassifier.isDatabase(definitionStore);
    }

    public boolean usesCloudArtifacts() {
        return HermesSkillPersistenceStoreClassifier.isObjectStorage(artifactStore);
    }

    public boolean hasFileFallback() {
        return HermesSkillPersistenceStoreClassifier.isFile(fallbackStore);
    }

    public HermesSkillPersistencePlan routePlan() {
        return HermesSkillPersistencePlan.from(this);
    }

    public Map<String, Object> toMetadata() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("definitionStore", definitionStore);
        metadata.put("artifactStore", artifactStore);
        metadata.put("fallbackStore", fallbackStore);
        metadata.put("cloudStores", cloudStores);
        metadata.put("usesHybridPersistence", usesHybridPersistence());
        metadata.put("usesDatabaseDefinitions", usesDatabaseDefinitions());
        metadata.put("usesCloudArtifacts", usesCloudArtifacts());
        metadata.put("hasFileFallback", hasFileFallback());
        metadata.put("routePlan", routePlan().toMetadata());
        metadata.put("hints", hints);
        return Map.copyOf(metadata);
    }
}
