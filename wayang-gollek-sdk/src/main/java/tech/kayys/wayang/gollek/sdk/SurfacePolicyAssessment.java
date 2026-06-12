package tech.kayys.wayang.gollek.sdk;

import java.util.List;

public record SurfacePolicyAssessment(
        String surfaceId,
        boolean ready,
        List<String> satisfiedContextKeys,
        List<String> missingContextKeys,
        List<String> recommendations,
        List<String> routingHints) {

    public SurfacePolicyAssessment {
        surfaceId = SdkText.trimToEmpty(surfaceId);
        satisfiedContextKeys = SdkLists.copy(satisfiedContextKeys);
        missingContextKeys = SdkLists.copy(missingContextKeys);
        recommendations = SdkLists.copy(recommendations);
        routingHints = SdkLists.copy(routingHints);
    }
}
