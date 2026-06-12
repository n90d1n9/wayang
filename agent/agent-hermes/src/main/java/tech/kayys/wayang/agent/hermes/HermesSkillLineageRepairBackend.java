package tech.kayys.wayang.agent.hermes;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Capability boundary for future learned-skill lineage repair adapters.
 */
public interface HermesSkillLineageRepairBackend {

    String backendId();

    boolean mutationSupported();

    boolean supports(HermesSkillLineageRepairIntent intent);

    default HermesSkillLineageRepairBackendProfile profile() {
        return HermesSkillLineageRepairBackendProfile.configured(backendId(), mutationSupported());
    }

    default HermesSkillLineageRepairBackendProbe probe(HermesSkillLineageRepairIntent intent) {
        return HermesSkillLineageRepairBackendProbe.from(this, intent, profile().toMetadata());
    }

    static HermesSkillLineageRepairBackend previewOnly(String backendId) {
        return configured(backendId, false);
    }

    static HermesSkillLineageRepairBackend configured(String backendId, boolean mutationSupported) {
        return new ConfiguredBackend(backendId, mutationSupported);
    }

    static String normalizeBackendId(String value) {
        return HermesText.oneLineOr(value, "backend")
                .toLowerCase(Locale.ROOT)
                .replace('_', '-');
    }

    final class ConfiguredBackend implements HermesSkillLineageRepairBackend {

        private final HermesSkillLineageRepairBackendProfile profile;

        private ConfiguredBackend(String backendId, boolean mutationSupported) {
            this.profile = HermesSkillLineageRepairBackendProfile.configured(backendId, mutationSupported);
        }

        @Override
        public String backendId() {
            return profile.backendId();
        }

        @Override
        public boolean mutationSupported() {
            return profile.mutationSupported();
        }

        @Override
        public boolean supports(HermesSkillLineageRepairIntent intent) {
            return intent != null && profile.supportsCommand(intent.command());
        }

        @Override
        public HermesSkillLineageRepairBackendProfile profile() {
            return profile;
        }

        @Override
        public HermesSkillLineageRepairBackendProbe probe(HermesSkillLineageRepairIntent intent) {
            Map<String, Object> metadata = new LinkedHashMap<>(profile.toMetadata());
            metadata.put("adapterMode", profile.adapterMode());
            return HermesSkillLineageRepairBackendProbe.from(
                    this,
                    intent,
                    metadata);
        }
    }
}
