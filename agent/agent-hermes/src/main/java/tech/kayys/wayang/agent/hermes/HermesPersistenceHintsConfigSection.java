package tech.kayys.wayang.agent.hermes;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Applies skill persistence hint config keys to an agent mode builder.
 */
final class HermesPersistenceHintsConfigSection implements HermesConfigSection {

    private final String propertyPrefix;

    private HermesPersistenceHintsConfigSection(String propertyPrefix) {
        this.propertyPrefix = propertyPrefix == null ? "" : propertyPrefix;
    }

    static HermesConfigSection withPrefix(String propertyPrefix) {
        return new HermesPersistenceHintsConfigSection(propertyPrefix);
    }

    @Override
    public void apply(
            HermesConfigValues scoped,
            HermesAgentModeConfig.Builder builder) {
        Map<String, String> configuredPersistenceHints = new LinkedHashMap<>();
        configuredPersistenceHints.putAll(HermesPersistenceHintParser.parse(scoped, propertyPrefix));
        HermesLearningPromotionReceiptLedgerHintParser.parse(scoped)
                .forEach((key, value) -> putOverridingNormalizedKey(configuredPersistenceHints, key, value));
        if (!configuredPersistenceHints.isEmpty()) {
            Map<String, String> persistenceHints =
                    new LinkedHashMap<>(HermesAgentModeConfig.defaults().persistenceHints());
            persistenceHints.putAll(configuredPersistenceHints);
            builder.persistenceHints(persistenceHints);
        }
    }

    private static void putOverridingNormalizedKey(Map<String, String> hints, String key, String value) {
        String normalizedKey = HermesConfigValues.normalizeKey(key);
        hints.keySet().removeIf(existing -> HermesConfigValues.normalizeKey(existing).equals(normalizedKey));
        hints.put(key, value);
    }
}
