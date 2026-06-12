package tech.kayys.wayang.agenticcommerce.wayang;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Diff-style report for persisted Agentic Commerce Wayang config reloads.
 */
public record AgenticCommerceWayangConfigReloadReport(
        AgenticCommerceWayangConfigSnapshot previousSnapshot,
        AgenticCommerceWayangConfigSnapshot currentSnapshot) {

    public AgenticCommerceWayangConfigReloadReport {
        previousSnapshot = previousSnapshot == null ? emptySnapshot() : previousSnapshot;
        currentSnapshot = currentSnapshot == null ? emptySnapshot() : currentSnapshot;
    }

    public static AgenticCommerceWayangConfigReloadReport from(
            AgenticCommerceWayangConfigSnapshot previousSnapshot,
            AgenticCommerceWayangConfigSnapshot currentSnapshot) {
        return new AgenticCommerceWayangConfigReloadReport(previousSnapshot, currentSnapshot);
    }

    public boolean runtimeConfigChanged() {
        return !previousSnapshot.runtimeConfig().toStorageMap().equals(currentSnapshot.runtimeConfig().toStorageMap());
    }

    public boolean bootstrapConfigChanged() {
        return !previousSnapshot.bootstrapConfig().toMap().equals(currentSnapshot.bootstrapConfig().toMap());
    }

    public boolean runtimeConfigSourceChanged() {
        return previousSnapshot.runtimeConfigPersisted() != currentSnapshot.runtimeConfigPersisted();
    }

    public boolean bootstrapConfigSourceChanged() {
        return previousSnapshot.bootstrapConfigPersisted() != currentSnapshot.bootstrapConfigPersisted();
    }

    public boolean persistenceTargetChanged() {
        return persistenceTargetComparison().changed();
    }

    public AgenticCommerceWayangPersistenceTargetComparison persistenceTargetComparison() {
        return AgenticCommerceWayangPersistenceTargetComparison.between(
                "previous",
                previousSnapshot.persistenceTarget(),
                "current",
                currentSnapshot.persistenceTarget());
    }

    public boolean changed() {
        return runtimeConfigChanged() || bootstrapConfigChanged();
    }

    public boolean sourceChanged() {
        return runtimeConfigSourceChanged() || bootstrapConfigSourceChanged();
    }

    public boolean runtimeRebuildRecommended() {
        return runtimeConfigChanged();
    }

    public boolean bootstrapRerunRecommended() {
        return runtimeConfigChanged() || bootstrapConfigChanged();
    }

    public List<String> changedSections() {
        List<String> sections = new ArrayList<>();
        if (runtimeConfigChanged()) {
            sections.add("runtimeConfig");
        }
        if (bootstrapConfigChanged()) {
            sections.add("bootstrapConfig");
        }
        return List.copyOf(sections);
    }

    public List<String> changedSources() {
        List<String> sources = new ArrayList<>();
        if (runtimeConfigSourceChanged()) {
            sources.add("runtimeConfigSource");
        }
        if (bootstrapConfigSourceChanged()) {
            sources.add("bootstrapConfigSource");
        }
        return List.copyOf(sources);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("changed", changed());
        values.put("sourceChanged", sourceChanged());
        values.put("runtimeConfigChanged", runtimeConfigChanged());
        values.put("bootstrapConfigChanged", bootstrapConfigChanged());
        values.put("runtimeConfigSourceChanged", runtimeConfigSourceChanged());
        values.put("bootstrapConfigSourceChanged", bootstrapConfigSourceChanged());
        values.put("persistenceTargetChanged", persistenceTargetChanged());
        values.put("runtimeRebuildRecommended", runtimeRebuildRecommended());
        values.put("bootstrapRerunRecommended", bootstrapRerunRecommended());
        values.put("changedSections", changedSections());
        values.put("changedSources", changedSources());
        values.put("previousPersistenceTarget", previousSnapshot.persistenceTarget());
        values.put("currentPersistenceTarget", currentSnapshot.persistenceTarget());
        values.put("persistenceTargetComparison", persistenceTargetComparison().toMap());
        values.put("previous", previousSnapshot.toMap());
        values.put("current", currentSnapshot.toMap());
        return Map.copyOf(values);
    }

    private static AgenticCommerceWayangConfigSnapshot emptySnapshot() {
        return new AgenticCommerceWayangConfigSnapshot(
                AgenticCommerceWayangRuntimeConfig.defaults(),
                AgenticCommerceWayangBootstrapConfig.defaults(),
                false,
                false,
                Map.of());
    }
}
