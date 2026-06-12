package tech.kayys.wayang.agent.hermes;

import java.util.Locale;

/**
 * Stable adapter-facing role names for learned-skill persistence routes.
 */
public final class HermesSkillPersistenceRouteRoles {

    public static final String DEFINITIONS = "definitions";
    public static final String ARTIFACTS = "artifacts";
    public static final String CLOUD = "cloud";
    public static final String FALLBACK = "fallback";
    public static final String CUSTOM = "custom";

    private HermesSkillPersistenceRouteRoles() {
    }

    public static String normalize(String role) {
        String value = HermesText.trimOr(role, CUSTOM)
                .toLowerCase(Locale.ROOT)
                .replace('_', '-')
                .replace(' ', '-');
        return value.isBlank() ? CUSTOM : value;
    }
}
