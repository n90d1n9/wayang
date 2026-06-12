package tech.kayys.wayang.a2ui.core;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A2UI client capability metadata sent with an agent request.
 */
public record A2uiClientCapabilities(
        List<String> supportedCatalogIds,
        List<Map<String, Object>> inlineCatalogs) {

    public A2uiClientCapabilities {
        supportedCatalogIds = supportedCatalogIds == null
                ? List.of()
                : supportedCatalogIds.stream()
                        .map(catalogId -> A2uiValues.required(catalogId, "catalogId"))
                        .distinct()
                        .toList();
        inlineCatalogs = inlineCatalogs == null
                ? List.of()
                : inlineCatalogs.stream()
                        .map(A2uiValues::copyMap)
                        .toList();
    }

    public static A2uiClientCapabilities standard() {
        return new A2uiClientCapabilities(List.of(A2uiProtocol.STANDARD_CATALOG_ID), List.of());
    }

    public static A2uiClientCapabilities fromMap(Map<?, ?> values) {
        Map<String, Object> normalized = A2uiValues.copyMap(values);
        return new A2uiClientCapabilities(
                strings(normalized.get("supportedCatalogIds")),
                maps(normalized.get("inlineCatalogs")));
    }

    public boolean supports(String catalogId) {
        String normalized = A2uiValues.optional(catalogId);
        if (normalized == null) {
            return false;
        }
        return supportedCatalogIds.contains(normalized)
                || inlineCatalogs.stream().anyMatch(catalog -> normalized.equals(catalog.get("catalogId")));
    }

    public Map<String, Object> toMetadata() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("supportedCatalogIds", supportedCatalogIds);
        if (!inlineCatalogs.isEmpty()) {
            metadata.put("inlineCatalogs", inlineCatalogs);
        }
        return Map.copyOf(metadata);
    }

    private static List<String> strings(Object raw) {
        if (raw instanceof List<?> list) {
            return list.stream()
                    .map(String::valueOf)
                    .toList();
        }
        return List.of();
    }

    private static List<Map<String, Object>> maps(Object raw) {
        if (raw instanceof List<?> list) {
            return list.stream()
                    .filter(Map.class::isInstance)
                    .map(Map.class::cast)
                    .map(A2uiValues::copyMap)
                    .toList();
        }
        return List.of();
    }
}
