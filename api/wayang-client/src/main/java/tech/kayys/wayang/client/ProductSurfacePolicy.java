package tech.kayys.wayang.client;

import java.util.List;

public record ProductSurfacePolicy(
        String surfaceId,
        boolean memoryPreferred,
        boolean workspacePreferred,
        boolean harnessPreferred,
        boolean workflowPreferred,
        List<String> suggestedSkills,
        List<String> requiredContextKeys,
        List<String> routingHints) {

    public ProductSurfacePolicy {
        surfaceId = SdkText.trimToEmpty(surfaceId);
        suggestedSkills = SdkLists.copy(suggestedSkills);
        requiredContextKeys = SdkLists.copy(requiredContextKeys);
        routingHints = SdkLists.copy(routingHints);
    }
}
