package tech.kayys.wayang.a2ui.core;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A2UI capability block advertised by an agent/server.
 */
public record A2uiServerCapabilities(List<String> supportedCatalogIds, boolean acceptsInlineCatalogs) {

    public A2uiServerCapabilities {
        supportedCatalogIds = normalizeCatalogIds(supportedCatalogIds);
    }

    public static A2uiServerCapabilities standard() {
        return new A2uiServerCapabilities(List.of(A2uiProtocol.STANDARD_CATALOG_ID), false);
    }

    public Map<String, Object> toParams() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("supportedCatalogIds", supportedCatalogIds);
        params.put("acceptsInlineCatalogs", acceptsInlineCatalogs);
        return Map.copyOf(params);
    }

    private static List<String> normalizeCatalogIds(List<String> catalogIds) {
        if (catalogIds == null || catalogIds.isEmpty()) {
            return List.of(A2uiProtocol.STANDARD_CATALOG_ID);
        }
        return catalogIds.stream()
                .map(catalogId -> A2uiValues.required(catalogId, "catalogId"))
                .distinct()
                .toList();
    }
}
