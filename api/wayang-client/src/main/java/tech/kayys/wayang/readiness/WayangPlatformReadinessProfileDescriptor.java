package tech.kayys.wayang.readiness;

import java.util.List;

import tech.kayys.wayang.client.SdkText;

public record WayangPlatformReadinessProfileDescriptor(
        String profileId,
        String description,
        boolean defaultProfile,
        boolean productionProfile,
        List<String> readinessIds) {

    public WayangPlatformReadinessProfileDescriptor {
        profileId = SdkText.trimToDefault(profileId, WayangPlatformReadinessProfile.DEFAULT_PROFILE_ID);
        description = SdkText.trimToEmpty(description);
        readinessIds = WayangPlatformReadinessProfile.of(profileId, readinessIds).readinessIds();
    }

    public int componentCount() {
        return readinessIds.size();
    }
}
