package tech.kayys.wayang.capability;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record WayangProviderCapabilityDiscovery(
        WayangProviderCapabilityQuery query,
        String search,
        List<WayangProviderCapabilityDescriptor> capabilities,
        List<String> capabilityIds,
        List<String> providerIds,
        Map<String, Integer> providerIdCounts,
        List<String> moduleIds,
        Map<String, Integer> moduleIdCounts,
        List<String> capabilityTypes,
        Map<String, Integer> capabilityTypeCounts,
        List<String> standardIds,
        Map<String, Integer> standardIdCounts,
        int totalCapabilities) {

    public WayangProviderCapabilityDiscovery {
        query = query == null ? WayangProviderCapabilityQuery.all() : query;
        search = SdkText.trimToEmpty(search);
        capabilities = SdkLists.copy(capabilities);
        capabilityIds = SdkLists.copy(capabilityIds);
        providerIds = SdkLists.copy(providerIds);
        providerIdCounts = SdkCounts.copyPositiveTextKeys(providerIdCounts);
        moduleIds = SdkLists.copy(moduleIds);
        moduleIdCounts = SdkCounts.copyPositiveTextKeys(moduleIdCounts);
        capabilityTypes = SdkLists.copy(capabilityTypes);
        capabilityTypeCounts = SdkCounts.copyPositiveTextKeys(capabilityTypeCounts);
        standardIds = SdkLists.copy(standardIds);
        standardIdCounts = SdkCounts.copyPositiveTextKeys(standardIdCounts);
        totalCapabilities = Math.max(0, totalCapabilities);
    }

    public static WayangProviderCapabilityDiscovery of(
            WayangProviderCapabilityQuery query,
            String search,
            List<WayangProviderCapabilityDescriptor> capabilities,
            int totalCapabilities) {
        List<WayangProviderCapabilityDescriptor> matches = SdkLists.copy(capabilities);
        return new WayangProviderCapabilityDiscovery(
                query,
                search,
                matches,
                matches.stream().map(WayangProviderCapabilityDescriptor::id).toList(),
                SdkFacets.textValues(matches, WayangProviderCapabilityDescriptor::providerId),
                SdkFacets.textCounts(matches, WayangProviderCapabilityDescriptor::providerId),
                SdkFacets.textValues(matches, WayangProviderCapabilityDescriptor::moduleId),
                SdkFacets.textCounts(matches, WayangProviderCapabilityDescriptor::moduleId),
                SdkFacets.textValues(matches, WayangProviderCapabilityDescriptor::capabilityType),
                SdkFacets.textCounts(matches, WayangProviderCapabilityDescriptor::capabilityType),
                SdkFacets.flatValues(matches, WayangProviderCapabilityDescriptor::standardIds),
                SdkFacets.flatCounts(matches, WayangProviderCapabilityDescriptor::standardIds),
                totalCapabilities);
    }

    public int matchingCapabilities() {
        return capabilities.size();
    }

    public boolean empty() {
        return capabilities.isEmpty();
    }

    public List<Map<String, Object>> capabilityMaps() {
        return capabilities.stream()
                .map(WayangProviderCapabilityDescriptor::toMap)
                .toList();
    }

    public List<WayangProviderCapabilityFacetSummary> providerSummaries() {
        return summaries(Facet.PROVIDER);
    }

    public List<WayangProviderCapabilityFacetSummary> moduleSummaries() {
        return summaries(Facet.MODULE);
    }

    public List<WayangProviderCapabilityFacetSummary> capabilityTypeSummaries() {
        return summaries(Facet.CAPABILITY_TYPE);
    }

    public List<WayangProviderCapabilityFacetSummary> standardSummaries() {
        return summaries(Facet.STANDARD);
    }

    private List<WayangProviderCapabilityFacetSummary> summaries(Facet facet) {
        Map<String, List<String>> idsByFacet = new LinkedHashMap<>();
        for (WayangProviderCapabilityDescriptor capability : capabilities) {
            for (String value : facet.values(capability)) {
                String normalized = SdkText.trimToEmpty(value);
                if (!normalized.isEmpty()) {
                    idsByFacet.computeIfAbsent(normalized, ignored -> new ArrayList<>()).add(capability.id());
                }
            }
        }
        return idsByFacet.entrySet().stream()
                .map(entry -> new WayangProviderCapabilityFacetSummary(
                        entry.getKey(),
                        entry.getValue().size(),
                        entry.getValue()))
                .toList();
    }

    private enum Facet {
        PROVIDER {
            @Override
            List<String> values(WayangProviderCapabilityDescriptor capability) {
                return List.of(capability.providerId());
            }
        },
        MODULE {
            @Override
            List<String> values(WayangProviderCapabilityDescriptor capability) {
                return List.of(capability.moduleId());
            }
        },
        CAPABILITY_TYPE {
            @Override
            List<String> values(WayangProviderCapabilityDescriptor capability) {
                return List.of(capability.capabilityType());
            }
        },
        STANDARD {
            @Override
            List<String> values(WayangProviderCapabilityDescriptor capability) {
                return capability.standardIds();
            }
        };

        abstract List<String> values(WayangProviderCapabilityDescriptor capability);
    }
}
