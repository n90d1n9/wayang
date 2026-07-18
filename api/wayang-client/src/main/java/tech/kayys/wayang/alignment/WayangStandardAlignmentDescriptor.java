package tech.kayys.wayang.alignment;

import java.util.LinkedHashMap;
import java.util.Map;

import tech.kayys.wayang.client.SdkMaps;
import tech.kayys.wayang.client.SdkText;
import tech.kayys.wayang.registry.WayangStandardRegistry;

/**
 * Common identity descriptor for a protocol or standard alignment report.
 */
public record WayangStandardAlignmentDescriptor(
        String standardId,
        String name,
        String version,
        String binding,
        String specUrl,
        Map<String, Object> attributes) {

    public WayangStandardAlignmentDescriptor {
        standardId = SdkText.trimToDefault(standardId, "unknown");
        name = SdkText.trimToDefault(name, standardId);
        version = SdkText.trimToEmpty(version);
        binding = SdkText.trimToEmpty(binding);
        specUrl = SdkText.trimToEmpty(specUrl);
        attributes = WayangStandardAlignmentMaps.copy(attributes);
    }

    public static WayangStandardAlignmentDescriptor fromReportMap(Map<?, ?> report) {
        Map<String, Object> resolved = WayangStandardAlignmentReportMaps.resolve(report);
        Map<String, Object> standard = WayangStandardAlignmentMaps.map(resolved.get("standard"));
        String standardId = first(standard, resolved, "standardId", "protocol");
        String name = first(standard, resolved, "name", "standardName");
        String version = first(standard, resolved, "version", "protocolVersion", "specVersion");
        String binding = first(standard, resolved, "binding");
        String specUrl = first(standard, resolved, "specUrl", "specHome");
        return WayangStandardRegistry.enrich(new WayangStandardAlignmentDescriptor(
                standardId,
                name.isEmpty() ? standardId : name,
                version,
                binding,
                specUrl,
                extraAttributes(standard)));
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("standardId", standardId);
        values.put("name", name);
        values.put("version", version);
        values.put("binding", binding);
        values.put("specUrl", specUrl);
        values.putAll(attributes);
        return SdkMaps.orderedCopy(values);
    }

    private static String first(Map<?, ?> primary, Map<?, ?> fallback, String... keys) {
        String value = WayangStandardAlignmentMaps.firstText(primary, keys);
        return value.isEmpty() ? WayangStandardAlignmentMaps.firstText(fallback, keys) : value;
    }

    private static Map<String, Object> extraAttributes(Map<String, Object> standard) {
        Map<String, Object> attributes = new LinkedHashMap<>(standard);
        attributes.remove("standardId");
        attributes.remove("name");
        attributes.remove("version");
        attributes.remove("binding");
        attributes.remove("specUrl");
        return WayangStandardAlignmentMaps.copy(attributes);
    }
}
