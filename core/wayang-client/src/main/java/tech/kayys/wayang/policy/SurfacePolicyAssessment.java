package tech.kayys.wayang.policy;

import java.util.List;

import tech.kayys.wayang.client.SdkLists;
import tech.kayys.wayang.client.SdkText;

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
