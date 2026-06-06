package tech.kayys.wayang.a2ui.wayang;

import java.util.Map;

/**
 * Transport-neutral A2UI session configuration.
 */
public record WayangA2uiSessionConfig(
        boolean enabled,
        WayangA2uiActionPolicy actionPolicy) {

    public static final String KEY_ENABLED = "enabled";
    public static final String KEY_POLICY = "policy";
    public static final String KEY_MODE = "mode";
    public static final String KEY_ALLOWED_ACTIONS = "allowedActions";
    public static final String KEY_ALLOWED_RUN_IDS = "allowedRunIds";
    public static final String KEY_REQUIRED_CONTEXT = "requiredContext";

    public static final String MODE_INSPECT_ONLY = "inspect-only";
    public static final String MODE_READ_ONLY = "read-only";
    public static final String MODE_RUN_LIFECYCLE = "run-lifecycle";
    public static final String MODE_CUSTOM = "custom";

    public WayangA2uiSessionConfig {
        actionPolicy = actionPolicy == null ? WayangA2uiActionPolicy.inspectOnly() : actionPolicy;
    }

    public static WayangA2uiSessionConfig inspectOnly() {
        return new WayangA2uiSessionConfig(true, WayangA2uiActionPolicy.inspectOnly());
    }

    public static WayangA2uiSessionConfig runLifecycle() {
        return new WayangA2uiSessionConfig(true, WayangA2uiActionPolicy.runLifecycle());
    }

    public static WayangA2uiSessionConfig readOnly() {
        return new WayangA2uiSessionConfig(true, WayangA2uiActionPolicy.readOnly());
    }

    public static WayangA2uiSessionConfig disabled() {
        return new WayangA2uiSessionConfig(false, WayangA2uiActionPolicy.inspectOnly());
    }

    public static WayangA2uiSessionConfig fromMap(Map<?, ?> values) {
        return WayangA2uiSessionConfigDecoder.fromMap(values);
    }

    public static WayangA2uiSessionConfig fromJson(String json) {
        return WayangA2uiSessionConfigDecoder.fromJson(json);
    }

    public Map<String, Object> toMap() {
        return WayangA2uiSessionProjection.config(this);
    }
}
