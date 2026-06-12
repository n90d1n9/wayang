package tech.kayys.wayang.gollek.sdk;

import java.util.List;

public record WayangProviderCapabilityFacetSummary(
        String name,
        int count,
        List<String> capabilityIds) {

    public WayangProviderCapabilityFacetSummary {
        name = SdkText.trimToEmpty(name);
        count = Math.max(0, count);
        capabilityIds = SdkLists.copy(capabilityIds);
    }
}
