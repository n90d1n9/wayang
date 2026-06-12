package tech.kayys.wayang.a2a.wayang;

import tech.kayys.wayang.a2a.core.A2aProtocol;

import java.util.LinkedHashMap;
import java.util.Map;

record WayangA2aSpecAlignmentStandardDescriptor(
        String standardId,
        String name,
        String version,
        String binding,
        String specUrl) {

    WayangA2aSpecAlignmentStandardDescriptor {
        standardId = standardId == null || standardId.isBlank()
                ? WayangA2aSpecAlignmentReport.STANDARD_ID
                : standardId.trim();
        name = name == null || name.isBlank()
                ? WayangA2aSpecAlignmentReport.STANDARD_NAME
                : name.trim();
        version = version == null || version.isBlank()
                ? A2aProtocol.VERSION
                : version.trim();
        binding = binding == null || binding.isBlank()
                ? A2aProtocol.BINDING_JSONRPC
                : binding.trim();
        specUrl = specUrl == null || specUrl.isBlank()
                ? WayangA2aSpecAlignmentReport.SPEC_URL
                : specUrl.trim();
    }

    static WayangA2aSpecAlignmentStandardDescriptor pinned() {
        return from(A2aProtocol.VERSION, A2aProtocol.BINDING_JSONRPC);
    }

    static WayangA2aSpecAlignmentStandardDescriptor from(String version, String binding) {
        return new WayangA2aSpecAlignmentStandardDescriptor(
                WayangA2aSpecAlignmentReport.STANDARD_ID,
                WayangA2aSpecAlignmentReport.STANDARD_NAME,
                version,
                binding,
                WayangA2aSpecAlignmentReport.SPEC_URL);
    }

    Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("standardId", standardId);
        values.put("name", name);
        values.put("version", version);
        values.put("binding", binding);
        values.put("specUrl", specUrl);
        return WayangA2aMaps.copyMap(values);
    }
}
