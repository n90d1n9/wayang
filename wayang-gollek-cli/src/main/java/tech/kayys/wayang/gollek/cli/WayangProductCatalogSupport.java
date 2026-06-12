package tech.kayys.wayang.gollek.cli;

import tech.kayys.wayang.gollek.sdk.ProductProfile;
import tech.kayys.wayang.gollek.sdk.ProductSurfacePolicy;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class WayangProductCatalogSupport {

    private WayangProductCatalogSupport() {
    }

    static Map<String, ProductSurfacePolicy> policiesBySurface(List<ProductSurfacePolicy> policies) {
        Map<String, ProductSurfacePolicy> indexed = new LinkedHashMap<>();
        for (ProductSurfacePolicy policy : CliLists.copy(policies)) {
            indexed.put(policy.surfaceId(), policy);
        }
        return indexed;
    }

    static Map<String, List<ProductProfile>> profilesBySurface(List<ProductProfile> profiles) {
        Map<String, List<ProductProfile>> indexed = new LinkedHashMap<>();
        for (ProductProfile profile : CliLists.copy(profiles)) {
            indexed.compute(profile.surfaceId(), (surfaceId, existing) -> {
                List<ProductProfile> current = existing == null ? List.of() : existing;
                java.util.ArrayList<ProductProfile> next = new java.util.ArrayList<>(current);
                next.add(profile);
                return List.copyOf(next);
            });
        }
        return indexed;
    }

    static List<String> profileIds(List<ProductProfile> profiles) {
        return CliLists.copy(profiles).stream()
                .map(ProductProfile::id)
                .toList();
    }
}
