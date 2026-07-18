package tech.kayys.wayang.capability;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class WayangProviderCapabilityRegistry {

    private final Map<String, WayangProviderCapabilityDescriptor> capabilities = new LinkedHashMap<>();

    private WayangProviderCapabilityRegistry(List<WayangProviderCapabilityDescriptor> initialCapabilities) {
        SdkLists.copy(initialCapabilities).forEach(this::register);
    }

    public static WayangProviderCapabilityRegistry create() {
        return new WayangProviderCapabilityRegistry(List.of());
    }

    public static WayangProviderCapabilityRegistry of(List<WayangProviderCapabilityDescriptor> capabilities) {
        return new WayangProviderCapabilityRegistry(capabilities);
    }

    public synchronized WayangProviderCapabilityDescriptor register(WayangProviderCapabilityDescriptor capability) {
        if (capability == null) {
            throw new IllegalArgumentException("Provider capability is required.");
        }
        if (capabilities.containsKey(capability.id())) {
            throw new IllegalArgumentException("Duplicate Wayang provider capability id '" + capability.id() + "'.");
        }
        capabilities.put(capability.id(), capability);
        return capability;
    }

    public synchronized boolean unregister(String capabilityId) {
        Optional<WayangProviderCapabilityDescriptor> existing = find(capabilityId);
        existing.ifPresent(capability -> capabilities.remove(capability.id()));
        return existing.isPresent();
    }

    public synchronized Optional<WayangProviderCapabilityDescriptor> find(String capabilityId) {
        String normalized = new WayangProviderCapabilityQuery(
                capabilityId,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null).capabilityId();
        return normalized.isEmpty() ? Optional.empty() : Optional.ofNullable(capabilities.get(normalized));
    }

    public synchronized WayangProviderCapabilityDescriptor require(String capabilityId) {
        String normalized = new WayangProviderCapabilityQuery(
                capabilityId,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null).capabilityId();
        return find(normalized)
                .orElseThrow(() -> new IllegalArgumentException("Unknown Wayang provider capability id '"
                        + normalized + "'. Known capability ids: " + String.join(", ", capabilityIds())));
    }

    public synchronized boolean contains(String capabilityId) {
        return find(capabilityId).isPresent();
    }

    public synchronized int size() {
        return capabilities.size();
    }

    public synchronized List<WayangProviderCapabilityDescriptor> list() {
        return List.copyOf(capabilities.values());
    }

    public synchronized List<WayangProviderCapabilityDescriptor> discover(WayangProviderCapabilityQuery query) {
        WayangProviderCapabilityQuery normalized = query == null ? WayangProviderCapabilityQuery.all() : query;
        return capabilities.values().stream()
                .filter(capability -> matches(capability, normalized))
                .toList();
    }

    public synchronized List<String> capabilityIds() {
        return List.copyOf(capabilities.keySet());
    }

    public synchronized List<String> providerIds() {
        return SdkFacets.values(list(), WayangProviderCapabilityDescriptor::providerId);
    }

    public synchronized List<String> providerNamespaces() {
        return SdkFacets.values(list(), WayangProviderCapabilityDescriptor::providerNamespace);
    }

    public synchronized List<String> moduleIds() {
        return SdkFacets.values(list(), WayangProviderCapabilityDescriptor::moduleId);
    }

    public synchronized List<String> capabilityTypes() {
        return SdkFacets.values(list(), WayangProviderCapabilityDescriptor::capabilityType);
    }

    public synchronized List<String> surfaceIds() {
        return SdkFacets.flatValues(list(), WayangProviderCapabilityDescriptor::surfaceIds);
    }

    public synchronized List<String> standardIds() {
        return SdkFacets.flatValues(list(), WayangProviderCapabilityDescriptor::standardIds);
    }

    public synchronized List<Map<String, Object>> maps(WayangProviderCapabilityQuery query) {
        return discover(query).stream()
                .map(WayangProviderCapabilityDescriptor::toMap)
                .toList();
    }

    private static boolean matches(
            WayangProviderCapabilityDescriptor capability,
            WayangProviderCapabilityQuery query) {
        return (!query.hasCapabilityId() || capability.id().equals(query.capabilityId()))
                && (!query.hasProviderId() || capability.providerId().equals(query.providerId()))
                && (!query.hasProviderNamespace() || capability.providerNamespace().equals(query.providerNamespace()))
                && (!query.hasModuleId() || capability.moduleId().equals(query.moduleId()))
                && (!query.hasCapabilityType() || capability.capabilityType().equals(query.capabilityType()))
                && (query.state() == null || capability.state() == query.state())
                && (!query.hasSurfaceId() || capability.supportsSurface(query.surfaceId()))
                && (!query.hasStandardId() || capability.supportsStandard(query.standardId()))
                && (!query.hasTag() || capability.hasTag(query.tag()));
    }
}
