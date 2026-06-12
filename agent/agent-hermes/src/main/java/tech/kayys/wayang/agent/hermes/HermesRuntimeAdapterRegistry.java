package tech.kayys.wayang.agent.hermes;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Named runtime adapter profiles that can be selected from Hermes config.
 */
public final class HermesRuntimeAdapterRegistry {

    public static final String DEFAULT_PROFILE = "default";

    private final Map<String, HermesRuntimePorts> profiles;

    public HermesRuntimeAdapterRegistry(Map<String, HermesRuntimePorts> profiles) {
        Map<String, HermesRuntimePorts> values = new LinkedHashMap<>();
        if (profiles != null) {
            profiles.forEach((profile, ports) -> {
                String normalized = normalizeProfile(profile);
                if (!normalized.isBlank()) {
                    values.put(normalized, ports == null ? HermesRuntimePorts.noop() : ports);
                }
            });
        }
        this.profiles = Collections.unmodifiableMap(values);
    }

    public static HermesRuntimeAdapterRegistry noop() {
        return builder()
                .register(DEFAULT_PROFILE, HermesRuntimePorts.noop())
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public HermesRuntimePorts resolve(HermesAgentModeConfig config) {
        return resolve(config == null ? DEFAULT_PROFILE : config.runtimeAdapterProfile());
    }

    public HermesRuntimePorts resolve(String profile) {
        HermesRuntimePorts selected = profiles.get(normalizeProfile(profile));
        if (selected != null) {
            return selected;
        }
        return profiles.getOrDefault(DEFAULT_PROFILE, HermesRuntimePorts.noop());
    }

    public boolean contains(String profile) {
        return profiles.containsKey(normalizeProfile(profile));
    }

    public boolean empty() {
        return profiles.isEmpty();
    }

    public List<String> profiles() {
        return List.copyOf(profiles.keySet());
    }

    public Map<String, Object> toMetadata() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("profileCount", profiles.size());
        metadata.put("profiles", profiles());
        metadata.put("defaultProfileAvailable", contains(DEFAULT_PROFILE));
        return Map.copyOf(metadata);
    }

    private static String normalizeProfile(String profile) {
        return HermesDirectiveSupport.clean(profile, DEFAULT_PROFILE)
                .toLowerCase(Locale.ROOT);
    }

    public static final class Builder {
        private final Map<String, HermesRuntimePorts> profiles = new LinkedHashMap<>();

        public Builder register(String profile, HermesRuntimePorts ports) {
            profiles.put(normalizeProfile(profile), ports == null ? HermesRuntimePorts.noop() : ports);
            return this;
        }

        public HermesRuntimeAdapterRegistry build() {
            return new HermesRuntimeAdapterRegistry(profiles);
        }
    }
}
