package tech.kayys.wayang.a2ui.wayang.session;

import tech.kayys.wayang.a2ui.wayang.WayangA2uiActionPolicy;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiSessionConfig;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Resolves named A2UI session profiles and their default action policies.
 */
public final class SessionProfiles {

    public static final String MODE_INSPECT_ONLY = "inspect-only";
    public static final String MODE_READ_ONLY = "read-only";
    public static final String MODE_RUN_LIFECYCLE = "run-lifecycle";
    public static final String MODE_CUSTOM = "custom";

    private static final List<String> MODE_NAMES = List.of(
            MODE_INSPECT_ONLY,
            MODE_READ_ONLY,
            MODE_RUN_LIFECYCLE,
            MODE_CUSTOM);

    private SessionProfiles() {
    }

    public static List<String> modeNames() {
        return MODE_NAMES;
    }

    public static String normalizeMode(String mode) {
        return switch (text(mode).toLowerCase(Locale.ROOT)) {
            case "read", "readonly", "read_only", MODE_READ_ONLY -> MODE_READ_ONLY;
            case "run", "lifecycle", "run_lifecycle", MODE_RUN_LIFECYCLE -> MODE_RUN_LIFECYCLE;
            case MODE_CUSTOM -> MODE_CUSTOM;
            default -> MODE_INSPECT_ONLY;
        };
    }

    public static WayangA2uiActionPolicy actionPolicy(String mode) {
        return switch (normalizeMode(mode)) {
            case MODE_READ_ONLY -> WayangA2uiActionPolicy.readOnly();
            case MODE_RUN_LIFECYCLE -> WayangA2uiActionPolicy.runLifecycle();
            case MODE_CUSTOM -> new WayangA2uiActionPolicy(Set.of(), Set.of(), Map.of());
            default -> WayangA2uiActionPolicy.defaultPolicy();
        };
    }

    public static WayangA2uiSessionConfig config(String mode) {
        return config(mode, true);
    }

    public static WayangA2uiSessionConfig config(String mode, boolean enabled) {
        return new WayangA2uiSessionConfig(enabled, actionPolicy(mode));
    }

    private static String text(String value) {
        return value == null ? "" : value.trim();
    }
}
