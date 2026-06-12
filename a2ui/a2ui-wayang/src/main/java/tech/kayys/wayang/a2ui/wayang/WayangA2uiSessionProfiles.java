package tech.kayys.wayang.a2ui.wayang;

import tech.kayys.wayang.a2ui.wayang.session.SessionProfiles;

import java.util.List;

/**
 * Public compatibility facade for named A2UI session profiles.
 */
public final class WayangA2uiSessionProfiles {

    public static final String MODE_INSPECT_ONLY = SessionProfiles.MODE_INSPECT_ONLY;
    public static final String MODE_READ_ONLY = SessionProfiles.MODE_READ_ONLY;
    public static final String MODE_RUN_LIFECYCLE = SessionProfiles.MODE_RUN_LIFECYCLE;
    public static final String MODE_CUSTOM = SessionProfiles.MODE_CUSTOM;

    private WayangA2uiSessionProfiles() {
    }

    public static List<String> modeNames() {
        return SessionProfiles.modeNames();
    }

    public static String normalizeMode(String mode) {
        return SessionProfiles.normalizeMode(mode);
    }

    public static WayangA2uiActionPolicy actionPolicy(String mode) {
        return SessionProfiles.actionPolicy(mode);
    }

    public static WayangA2uiSessionConfig config(String mode) {
        return SessionProfiles.config(mode);
    }

    public static WayangA2uiSessionConfig config(String mode, boolean enabled) {
        return SessionProfiles.config(mode, enabled);
    }
}
