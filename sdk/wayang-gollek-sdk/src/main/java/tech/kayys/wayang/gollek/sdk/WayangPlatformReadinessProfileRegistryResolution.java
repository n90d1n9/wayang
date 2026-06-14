package tech.kayys.wayang.gollek.sdk;

import java.util.List;

/**
 * Resolved readiness profile registry state with redacted active-source diagnostics.
 *
 * <p>The resolution is the shared model behind platform readiness, registry
 * inspection, and preflight commands. It keeps profile descriptors intact while
 * making source locations safe for operator-facing projections.</p>
 */
public record WayangPlatformReadinessProfileRegistryResolution(
        String activeSourceId,
        String activeSourceType,
        String activeSourceLocation,
        boolean fallbackUsed,
        List<WayangPlatformReadinessProfileSourceStatus> sources,
        List<WayangPlatformReadinessProfileDescriptor> profiles,
        WayangPlatformReadinessProfileValidationReport validation) {

    public WayangPlatformReadinessProfileRegistryResolution {
        activeSourceId = SdkText.trimToDefault(activeSourceId, "unknown");
        activeSourceType = SdkText.trimToDefault(activeSourceType, "unknown");
        activeSourceLocation = WayangSecretRedactor.connectionString(activeSourceLocation);
        sources = SdkLists.copy(sources);
        profiles = profiles == null || profiles.isEmpty()
                ? List.of()
                : profiles.stream()
                        .filter(profile -> profile != null)
                        .toList();
        validation = validation == null
                ? WayangPlatformReadinessProfileValidation.validate(profiles)
                : validation;
    }

    public boolean valid() {
        return validation.valid();
    }

    public int sourceCount() {
        return sources.size();
    }

    public int totalProfiles() {
        return profiles.size();
    }
}
