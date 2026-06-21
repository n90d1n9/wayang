package tech.kayys.wayang.gollek.sdk;

import java.util.List;
import java.util.Map;

/**
 * Platform API for product surfaces, status, profiles, and readiness profile metadata.
 *
 * <p>The API groups product-level SDK operations and owns the platform JSON
 * envelope boundary without absorbing run lifecycle or workspace responsibilities.</p>
 */
public final class WayangPlatformApi {

    private final WayangGollekSdk sdk;
    private final WayangWireApi wire;

    WayangPlatformApi(WayangGollekSdk sdk, WayangWireApi wire) {
        this.sdk = sdk == null ? Wayang.local() : sdk;
        this.wire = wire == null ? new WayangWireApi() : wire;
    }

    public WayangPlatformStatus status() {
        return sdk.status();
    }

    public WayangReadinessReport readiness() {
        return sdk.platformReadiness();
    }

    public WayangReadinessReport readiness(String profileId) {
        return sdk.platformReadiness(profileId);
    }

    public List<ProductSurface> productSurfaces() {
        return sdk.productSurfaces();
    }

    public List<ProductSurfacePolicy> productSurfacePolicies() {
        return sdk.productSurfacePolicies();
    }

    public List<ProductProfile> productProfiles() {
        return sdk.productProfiles();
    }

    public List<ProductProfile> productProfilesForSurface(String surfaceId) {
        return sdk.productProfilesForSurface(surfaceId);
    }

    public ProductProfile productProfile(String profileId) {
        return sdk.productProfile(profileId);
    }

    public List<WayangSdkBoundary> sdkBoundaries() {
        return sdk.sdkBoundaries();
    }

    public WayangSdkBoundary sdkBoundary(String boundaryId) {
        return sdk.sdkBoundary(boundaryId);
    }

    public Map<String, Object> sdkBoundaryCatalogEnvelope() {
        return WayangPlatformEnvelopes.sdkBoundaryCatalog(sdkBoundaries());
    }

    public Map<String, Object> sdkBoundaryEnvelope(String boundaryId) {
        return WayangPlatformEnvelopes.sdkBoundaryDetail(sdkBoundary(boundaryId));
    }

    public WayangSdkBoundaryCatalogValidationReport sdkBoundaryCatalogValidation() {
        return sdk.sdkBoundaryCatalogValidation();
    }

    public WayangSdkBoundaryCatalogValidationReport validateSdkBoundaries(List<WayangSdkBoundary> boundaries) {
        return sdk.validateSdkBoundaries(boundaries);
    }

    public List<WayangPlatformReadinessProfileDescriptor> readinessProfiles() {
        return sdk.platformReadinessProfiles();
    }

    public WayangPlatformReadinessProfileDescriptor readinessProfile(String profileId) {
        return sdk.platformReadinessProfile(profileId);
    }

    public WayangPlatformReadinessProfileValidationReport readinessProfileValidation() {
        return sdk.platformReadinessProfileValidation();
    }

    public WayangPlatformReadinessProfileValidationReport readinessProfileValidation(String policyId) {
        return sdk.platformReadinessProfileValidation(policyId);
    }

    public List<WayangPlatformReadinessProfileValidationPolicyDescriptor> readinessProfileValidationPolicies() {
        return sdk.platformReadinessProfileValidationPolicies();
    }

    public WayangPlatformReadinessProfileValidationPolicy readinessProfileValidationPolicy(String policyId) {
        return WayangPlatformReadinessProfileValidationPolicies.policy(policyId);
    }

    public WayangPlatformReadinessProfileRegistryConfigDiagnostics readinessProfileRegistryConfigDiagnostics() {
        return sdk.platformReadinessProfileRegistryConfigDiagnostics();
    }

    public WayangPlatformReadinessProfileExternalReaderProviderDiscoveryReport
            readinessProfileExternalReaderProviderDiscovery() {
        return sdk.platformReadinessProfileExternalReaderProviderDiscovery();
    }

    public WayangPlatformReadinessProfileRegistryPreflightReport readinessProfileRegistryPreflight() {
        return sdk.platformReadinessProfileRegistryPreflight();
    }

    public WayangPlatformReadinessProfileRegistryResolution readinessProfileRegistryResolution() {
        return sdk.platformReadinessProfileRegistryResolution();
    }

    public WayangPlatformReadinessProfileRegistryResolution readinessProfileRegistryResolution(
            WayangPlatformReadinessProfileValidationPolicy policy) {
        return policy == null
                ? readinessProfileRegistryResolution()
                : sdk.platformReadinessProfileRegistry().resolve(policy);
    }

    public WayangPlatformReadinessProfileRegistryResolution readinessProfileRegistryResolution(
            WayangPlatformReadinessProfileSource source,
            WayangPlatformReadinessProfileValidationPolicy policy) {
        return sdk.platformReadinessProfileRegistryResolution(source, policy);
    }

    public Map<String, Object> statusEnvelope(WayangPlatformStatus status) {
        return WayangPlatformEnvelopes.status(status);
    }

    public Map<String, Object> readinessEnvelope(WayangReadinessReport report) {
        return report == null ? Map.of() : report.toMap();
    }

    public Map<String, Object> productCatalogEnvelope() {
        return WayangPlatformEnvelopes.productCatalog(
                productSurfaces(),
                productSurfacePolicies(),
                productProfiles());
    }

    public Map<String, Object> profilesEnvelope(String surfaceId, List<ProductProfile> profiles) {
        return WayangPlatformEnvelopes.profiles(productName(), surfaceId, profiles);
    }

    public Map<String, Object> profileDetailEnvelope(ProductProfile profile) {
        return WayangPlatformEnvelopes.profileDetail(productName(), profile);
    }

    public Map<String, Object> readinessProfilesEnvelope(
            List<WayangPlatformReadinessProfileDescriptor> profiles) {
        return WayangPlatformEnvelopes.readinessProfiles(profiles);
    }

    public Map<String, Object> readinessProfileDetailEnvelope(
            WayangPlatformReadinessProfileDescriptor profile) {
        return WayangPlatformEnvelopes.readinessProfileDetail(profile);
    }

    public Map<String, Object> readinessProfileValidationEnvelope(
            WayangPlatformReadinessProfileValidationReport report) {
        return WayangPlatformEnvelopes.readinessProfileValidation(report);
    }

    public Map<String, Object> readinessProfileValidationPoliciesEnvelope(
            List<WayangPlatformReadinessProfileValidationPolicyDescriptor> policies) {
        return WayangPlatformEnvelopes.readinessProfileValidationPolicies(policies);
    }

    public Map<String, Object> readinessProfileRegistryResolutionEnvelope(
            WayangPlatformReadinessProfileRegistryResolution resolution) {
        return WayangPlatformEnvelopes.readinessProfileRegistryResolution(resolution);
    }

    public Map<String, Object> readinessProfileRegistryConfigDiagnosticsEnvelope(
            WayangPlatformReadinessProfileRegistryConfigDiagnostics diagnostics) {
        return WayangPlatformEnvelopes.readinessProfileRegistryConfigDiagnostics(diagnostics);
    }

    public Map<String, Object> readinessProfileExternalReaderProviderDiscoveryEnvelope(
            WayangPlatformReadinessProfileExternalReaderProviderDiscoveryReport report) {
        return WayangPlatformEnvelopes.readinessProfileExternalReaderProviderDiscovery(report);
    }

    public Map<String, Object> readinessProfileRegistryPreflightEnvelope(
            WayangPlatformReadinessProfileRegistryPreflightReport report) {
        return WayangPlatformEnvelopes.readinessProfileRegistryPreflight(report);
    }

    public String statusJson(WayangPlatformStatus status) {
        return wire.object(statusEnvelope(status));
    }

    public String readinessJson(WayangReadinessReport report) {
        return wire.object(readinessEnvelope(report));
    }

    public String productCatalogJson() {
        return wire.object(productCatalogEnvelope());
    }

    public String profilesJson(String surfaceId, List<ProductProfile> profiles) {
        return wire.object(profilesEnvelope(surfaceId, profiles));
    }

    public String profileDetailJson(ProductProfile profile) {
        return wire.object(profileDetailEnvelope(profile));
    }

    public String sdkBoundaryCatalogJson() {
        return wire.object(sdkBoundaryCatalogEnvelope());
    }

    public String sdkBoundaryJson(String boundaryId) {
        return wire.object(sdkBoundaryEnvelope(boundaryId));
    }

    public Map<String, Object> sdkBoundaryCatalogValidationEnvelope() {
        return WayangPlatformEnvelopes.sdkBoundaryCatalogValidation(sdkBoundaryCatalogValidation());
    }

    public String sdkBoundaryCatalogValidationJson() {
        return wire.object(sdkBoundaryCatalogValidationEnvelope());
    }

    public String readinessProfilesJson(List<WayangPlatformReadinessProfileDescriptor> profiles) {
        return wire.object(readinessProfilesEnvelope(profiles));
    }

    public String readinessProfileDetailJson(WayangPlatformReadinessProfileDescriptor profile) {
        return wire.object(readinessProfileDetailEnvelope(profile));
    }

    public String readinessProfileValidationJson(WayangPlatformReadinessProfileValidationReport report) {
        return wire.object(readinessProfileValidationEnvelope(report));
    }

    public String readinessProfileValidationPoliciesJson(
            List<WayangPlatformReadinessProfileValidationPolicyDescriptor> policies) {
        return wire.object(readinessProfileValidationPoliciesEnvelope(policies));
    }

    public String readinessProfileRegistryResolutionJson(
            WayangPlatformReadinessProfileRegistryResolution resolution) {
        return wire.object(readinessProfileRegistryResolutionEnvelope(resolution));
    }

    public String readinessProfileRegistryConfigDiagnosticsJson(
            WayangPlatformReadinessProfileRegistryConfigDiagnostics diagnostics) {
        return wire.object(readinessProfileRegistryConfigDiagnosticsEnvelope(diagnostics));
    }

    public String readinessProfileExternalReaderProviderDiscoveryJson(
            WayangPlatformReadinessProfileExternalReaderProviderDiscoveryReport report) {
        return wire.object(readinessProfileExternalReaderProviderDiscoveryEnvelope(report));
    }

    public String readinessProfileRegistryPreflightJson(
            WayangPlatformReadinessProfileRegistryPreflightReport report) {
        return wire.object(readinessProfileRegistryPreflightEnvelope(report));
    }

    private String productName() {
        return sdk.status().productName();
    }
}
