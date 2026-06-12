package tech.kayys.wayang.gollek.sdk;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public record WayangPlatformReadinessProfile(
        String profileId,
        List<String> readinessIds) {

    public static final String DEFAULT_PROFILE_ID = "default";

    public WayangPlatformReadinessProfile {
        profileId = SdkText.trimToDefault(profileId, DEFAULT_PROFILE_ID);
        readinessIds = readinessIds(readinessIds);
    }

    public static WayangPlatformReadinessProfile of(
            String profileId,
            List<String> readinessIds) {
        return new WayangPlatformReadinessProfile(profileId, readinessIds);
    }

    private static List<String> readinessIds(List<String> readinessIds) {
        if (readinessIds == null || readinessIds.isEmpty()) {
            throw new IllegalArgumentException("Platform readiness profile must include at least one component id.");
        }
        Set<String> resolved = new LinkedHashSet<>();
        for (String readinessId : readinessIds) {
            String normalized = SdkText.trimToEmpty(readinessId);
            if (normalized.isEmpty()) {
                throw new IllegalArgumentException("Platform readiness profile component id is required.");
            }
            if (!resolved.add(normalized)) {
                throw new IllegalArgumentException("Duplicate platform readiness profile component id '"
                        + normalized + "'.");
            }
        }
        return List.copyOf(resolved);
    }
}
