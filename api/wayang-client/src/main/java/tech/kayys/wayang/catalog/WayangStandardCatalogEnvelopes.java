package tech.kayys.wayang.catalog;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import tech.kayys.wayang.client.SdkMaps;
import tech.kayys.wayang.client.SdkText;
import tech.kayys.wayang.client.WayangStandardDefinition;

/**
 * Wire envelope factory for the standards catalog exposed by Wayang SDK surfaces.
 */
public final class WayangStandardCatalogEnvelopes {

    private WayangStandardCatalogEnvelopes() {
    }

    public static Map<String, Object> catalog(String productName, WayangStandardCatalog catalog) {
        WayangStandardCatalog model = normalize(catalog);
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("product", SdkText.trimToEmpty(productName));
        values.put("totalStandards", model.totalStandards());
        values.put("standardIds", model.standardIds());
        values.put("names", model.names());
        values.put("versions", model.versions());
        values.put("bindings", model.bindings());
        values.put("bindingCounts", model.bindingCounts());
        values.put("specUrls", model.specUrls());
        values.put("standards", model.standards().stream()
                .map(WayangStandardCatalogEnvelopes::standard)
                .toList());
        return SdkMaps.orderedCopy(values);
    }

    public static Map<String, Object> standard(WayangStandardDefinition definition) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("standardId", definition.standardId());
        values.put("name", definition.name());
        values.put("version", definition.version());
        values.put("binding", definition.binding());
        values.put("specUrl", definition.specUrl());
        values.put("aliases", definition.aliases());
        values.put("attributes", attributes(definition.attributes()));
        return SdkMaps.orderedCopy(values);
    }

    public static Map<String, Object> attributes(Map<String, Object> attributes) {
        if (attributes == null || attributes.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> values = new LinkedHashMap<>();
        attributes.keySet().stream()
                .sorted()
                .forEach(key -> values.put(key, attributes.get(key)));
        return SdkMaps.orderedCopy(values);
    }

    public static WayangStandardCatalog normalize(WayangStandardCatalog catalog) {
        return catalog == null ? new WayangStandardCatalog(List.of()) : catalog;
    }
}
