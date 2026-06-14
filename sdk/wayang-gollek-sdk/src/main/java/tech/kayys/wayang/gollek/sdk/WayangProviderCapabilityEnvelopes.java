package tech.kayys.wayang.gollek.sdk;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Wire envelope factory for provider capability discovery and detail payloads.
 *
 * <p>Provider capability envelopes are SDK-owned so every Wayang product surface
 * can share the same ordered contract and empty-discovery fallback.</p>
 */
public final class WayangProviderCapabilityEnvelopes {

    private WayangProviderCapabilityEnvelopes() {
    }

    public static Map<String, Object> discovery(
            String productName,
            WayangProviderCapabilityDiscovery discovery) {
        WayangProviderCapabilityDiscovery model = normalize(discovery);
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("product", SdkText.trimToEmpty(productName));
        values.put("query", query(model.query()));
        values.put("search", SdkText.blankToNull(model.search()));
        values.put("totalCapabilities", model.totalCapabilities());
        values.put("matchingCapabilities", model.matchingCapabilities());
        values.put("providerIds", model.providerIds());
        values.put("providerIdCounts", model.providerIdCounts());
        values.put("providerSummaries", model.providerSummaries().stream()
                .map(WayangProviderCapabilityEnvelopes::facetSummary)
                .toList());
        values.put("moduleIds", model.moduleIds());
        values.put("moduleIdCounts", model.moduleIdCounts());
        values.put("moduleSummaries", model.moduleSummaries().stream()
                .map(WayangProviderCapabilityEnvelopes::facetSummary)
                .toList());
        values.put("capabilityTypes", model.capabilityTypes());
        values.put("capabilityTypeCounts", model.capabilityTypeCounts());
        values.put("capabilityTypeSummaries", model.capabilityTypeSummaries().stream()
                .map(WayangProviderCapabilityEnvelopes::facetSummary)
                .toList());
        values.put("standardIds", model.standardIds());
        values.put("standardIdCounts", model.standardIdCounts());
        values.put("standardSummaries", model.standardSummaries().stream()
                .map(WayangProviderCapabilityEnvelopes::facetSummary)
                .toList());
        values.put("capabilityIds", model.capabilityIds());
        values.put("capabilities", model.capabilityMaps());
        return SdkMaps.orderedCopy(values);
    }

    public static Map<String, Object> detail(
            String productName,
            WayangProviderCapabilityDescriptor capability) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("product", SdkText.trimToEmpty(productName));
        values.put("capabilityId", capability.id());
        values.put("capability", capability.toMap());
        return SdkMaps.orderedCopy(values);
    }

    public static Map<String, Object> query(WayangProviderCapabilityQuery query) {
        WayangProviderCapabilityQuery normalized = query == null ? WayangProviderCapabilityQuery.all() : query;
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("capabilityId", SdkText.blankToNull(normalized.capabilityId()));
        values.put("providerId", SdkText.blankToNull(normalized.providerId()));
        values.put("providerNamespace", SdkText.blankToNull(normalized.providerNamespace()));
        values.put("moduleId", SdkText.blankToNull(normalized.moduleId()));
        values.put("capabilityType", SdkText.blankToNull(normalized.capabilityType()));
        values.put("state", normalized.state() == null ? null : normalized.state().id());
        values.put("surfaceId", SdkText.blankToNull(normalized.surfaceId()));
        values.put("standardId", SdkText.blankToNull(normalized.standardId()));
        values.put("tag", SdkText.blankToNull(normalized.tag()));
        values.put("filtered", normalized.filtered());
        return SdkMaps.orderedCopy(values);
    }

    public static Map<String, Object> facetSummary(WayangProviderCapabilityFacetSummary summary) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("name", summary.name());
        values.put("count", summary.count());
        values.put("capabilityIds", summary.capabilityIds());
        return SdkMaps.orderedCopy(values);
    }

    public static WayangProviderCapabilityDiscovery normalize(WayangProviderCapabilityDiscovery discovery) {
        return discovery == null
                ? WayangProviderCapabilityDiscovery.of(WayangProviderCapabilityQuery.all(), "", List.of(), 0)
                : discovery;
    }

}
