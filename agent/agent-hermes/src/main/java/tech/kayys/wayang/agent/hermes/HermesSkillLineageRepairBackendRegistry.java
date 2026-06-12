package tech.kayys.wayang.agent.hermes;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Registry of repair backends used to assess dry-run repair readiness.
 */
public final class HermesSkillLineageRepairBackendRegistry {

    public static final List<String> DEFAULT_BACKENDS = List.of(
            "database",
            "file-system",
            "object-storage");

    private static final HermesSkillLineageRepairBackendRegistry DEFAULT =
            from(DEFAULT_BACKENDS, List.of());

    private final List<HermesSkillLineageRepairBackend> backends;

    public HermesSkillLineageRepairBackendRegistry(List<HermesSkillLineageRepairBackend> backends) {
        this.backends = HermesCollections.copyNonNull(backends);
    }

    public static HermesSkillLineageRepairBackendRegistry defaultRegistry() {
        return DEFAULT;
    }

    public static HermesSkillLineageRepairBackendRegistry from(
            List<String> backendIds,
            List<String> mutationBackendIds) {
        Set<String> configuredBackends = normalizeSet(
                backendIds == null || backendIds.isEmpty() ? DEFAULT_BACKENDS : backendIds);
        Set<String> mutatingBackends = normalizeSet(mutationBackendIds);
        configuredBackends.addAll(mutatingBackends);
        return new HermesSkillLineageRepairBackendRegistry(configuredBackends.stream()
                .map(backend -> HermesSkillLineageRepairBackend.configured(
                        backend,
                        mutatingBackends.contains(backend)))
                .toList());
    }

    public int backendCount() {
        return backends.size();
    }

    public List<HermesSkillLineageRepairBackend> backends() {
        return backends;
    }

    public List<String> backendIds() {
        return backends.stream()
                .map(HermesSkillLineageRepairBackend::backendId)
                .toList();
    }

    public List<String> mutationBackendIds() {
        return backends.stream()
                .filter(HermesSkillLineageRepairBackend::mutationSupported)
                .map(HermesSkillLineageRepairBackend::backendId)
                .toList();
    }

    public List<HermesSkillLineageRepairBackendProfile> profiles() {
        return backends.stream()
                .map(HermesSkillLineageRepairBackend::profile)
                .toList();
    }

    public HermesSkillLineageRepairBackendPlan assess(HermesSkillLineageRepairIntentPlan intentPlan) {
        HermesSkillLineageRepairIntentPlan resolved = intentPlan == null
                ? new HermesSkillLineageRepairIntentPlan(true, false, false, 0, 0, 0, List.of())
                : intentPlan;
        if (resolved.intents().isEmpty()) {
            return HermesSkillLineageRepairBackendPlan.empty(backendCount());
        }
        return new HermesSkillLineageRepairBackendPlan(
                backendCount(),
                0,
                0,
                0,
                0,
                false,
                resolved.intents().stream()
                        .map(intent -> HermesSkillLineageRepairBackendAssessment.from(intent, backends))
                        .toList());
    }

    public Map<String, Object> toMetadata() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("backendCount", backendCount());
        metadata.put("backendIds", backendIds());
        metadata.put("mutationBackendIds", mutationBackendIds());
        metadata.put("mutationBackendCount", mutationBackendIds().size());
        metadata.put("backendProfiles", profiles().stream()
                .map(HermesSkillLineageRepairBackendProfile::toMetadata)
                .toList());
        return Map.copyOf(metadata);
    }

    private static Set<String> normalizeSet(List<String> values) {
        Set<String> normalized = new LinkedHashSet<>();
        if (values != null) {
            values.stream()
                    .filter(value -> value != null && !value.isBlank())
                    .map(HermesSkillLineageRepairBackend::normalizeBackendId)
                    .forEach(normalized::add);
        }
        return normalized;
    }
}
