package tech.kayys.wayang.client;

import java.util.List;

import tech.kayys.wayang.client.SdkLists;
import tech.kayys.wayang.client.SdkText;

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
