package tech.kayys.wayang.agenticcommerce.wayang;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Structured comparison between two persistence targets.
 */
public record AgenticCommerceWayangPersistenceTargetComparison(
        String sourceLabel,
        String targetLabel,
        Map<String, Object> sourceTarget,
        Map<String, Object> targetTarget) {

    public AgenticCommerceWayangPersistenceTargetComparison {
        sourceLabel = label(sourceLabel, "source");
        targetLabel = label(targetLabel, "target");
        sourceTarget = AgenticCommerceWayangMaps.copy(sourceTarget);
        targetTarget = AgenticCommerceWayangMaps.copy(targetTarget);
    }

    public static AgenticCommerceWayangPersistenceTargetComparison between(
            Map<?, ?> sourceTarget,
            Map<?, ?> targetTarget) {
        return between("source", sourceTarget, "target", targetTarget);
    }

    public static AgenticCommerceWayangPersistenceTargetComparison between(
            String sourceLabel,
            Map<?, ?> sourceTarget,
            String targetLabel,
            Map<?, ?> targetTarget) {
        return new AgenticCommerceWayangPersistenceTargetComparison(
                sourceLabel,
                targetLabel,
                AgenticCommerceWayangMaps.copy(sourceTarget),
                AgenticCommerceWayangMaps.copy(targetTarget));
    }

    public boolean changed() {
        return !sourceTarget.equals(targetTarget);
    }

    public boolean storageKindChanged() {
        return changed("storageKind");
    }

    public boolean targetKindChanged() {
        return changed("targetKind");
    }

    public boolean providerChanged() {
        return changed("provider");
    }

    public boolean locationChanged() {
        return changed("location");
    }

    public boolean durabilityChanged() {
        return changedBoolean("durable") || changedBoolean("ephemeral");
    }

    public boolean cloudStorageChanged() {
        return changedBoolean("cloudStorage");
    }

    public boolean databaseChanged() {
        return changedBoolean("database");
    }

    public boolean hybridChanged() {
        return changedBoolean("hybrid");
    }

    public List<String> changeReasons() {
        List<String> reasons = new ArrayList<>();
        addIf(reasons, storageKindChanged(), "storage_kind_changed");
        addIf(reasons, targetKindChanged(), "target_kind_changed");
        addIf(reasons, providerChanged(), "provider_changed");
        addIf(reasons, locationChanged(), "location_changed");
        addIf(reasons, durabilityChanged(), "durability_changed");
        addIf(reasons, cloudStorageChanged(), "cloud_storage_changed");
        addIf(reasons, databaseChanged(), "database_changed");
        addIf(reasons, hybridChanged(), "hybrid_changed");
        return List.copyOf(reasons);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("sourceLabel", sourceLabel);
        values.put("targetLabel", targetLabel);
        values.put("changed", changed());
        values.put("storageKindChanged", storageKindChanged());
        values.put("targetKindChanged", targetKindChanged());
        values.put("providerChanged", providerChanged());
        values.put("locationChanged", locationChanged());
        values.put("durabilityChanged", durabilityChanged());
        values.put("cloudStorageChanged", cloudStorageChanged());
        values.put("databaseChanged", databaseChanged());
        values.put("hybridChanged", hybridChanged());
        values.put("changeReasons", changeReasons());
        values.put("sourceTarget", sourceTarget);
        values.put("targetTarget", targetTarget);
        return Map.copyOf(values);
    }

    private boolean changed(String key) {
        return !Objects.equals(text(sourceTarget.get(key)), text(targetTarget.get(key)));
    }

    private boolean changedBoolean(String key) {
        return bool(sourceTarget.get(key)) != bool(targetTarget.get(key));
    }

    private static void addIf(List<String> values, boolean condition, String value) {
        if (condition) {
            values.add(value);
        }
    }

    private static String label(String value, String fallback) {
        String normalized = AgenticCommerceWayangMaps.text(value);
        return normalized.isBlank() ? fallback : normalized;
    }

    private static String text(Object value) {
        return AgenticCommerceWayangMaps.text(value);
    }

    private static boolean bool(Object value) {
        return Boolean.TRUE.equals(value);
    }
}
