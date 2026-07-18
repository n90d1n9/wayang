package tech.kayys.wayang.client;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import tech.kayys.wayang.alignment.WayangStandardAlignmentDescriptor;
import tech.kayys.wayang.alignment.WayangStandardAlignmentMaps;


/**
 * Stable SDK identity metadata for a protocol or interoperability standard.
 */
public record WayangStandardDefinition(
        String standardId,
        String name,
        String version,
        String binding,
        String specUrl,
        List<String> aliases,
        Map<String, Object> attributes) {

    public WayangStandardDefinition {
        standardId = SdkText.trimToDefault(standardId, "unknown");
        name = SdkText.trimToDefault(name, standardId);
        version = SdkText.trimToEmpty(version);
        binding = SdkText.trimToEmpty(binding);
        specUrl = SdkText.trimToEmpty(specUrl);
        aliases = SdkLists.copy(aliases);
        attributes = WayangStandardAlignmentMaps.copy(attributes);
    }

    public WayangStandardAlignmentDescriptor toDescriptor() {
        return new WayangStandardAlignmentDescriptor(
                standardId,
                name,
                version,
                binding,
                specUrl,
                attributes);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("standardId", standardId);
        values.put("name", name);
        values.put("version", version);
        values.put("binding", binding);
        values.put("specUrl", specUrl);
        values.put("aliases", aliases);
        values.putAll(attributes);
        return SdkMaps.orderedCopy(values);
    }
}
