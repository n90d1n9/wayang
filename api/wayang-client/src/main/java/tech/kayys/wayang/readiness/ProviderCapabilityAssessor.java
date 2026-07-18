package tech.kayys.wayang.readiness;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import tech.kayys.wayang.capability.WayangProviderCapabilityCatalog;
import tech.kayys.wayang.capability.WayangProviderCapabilityDescriptor;
import tech.kayys.wayang.capability.WayangProviderCapabilityDiscovery;
import tech.kayys.wayang.capability.WayangProviderCapabilityDiscoveryService;
import tech.kayys.wayang.capability.WayangProviderCapabilityQuery;
import tech.kayys.wayang.client.SdkCounts;
import tech.kayys.wayang.client.WayangReadinessAttributeMaps;
import tech.kayys.wayang.client.WayangReadinessReports;

/**
 * Readiness assessor for provider capability discovery.
 * Evaluates whether provider capabilities are available and discoverable.
 */
public class ProviderCapabilityAssessor extends ComponentReadinessAssessor {

    public static final String READINESS_ID = "wayang.provider-capability.readiness";

    @Override
    protected String getId() {
        return READINESS_ID;
    }

    @Override
    protected String getSource() {
        return "providers";
    }

    @Override
    protected String buildProbeName() {
        return "providers.capability_discovery";
    }

    @Override
    protected List<Map<String, Object>> validate(Object input) {
        WayangProviderCapabilityDiscovery discovery = (WayangProviderCapabilityDiscovery) input;
        WayangProviderCapabilityDiscovery resolved = discovery == null
                ? WayangProviderCapabilityDiscoveryService.create()
                        .discover(WayangProviderCapabilityCatalog.defaultRegistry(), WayangProviderCapabilityQuery.all())
                : discovery;

        Map<String, Object> attributes = buildAttributes(resolved);
        return issues(resolved, attributes);
    }

    @Override
    protected Map<String, Object> buildAttributes(Object input) {
        WayangProviderCapabilityDiscovery discovery = (WayangProviderCapabilityDiscovery) input;
        WayangProviderCapabilityDiscovery resolved = discovery == null
                ? WayangProviderCapabilityDiscoveryService.create()
                        .discover(WayangProviderCapabilityCatalog.defaultRegistry(), WayangProviderCapabilityQuery.all())
                : discovery;

        List<WayangProviderCapabilityDescriptor> capabilities = resolved.capabilities();
        return WayangReadinessAttributeMaps.ordered(
                "totalCapabilities", resolved.totalCapabilities(),
                "matchingCapabilities", resolved.matchingCapabilities(),
                "availableCapabilities", availableCapabilityCount(capabilities),
                "capabilityIds", resolved.capabilityIds(),
                "availableCapabilityIds", availableCapabilityIds(capabilities),
                "providerIds", resolved.providerIds(),
                "providerIdCounts", resolved.providerIdCounts(),
                "moduleIds", resolved.moduleIds(),
                "moduleIdCounts", resolved.moduleIdCounts(),
                "capabilityTypes", resolved.capabilityTypes(),
                "capabilityTypeCounts", resolved.capabilityTypeCounts(),
                "standardIds", resolved.standardIds(),
                "standardIdCounts", resolved.standardIdCounts(),
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
