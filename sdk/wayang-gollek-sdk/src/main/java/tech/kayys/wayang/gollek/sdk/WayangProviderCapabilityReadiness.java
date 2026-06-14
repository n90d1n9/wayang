package tech.kayys.wayang.gollek.sdk;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class WayangProviderCapabilityReadiness {

    public static final String READINESS_ID = "wayang.provider-capability.readiness";

    private WayangProviderCapabilityReadiness() {
    }

    public static WayangReadinessReport assess(WayangProviderCapabilityDiscovery discovery) {
        WayangProviderCapabilityDiscovery resolved = discovery == null
                ? WayangProviderCapabilityDiscoveryService.create()
                        .discover(WayangProviderCapabilityCatalog.defaultRegistry(), WayangProviderCapabilityQuery.all())
                : discovery;
        Map<String, Object> attributes = attributes(resolved);
        List<Map<String, Object>> issues = issues(resolved, attributes);
        boolean ready = issues.isEmpty();
        return WayangReadinessReport.from(
                READINESS_ID,
                ready,
                WayangReadinessReports.exitCode(ready),
                issues.size(),
                List.of(WayangReadinessReports.probe(
                        "providers.capability_discovery",
                        true,
                        ready,
                        issues.size(),
                        attributes)),
                issues,
                attributes);
    }

    private static Map<String, Object> attributes(WayangProviderCapabilityDiscovery discovery) {
        List<WayangProviderCapabilityDescriptor> capabilities = discovery.capabilities();
        return WayangReadinessAttributeMaps.ordered(
                "totalCapabilities", discovery.totalCapabilities(),
                "matchingCapabilities", discovery.matchingCapabilities(),
                "availableCapabilities", availableCapabilityCount(capabilities),
                "capabilityIds", discovery.capabilityIds(),
                "availableCapabilityIds", availableCapabilityIds(capabilities),
                "providerIds", discovery.providerIds(),
                "providerIdCounts", discovery.providerIdCounts(),
                "moduleIds", discovery.moduleIds(),
                "moduleIdCounts", discovery.moduleIdCounts(),
                "capabilityTypes", discovery.capabilityTypes(),
                "capabilityTypeCounts", discovery.capabilityTypeCounts(),
                "standardIds", discovery.standardIds(),
                "standardIdCounts", discovery.standardIdCounts(),
                "stateCounts", stateCounts(capabilities));
    }

    private static List<Map<String, Object>> issues(
            WayangProviderCapabilityDiscovery discovery,
            Map<String, Object> attributes) {
        if (discovery.totalCapabilities() == 0) {
            return List.of(WayangReadinessReports.issue(
                    "provider_capability_catalog_empty",
                    "providers",
                    "Provider capability discovery catalog is empty.",
                    attributes));
        }
        if (discovery.matchingCapabilities() == 0) {
            return List.of(WayangReadinessReports.issue(
                    "provider_capability_catalog_unmatched",
                    "providers",
                    "Provider capability discovery did not return any matching capabilities.",
                    attributes));
        }
        if (availableCapabilityCount(discovery.capabilities()) == 0) {
            return List.of(WayangReadinessReports.issue(
                    "provider_capability_catalog_unavailable",
                    "providers",
                    "Provider capability discovery has no available or preview capabilities.",
                    attributes));
        }
        return List.of();
    }

    private static int availableCapabilityCount(List<WayangProviderCapabilityDescriptor> capabilities) {
        return (int) capabilities.stream()
                .filter(WayangProviderCapabilityDescriptor::available)
                .count();
    }

    private static List<String> availableCapabilityIds(List<WayangProviderCapabilityDescriptor> capabilities) {
        return capabilities.stream()
                .filter(WayangProviderCapabilityDescriptor::available)
                .map(WayangProviderCapabilityDescriptor::id)
                .toList();
    }

    private static Map<String, Integer> stateCounts(List<WayangProviderCapabilityDescriptor> capabilities) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (WayangProviderCapabilityDescriptor capability : capabilities) {
            counts.merge(capability.state().id(), 1, Integer::sum);
        }
        return SdkCounts.copyPositiveTextKeys(counts);
    }
}
