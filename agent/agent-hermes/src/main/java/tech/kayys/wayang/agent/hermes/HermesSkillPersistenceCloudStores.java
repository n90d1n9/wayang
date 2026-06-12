package tech.kayys.wayang.agent.hermes;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Canonical object-storage store list derived from persistence hints and backends.
 */
public final class HermesSkillPersistenceCloudStores {

    private HermesSkillPersistenceCloudStores() {
    }

    public static List<String> fromHints(Map<String, String> hints, String... configuredStores) {
        return fromValues(HermesSkillPersistenceHintKeys.cloudStores(hints), configuredStores);
    }

    public static List<String> fromValues(String explicitStores, String... configuredStores) {
        Set<String> stores = new LinkedHashSet<>();
        addDelimited(stores, explicitStores);
        if (configuredStores != null) {
            for (String store : configuredStores) {
                addStore(stores, store);
            }
        }
        return List.copyOf(stores);
    }

    public static List<String> parse(String value) {
        Set<String> stores = new LinkedHashSet<>();
        addDelimited(stores, value);
        return List.copyOf(stores);
    }

    private static void addDelimited(Set<String> stores, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        for (String entry : value.split("[,;]")) {
            addStore(stores, entry);
        }
    }

    private static void addStore(Set<String> stores, String value) {
        String canonical = HermesSkillPersistenceStoreClassifier.canonicalCloudStore(value);
        if (canonical != null) {
            stores.add(canonical);
        }
    }
}
