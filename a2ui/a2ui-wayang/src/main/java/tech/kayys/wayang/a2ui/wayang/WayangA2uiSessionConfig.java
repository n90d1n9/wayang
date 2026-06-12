package tech.kayys.wayang.a2ui.wayang;

import tech.kayys.wayang.a2ui.wayang.projection.SessionProjection;
import tech.kayys.wayang.a2ui.wayang.session.SessionConfigDecoder;
import tech.kayys.wayang.a2ui.wayang.session.SessionConfigSource;
import tech.kayys.wayang.a2ui.wayang.session.SessionConfigSources;
import tech.kayys.wayang.a2ui.wayang.session.SessionProfiles;

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

    public static final String MODE_INSPECT_ONLY = SessionProfiles.MODE_INSPECT_ONLY;
    public static final String MODE_READ_ONLY = SessionProfiles.MODE_READ_ONLY;
    public static final String MODE_RUN_LIFECYCLE = SessionProfiles.MODE_RUN_LIFECYCLE;
    public static final String MODE_CUSTOM = SessionProfiles.MODE_CUSTOM;

    public WayangA2uiSessionConfig {
        actionPolicy = actionPolicy == null ? WayangA2uiActionPolicy.defaultPolicy() : actionPolicy;
    }

    public static WayangA2uiSessionConfig inspectOnly() {
        return SessionProfiles.config(MODE_INSPECT_ONLY);
    }

    public static WayangA2uiSessionConfig defaultConfig() {
        return inspectOnly();
    }

    public static WayangA2uiSessionConfig runLifecycle() {
        return SessionProfiles.config(MODE_RUN_LIFECYCLE);
    }

    public static WayangA2uiSessionConfig readOnly() {
        return SessionProfiles.config(MODE_READ_ONLY);
    }

    public static WayangA2uiSessionConfig disabled() {
        return SessionProfiles.config(MODE_INSPECT_ONLY, false);
    }

    public static WayangA2uiSessionConfig fromMap(Map<?, ?> values) {
        return SessionConfigDecoder.fromMap(values);
    }

    public static WayangA2uiSessionConfig fromJson(String json) {
        return SessionConfigDecoder.fromJson(json);
    }

    public static WayangA2uiSessionConfig fromSource(SessionConfigSource source) {
        return SessionConfigSources.loadOrDefault(source);
    }

    public Map<String, Object> toMap() {
        return SessionProjection.config(this);
    }
}
