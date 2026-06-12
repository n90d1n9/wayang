package tech.kayys.wayang.gollek.sdk;

import java.util.List;

public record ProductSurface(
        String id,
        String name,
        String role,
        List<String> engineCapabilities,
        List<String> adapterBoundaries) {

    public ProductSurface {
        id = SdkText.trimToEmpty(id);
        name = SdkText.trimToEmpty(name);
        role = SdkText.trimToEmpty(role);
        engineCapabilities = SdkLists.copy(engineCapabilities);
        adapterBoundaries = SdkLists.copy(adapterBoundaries);
    }
}
