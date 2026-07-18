package tech.kayys.wayang.capability;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class WayangProviderCapabilityDiscoveryService {

    private static final WayangProviderCapabilityDiscoveryService INSTANCE =
            new WayangProviderCapabilityDiscoveryService();

    private WayangProviderCapabilityDiscoveryService() {
    }

    public static WayangProviderCapabilityDiscoveryService create() {
        return INSTANCE;
    }

    public WayangProviderCapabilityDiscovery discover(
            WayangProviderCapabilityRegistry registry,
            WayangProviderCapabilityQuery query) {
        return discover(registry, query, "");
    }

    public WayangProviderCapabilityDiscovery discover(
            WayangProviderCapabilityRegistry registry,
            WayangProviderCapabilityQuery query,
            String search) {
        WayangProviderCapabilityRegistry source = registry == null
                ? WayangProviderCapabilityRegistry.create()
                : registry;
        WayangProviderCapabilityQuery normalized = query == null
                ? WayangProviderCapabilityQuery.all()
                : query;
        List<WayangProviderCapabilityDescriptor> filtered = filterSearch(source.discover(normalized), search);
        return WayangProviderCapabilityDiscovery.of(normalized, search, filtered, source.size());
    }

    public List<WayangProviderCapabilityDescriptor> filterSearch(
            List<WayangProviderCapabilityDescriptor> capabilities,
            String search) {
        String term = SdkText.trimToEmpty(search).toLowerCase(Locale.ROOT);
        if (term.isEmpty()) {
            return SdkLists.copy(capabilities);
        }
        return SdkLists.copy(capabilities).stream()
                .filter(capability -> matchesSearch(capability, term))
                .toList();
    }

    private static boolean matchesSearch(WayangProviderCapabilityDescriptor capability, String term) {
        return contains(capability.id(), term)
                || contains(capability.providerId(), term)
                || contains(capability.providerNamespace(), term)
                || contains(capability.moduleId(), term)
                || contains(capability.capabilityType(), term)
                || contains(capability.name(), term)
                || contains(capability.description(), term)
                || contains(capability.state().id(), term)
                || capability.surfaceIds().stream().anyMatch(value -> contains(value, term))
                || capability.standardIds().stream().anyMatch(value -> contains(value, term))
                || capability.tags().stream().anyMatch(value -> contains(value, term))
                || metadataMatches(capability.metadata(), term);
    }

    private static boolean metadataMatches(Map<String, Object> metadata, String term) {
        return SdkMaps.copy(metadata).entrySet().stream()
                .anyMatch(entry -> contains(entry.getKey(), term)
                        || contains(String.valueOf(entry.getValue()), term));
    }

    private static boolean contains(String value, String term) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(term);
    }
}
