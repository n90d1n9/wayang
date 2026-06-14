package tech.kayys.wayang.gollek.sdk;

import java.util.Locale;

public record WayangProviderCapabilityQuery(
        String capabilityId,
        String providerId,
        String providerNamespace,
        String moduleId,
        String capabilityType,
        WayangProviderCapabilityState state,
        String surfaceId,
        String standardId,
        String tag) {

    public WayangProviderCapabilityQuery {
        capabilityId = normalizeIdentifier(capabilityId);
        providerId = normalizeIdentifier(providerId);
        providerNamespace = normalizeIdentifier(providerNamespace);
        moduleId = normalizeIdentifier(moduleId);
        capabilityType = normalizeIdentifier(capabilityType);
        surfaceId = normalizeIdentifier(surfaceId);
        standardId = normalizeStandardId(standardId);
        tag = normalizeIdentifier(tag);
    }

    public static WayangProviderCapabilityQuery all() {
        return new WayangProviderCapabilityQuery(null, null, null, null, null, null, null, null, null);
    }

    public static WayangProviderCapabilityQuery forCapability(String capabilityId) {
        return new WayangProviderCapabilityQuery(capabilityId, null, null, null, null, null, null, null, null);
    }

    public static WayangProviderCapabilityQuery forProvider(String providerId) {
        return new WayangProviderCapabilityQuery(null, providerId, null, null, null, null, null, null, null);
    }

    public static WayangProviderCapabilityQuery forModule(String moduleId) {
        return new WayangProviderCapabilityQuery(null, null, null, moduleId, null, null, null, null, null);
    }

    public static WayangProviderCapabilityQuery forStandard(String standardId) {
        return new WayangProviderCapabilityQuery(null, null, null, null, null, null, null, standardId, null);
    }

    public static WayangProviderCapabilityQuery forSurface(String surfaceId) {
        return new WayangProviderCapabilityQuery(null, null, null, null, null, null, surfaceId, null, null);
    }

    public boolean filtered() {
        return hasCapabilityId()
                || hasProviderId()
                || hasProviderNamespace()
                || hasModuleId()
                || hasCapabilityType()
                || state != null
                || hasSurfaceId()
                || hasStandardId()
                || hasTag();
    }

    public boolean hasCapabilityId() {
        return !capabilityId.isEmpty();
    }

    public boolean hasProviderId() {
        return !providerId.isEmpty();
    }

    public boolean hasProviderNamespace() {
        return !providerNamespace.isEmpty();
    }

    public boolean hasModuleId() {
        return !moduleId.isEmpty();
    }

    public boolean hasCapabilityType() {
        return !capabilityType.isEmpty();
    }

    public boolean hasSurfaceId() {
        return !surfaceId.isEmpty();
    }

    public boolean hasStandardId() {
        return !standardId.isEmpty();
    }

    public boolean hasTag() {
        return !tag.isEmpty();
    }

    private static String normalizeIdentifier(String value) {
        return SdkText.trimToEmpty(value).toLowerCase(Locale.ROOT);
    }

    private static String normalizeStandardId(String value) {
        String normalized = normalizeIdentifier(value);
        return normalized.isEmpty() ? "" : WayangStandardRegistry.canonicalId(normalized);
    }
}
