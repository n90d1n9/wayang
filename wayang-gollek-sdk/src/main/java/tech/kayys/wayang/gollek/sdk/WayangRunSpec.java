package tech.kayys.wayang.gollek.sdk;

import java.util.Properties;

/**
 * Portable property-file description of a Wayang agent run request.
 *
 * <p>A run spec can be produced by product templates, checked into a workspace,
 * validated before execution, and reused by CLI, TUI, HTTP, or automation shells
 * without depending on any one user-interface wrapper.</p>
 */
public record WayangRunSpec(
        String specVersion,
        String profileId,
        AgentRunRequest request,
        boolean requireReady) {

    public static final String CURRENT_VERSION = "1";
    private static final String SPEC_VERSION_KEY = "specVersion";
    private static final String PROFILE_ID_KEY = "profileId";
    private static final String REQUIRE_READY_KEY = "requireReady";

    public WayangRunSpec {
        specVersion = normalizeVersion(specVersion);
        profileId = SdkText.trimToEmpty(profileId);
        request = AgentRunRequest.builder(request).build();
    }

    public static WayangRunSpec of(AgentRunRequest request) {
        return of(request, false);
    }

    public static WayangRunSpec of(AgentRunRequest request, boolean requireReady) {
        return of("", request, requireReady);
    }

    public static WayangRunSpec of(String profileId, AgentRunRequest request, boolean requireReady) {
        return new WayangRunSpec(CURRENT_VERSION, profileId, request, requireReady);
    }

    public static WayangRunSpec fromProperties(Properties properties) {
        return fromProperties(properties, "");
    }

    public static WayangRunSpec fromProperties(Properties properties, String profileOverride) {
        Properties source = properties == null ? new Properties() : properties;
        String specVersion = normalizeVersion(source.getProperty(SPEC_VERSION_KEY));
        String overrideProfileId = SdkText.trimToEmpty(profileOverride);
        String profileId = overrideProfileId.isBlank()
                ? SdkText.trimToEmpty(source.getProperty(PROFILE_ID_KEY))
                : overrideProfileId;
        ProductProfile profile = profileId.isBlank() ? null : WayangProductCatalog.profileFor(profileId);
        AgentRunRequest baseRequest = profile == null
                ? AgentRunRequest.builder().build()
                : profile.requestTemplate();
        AgentRunRequest request = AgentRunSpec.fromProperties(source, baseRequest);
        boolean requireReady = source.containsKey(REQUIRE_READY_KEY)
                ? booleanValue(source, REQUIRE_READY_KEY)
                : profile != null && profile.requireReady();
        return new WayangRunSpec(specVersion, profileId, request, requireReady);
    }

    public static WayangRunSpec template(String surfaceId) {
        return of(AgentRunSpec.template(surfaceId));
    }

    public static WayangRunSpec profileTemplate(String profileId) {
        ProductProfile profile = WayangProductCatalog.profileFor(profileId);
        return of(profile.id(), profile.requestTemplate(), profile.requireReady());
    }

    public static String formatProperties(WayangRunSpec spec) {
        WayangRunSpec source = spec == null ? of(AgentRunRequest.builder().build()) : spec;
        StringBuilder output = new StringBuilder();
        output.append(SPEC_VERSION_KEY)
                .append('=')
                .append(source.specVersion())
                .append(System.lineSeparator());
        if (!source.profileId().isBlank()) {
            output.append(PROFILE_ID_KEY)
                    .append('=')
                    .append(source.profileId())
                    .append(System.lineSeparator());
        }
        output.append(AgentRunSpec.formatProperties(source.request()));
        if (source.requireReady()) {
            output.append(REQUIRE_READY_KEY)
                    .append("=true")
                    .append(System.lineSeparator());
        }
        return output.toString();
    }

    private static String normalizeVersion(String version) {
        String normalized = SdkText.trimToDefault(version, CURRENT_VERSION);
        if (CURRENT_VERSION.equals(normalized)) {
            return normalized;
        }
        throw new IllegalArgumentException(
                "Unsupported Wayang run spec version '" + normalized + "'. Supported version: " + CURRENT_VERSION + ".");
    }

    private static boolean booleanValue(Properties properties, String key) {
        String value = SdkText.trimToEmpty(properties.getProperty(key));
        if ("true".equalsIgnoreCase(value)) {
            return true;
        }
        if ("false".equalsIgnoreCase(value)) {
            return false;
        }
        throw new IllegalArgumentException("Run spec key '" + key + "' must be true or false.");
    }
}
