package tech.kayys.wayang.a2ui.wayang;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Canonical decoder for stored or remote A2UI session configuration.
 */
public final class WayangA2uiSessionConfigDecoder {

    public static WayangA2uiSessionConfig fromMap(Map<?, ?> values) {
        if (values == null || values.isEmpty()) {
            return WayangA2uiSessionConfig.inspectOnly();
        }
        Map<String, Object> config = copy(values);
        boolean enabled = WayangA2uiDecodeValues.bool(config.get(WayangA2uiSessionConfig.KEY_ENABLED), true);
        Map<String, Object> policy = policyMap(config);
        return new WayangA2uiSessionConfig(enabled, policy(policy));
    }

    public static WayangA2uiSessionConfig fromJson(String json) {
        return fromMap(WayangA2uiTransportJson.map(
                json,
                "A2UI session config JSON must not be blank",
                "Unable to decode A2UI session config JSON"));
    }

    private static WayangA2uiActionPolicy policy(Map<String, Object> values) {
        String mode = WayangA2uiDecodeValues.text(values.get(WayangA2uiSessionConfig.KEY_MODE));
        WayangA2uiActionPolicy base = switch (normalizeMode(mode)) {
            case WayangA2uiSessionConfig.MODE_READ_ONLY -> WayangA2uiActionPolicy.readOnly();
            case WayangA2uiSessionConfig.MODE_RUN_LIFECYCLE -> WayangA2uiActionPolicy.runLifecycle();
            case WayangA2uiSessionConfig.MODE_CUSTOM -> new WayangA2uiActionPolicy(Set.of(), Set.of(), Map.of());
            default -> WayangA2uiActionPolicy.inspectOnly();
        };
        Set<String> allowedActions = strings(values.get(WayangA2uiSessionConfig.KEY_ALLOWED_ACTIONS));
        Set<String> allowedRunIds = strings(values.get(WayangA2uiSessionConfig.KEY_ALLOWED_RUN_IDS));
        Map<String, String> requiredContext = stringMap(values.get(WayangA2uiSessionConfig.KEY_REQUIRED_CONTEXT));
        return new WayangA2uiActionPolicy(
                allowedActions.isEmpty() ? base.allowedActions() : allowedActions,
                allowedRunIds.isEmpty() ? base.allowedRunIds() : allowedRunIds,
                requiredContext.isEmpty() ? base.requiredContext() : requiredContext);
    }

    private static Map<String, Object> policyMap(Map<String, Object> config) {
        Object nested = config.get(WayangA2uiSessionConfig.KEY_POLICY);
        if (nested instanceof Map<?, ?> nestedMap) {
            Map<String, Object> copy = copy(nestedMap);
            config.forEach((key, value) -> {
                if (!WayangA2uiSessionConfig.KEY_POLICY.equals(key)
                        && !WayangA2uiSessionConfig.KEY_ENABLED.equals(key)) {
                    copy.putIfAbsent(key, value);
                }
            });
            return copy;
        }
        Map<String, Object> policy = new LinkedHashMap<>(config);
        policy.remove(WayangA2uiSessionConfig.KEY_ENABLED);
        return policy;
    }

    private static Map<String, Object> copy(Map<?, ?> values) {
        Map<String, Object> copy = new LinkedHashMap<>();
        values.forEach((key, value) -> {
            if (key != null) {
                copy.put(String.valueOf(key), value);
            }
        });
        return copy;
    }

    private static Set<String> strings(Object value) {
        if (value == null) {
            return Set.of();
        }
        if (value instanceof Collection<?> collection) {
            return collection.stream()
                    .map(WayangA2uiDecodeValues::text)
                    .filter(text -> !text.isBlank())
                    .collect(Collectors.toUnmodifiableSet());
        }
        return Arrays.stream(WayangA2uiDecodeValues.text(value).split(","))
                .map(String::trim)
                .filter(text -> !text.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    }

    private static Map<String, String> stringMap(Object value) {
        if (!(value instanceof Map<?, ?> map) || map.isEmpty()) {
            return Map.of();
        }
        Map<String, String> copy = new LinkedHashMap<>();
        map.forEach((key, entryValue) -> {
            String normalizedKey = WayangA2uiDecodeValues.text(key);
            if (!normalizedKey.isBlank() && entryValue != null) {
                copy.put(normalizedKey, String.valueOf(entryValue));
            }
        });
        return Collections.unmodifiableMap(new LinkedHashMap<>(copy));
    }

    private static String normalizeMode(String mode) {
        return switch (mode.toLowerCase(java.util.Locale.ROOT)) {
            case "read", "readonly", "read_only", "read-only" -> WayangA2uiSessionConfig.MODE_READ_ONLY;
            case "run", "lifecycle", "run_lifecycle", "run-lifecycle" -> WayangA2uiSessionConfig.MODE_RUN_LIFECYCLE;
            case "custom" -> WayangA2uiSessionConfig.MODE_CUSTOM;
            default -> WayangA2uiSessionConfig.MODE_INSPECT_ONLY;
        };
    }

    private WayangA2uiSessionConfigDecoder() {
    }
}
