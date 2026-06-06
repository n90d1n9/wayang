package tech.kayys.wayang.a2ui.wayang;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Ordered transport projections for A2UI session configuration.
 */
final class WayangA2uiSessionProjection {

    private WayangA2uiSessionProjection() {
    }

    static Map<String, Object> config(WayangA2uiSessionConfig config) {
        Objects.requireNonNull(config, "config");

        Map<String, Object> values = new LinkedHashMap<>();
        values.put(WayangA2uiSessionConfig.KEY_ENABLED, config.enabled());
        values.put(WayangA2uiSessionConfig.KEY_POLICY, actionPolicy(config.actionPolicy()));
        return WayangA2uiTransportMaps.freeze(values);
    }

    static Map<String, Object> actionPolicy(WayangA2uiActionPolicy policy) {
        Objects.requireNonNull(policy, "policy");

        Map<String, Object> values = new LinkedHashMap<>();
        values.put(WayangA2uiSessionConfig.KEY_ALLOWED_ACTIONS, sorted(policy.allowedActions()));
        values.put(WayangA2uiSessionConfig.KEY_ALLOWED_RUN_IDS, sorted(policy.allowedRunIds()));
        values.put(WayangA2uiSessionConfig.KEY_REQUIRED_CONTEXT,
                WayangA2uiTransportMaps.copy(policy.requiredContext()));
        return WayangA2uiTransportMaps.freeze(values);
    }

    private static List<String> sorted(Set<String> values) {
        return values.stream()
                .sorted()
                .toList();
    }
}
