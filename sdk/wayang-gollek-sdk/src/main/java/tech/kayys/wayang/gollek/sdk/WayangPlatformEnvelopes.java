package tech.kayys.wayang.gollek.sdk;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Wire envelope factory for platform status, product catalog, profile, and readiness payloads.
 */
public final class WayangPlatformEnvelopes {

    private static final String PRODUCT = "Wayang";
    private static final String CORE_ENGINE =
            "agents, skills, tools, MCP, RAG, memory, workflows, and harness checks";

    private WayangPlatformEnvelopes() {
    }

    public static Map<String, Object> status(WayangPlatformStatus status) {
        WayangPlatformStatus model = normalize(status);
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("product", model.productName());
        values.put("version", model.version());
        values.put("gollek", model.gollek().state());
        values.put("gamelan", model.gamelan().state());
        values.put("agentCore", model.agentCore().state());
        values.put("rag", model.rag().state());
        values.put("mcp", model.mcp().state());
        values.put("activeSkills", model.activeSkills());
        return SdkMaps.orderedCopy(values);
    }

    public static Map<String, Object> productCatalog(
            List<ProductSurface> surfaces,
            List<ProductSurfacePolicy> policies) {
        return productCatalog(surfaces, policies, List.of());
    }

    public static Map<String, Object> productCatalog(
            List<ProductSurface> surfaces,
            List<ProductSurfacePolicy> policies,
            List<ProductProfile> profiles) {
        Map<String, ProductSurfacePolicy> policyBySurface = policiesBySurface(policies);
        Map<String, List<ProductProfile>> profilesBySurface = profilesBySurface(profiles);
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("product", PRODUCT);
        values.put("coreEngine", CORE_ENGINE);
        values.put("surfaces", SdkLists.copy(surfaces).stream()
                .map(surface -> surface(
                        surface,
                        policyBySurface.get(surface.id()),
                        profilesBySurface.getOrDefault(surface.id(), List.of())))
                .toList());
        values.put("profiles", SdkLists.copy(profiles).stream()
                .map(WayangPlatformEnvelopes::profile)
                .toList());
        return SdkMaps.orderedCopy(values);
    }

    public static Map<String, Object> profiles(
            String productName,
            String surfaceId,
            List<ProductProfile> profiles) {
        List<ProductProfile> model = SdkLists.copy(profiles);
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("product", SdkText.trimToEmpty(productName));
        values.put("surfaceId", SdkText.blankToNull(surfaceId));
        values.put("totalProfiles", model.size());
        values.put("profiles", model.stream()
                .map(WayangPlatformEnvelopes::profile)
                .toList());
        return SdkMaps.orderedCopy(values);
    }

    public static Map<String, Object> profileDetail(String productName, ProductProfile profile) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("product", SdkText.trimToEmpty(productName));
        values.put("profileId", profile.id());
        values.put("profile", profile(profile));
        return SdkMaps.orderedCopy(values);
    }

    public static Map<String, Object> sdkBoundaryCatalog(List<WayangSdkBoundary> boundaries) {
        List<WayangSdkBoundary> model = SdkLists.copy(boundaries);
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("product", PRODUCT);
        values.put("rootPackage", WayangSdkBoundaryCatalog.SDK_ROOT_PACKAGE);
        values.put("defaultBoundaryId", WayangSdkBoundaryCatalog.DEFAULT_BOUNDARY_ID);
        values.put("totalBoundaries", model.size());
        values.put("boundaryIds", model.stream()
                .map(WayangSdkBoundary::id)
                .toList());
        values.put("boundaries", model.stream()
                .map(WayangPlatformEnvelopes::sdkBoundary)
                .toList());
        return SdkMaps.orderedCopy(values);
    }

    public static Map<String, Object> sdkBoundaryDetail(WayangSdkBoundary boundary) {
        WayangSdkBoundary model = boundary == null
                ? WayangSdkBoundaryCatalog.require(WayangSdkBoundaryCatalog.DEFAULT_BOUNDARY_ID)
                : boundary;
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("product", PRODUCT);
        values.put("boundaryId", model.id());
        values.put("boundary", sdkBoundary(model));
        return SdkMaps.orderedCopy(values);
    }

    public static Map<String, Object> sdkBoundaryCatalogValidation(
            WayangSdkBoundaryCatalogValidationReport report) {
        WayangSdkBoundaryCatalogValidationReport model = report == null
                ? WayangSdkBoundaryCatalog.validateDefault()
                : report;
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("product", PRODUCT);
        values.put("valid", model.valid());
        values.put("issueCount", model.issueCount());
        values.put("rootPackage", WayangSdkBoundaryCatalog.SDK_ROOT_PACKAGE);
        values.put("defaultBoundaryId", WayangSdkBoundaryCatalog.DEFAULT_BOUNDARY_ID);
        values.put("totalBoundaries", model.totalBoundaries());
        values.put("boundaryIds", model.boundaryIds());
        values.put("intendedPackageCount", model.intendedPackageCount());
        values.put("intendedPackages", model.intendedPackages());
        values.put("classPrefixCount", model.classPrefixCount());
        values.put("classPrefixes", model.classPrefixes());
        values.put("contractSchemaCount", model.contractSchemaCount());
        values.put("contractSchemas", model.contractSchemas());
        values.put("issues", model.issues().stream()
                .map(WayangSdkBoundaryCatalogValidationIssue::toMap)
                .toList());
        return SdkMaps.orderedCopy(values);
    }

    public static Map<String, Object> codeAgentExtensions(
            WayangCodeAgentExtensionDiscovery discovery) {
        WayangCodeAgentExtensionDiscovery model = discovery == null
                ? WayangCodeAgentExtensionDiscovery.empty()
                : discovery;
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("product", PRODUCT);
        values.put("surfaceId", "coding-agent");
        values.putAll(model.toMap());
        return SdkMaps.orderedCopy(values);
    }

    public static Map<String, Object> readinessProfiles(
            List<WayangPlatformReadinessProfileDescriptor> profiles) {
        List<WayangPlatformReadinessProfileDescriptor> model = SdkLists.copy(profiles);
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("product", PRODUCT);
        values.put("totalProfiles", model.size());
        values.put("defaultProfileId", WayangPlatformReadinessProfiles.DEFAULT);
        values.put("productionProfileId", WayangPlatformReadinessProfiles.PRODUCTION);
        values.put("profileIds", model.stream()
                .map(WayangPlatformReadinessProfileDescriptor::profileId)
                .toList());
        values.put("profiles", model.stream()
                .map(WayangPlatformEnvelopes::readinessProfile)
                .toList());
        return SdkMaps.orderedCopy(values);
    }

    public static Map<String, Object> readinessProfileDetail(
            WayangPlatformReadinessProfileDescriptor profile) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("product", PRODUCT);
        values.put("profileId", profile.profileId());
        values.put("profile", readinessProfile(profile));
        return SdkMaps.orderedCopy(values);
    }

    public static Map<String, Object> readinessProfileValidation(
            WayangPlatformReadinessProfileValidationReport report) {
        WayangPlatformReadinessProfileValidationReport model = normalizeReadinessProfileValidation(report);
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("product", PRODUCT);
        values.put("valid", model.valid());
        values.put("issueCount", model.issueCount());
        values.put("totalProfiles", model.totalProfiles());
        values.put("profileIds", model.profileIds());
        values.put("validationPolicy", readinessProfileValidationPolicy(model.validationPolicy()));
        values.put("defaultProfileCount", model.defaultProfileCount());
        values.put("defaultProfileIds", model.defaultProfileIds());
        values.put("productionProfileCount", model.productionProfileCount());
        values.put("productionProfileIds", model.productionProfileIds());
        values.put("knownReadinessIds", model.knownReadinessIds());
        values.put("coveredReadinessCount", model.coveredReadinessCount());
        values.put("coveredReadinessIds", model.coveredReadinessIds());
        values.put("uncoveredReadinessCount", model.uncoveredReadinessCount());
        values.put("uncoveredReadinessIds", model.uncoveredReadinessIds());
        values.put("issues", model.issues().stream()
                .map(WayangPlatformEnvelopes::readinessProfileValidationIssue)
                .toList());
        return SdkMaps.orderedCopy(values);
    }

    public static Map<String, Object> readinessProfileValidationPolicies(
            List<WayangPlatformReadinessProfileValidationPolicyDescriptor> policies) {
        List<WayangPlatformReadinessProfileValidationPolicyDescriptor> model = SdkLists.copy(policies);
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("product", PRODUCT);
        values.put("totalPolicies", model.size());
        values.put("defaultPolicyId", WayangPlatformReadinessProfileValidationPolicies.STRICT);
        values.put("policyIds", model.stream()
                .map(WayangPlatformReadinessProfileValidationPolicyDescriptor::policyId)
                .toList());
        values.put("policies", model.stream()
                .map(WayangPlatformEnvelopes::readinessProfileValidationPolicyDescriptor)
                .toList());
        return SdkMaps.orderedCopy(values);
    }

    public static Map<String, Object> readinessProfileRegistryResolution(
            WayangPlatformReadinessProfileRegistryResolution resolution) {
        WayangPlatformReadinessProfileRegistryResolution model = resolution == null
                ? WayangPlatformReadinessProfileRegistry.defaultRegistry().resolve()
                : resolution;
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("product", PRODUCT);
        values.put("valid", model.valid());
        values.put("activeSourceId", model.activeSourceId());
        values.put("activeSourceType", model.activeSourceType());
        values.put("activeSourceLocation", model.activeSourceLocation());
        values.put("fallbackUsed", model.fallbackUsed());
        values.put("sourceCount", model.sourceCount());
        values.put("sources", model.sources().stream()
                .map(WayangPlatformEnvelopes::readinessProfileSourceStatus)
                .toList());
        values.put("totalProfiles", model.totalProfiles());
        values.put("profileIds", model.profiles().stream()
                .map(WayangPlatformReadinessProfileDescriptor::profileId)
                .toList());
        values.put("profiles", model.profiles().stream()
                .map(WayangPlatformEnvelopes::readinessProfile)
                .toList());
        values.put("validation", readinessProfileValidation(model.validation()));
        return SdkMaps.orderedCopy(values);
    }

    public static Map<String, Object> readinessProfileRegistryConfigDiagnostics(
            WayangPlatformReadinessProfileRegistryConfigDiagnostics diagnostics) {
        WayangPlatformReadinessProfileRegistryConfigDiagnostics model = diagnostics == null
                ? WayangPlatformReadinessProfileRegistryConfig.builtin().diagnostics()
                : diagnostics;
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("product", PRODUCT);
        values.put("valid", model.valid());
        values.put("issueCount", model.issueCount());
        values.put("config", model.config().toMap());
        values.put("issues", model.issues().stream()
                .map(WayangPlatformReadinessProfileRegistryConfigIssue::toMap)
                .toList());
        return SdkMaps.orderedCopy(values);
    }

    public static Map<String, Object> readinessProfileExternalReaderProviderDiscovery(
            WayangPlatformReadinessProfileExternalReaderProviderDiscoveryReport report) {
        WayangPlatformReadinessProfileExternalReaderProviderDiscoveryReport model = report == null
                ? WayangPlatformReadinessProfileExternalReaderProviderDiscoveryReport.empty()
                : report;
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("product", PRODUCT);
        values.putAll(model.toMap());
        return SdkMaps.orderedCopy(values);
    }

    public static Map<String, Object> readinessProfileRegistryPreflight(
            WayangPlatformReadinessProfileRegistryPreflightReport report) {
        WayangPlatformReadinessProfileRegistryPreflightReport model = report == null
                ? WayangPlatformReadinessProfileRegistryPreflightReport.of(
                        WayangPlatformReadinessProfileRegistryConfig.builtin().diagnostics(),
                        WayangPlatformReadinessProfileExternalReaderProviderDiscoveryReport.empty(),
                        WayangPlatformReadinessProfileRegistry.defaultRegistry().resolve())
                : report;
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("product", PRODUCT);
        values.putAll(model.toMap());
        return SdkMaps.orderedCopy(values);
    }

    public static Map<String, Object> profile(ProductProfile profile) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("id", profile.id());
        values.put("name", profile.name());
        values.put("surfaceId", profile.surfaceId());
        values.put("description", profile.description());
        values.put("starterPrompt", profile.starterPrompt());
        values.put("workflowId", profile.workflowId());
        values.put("skills", profile.skills());
        values.put("memoryEnabled", profile.memoryEnabled());
        values.put("workspaceEnabled", profile.workspaceEnabled());
        values.put("harnessEnabled", profile.harnessEnabled());
        values.put("harnessIncludeOptional", profile.harnessIncludeOptional());
        values.put("requireReady", profile.requireReady());
        values.put("maxSteps", profile.maxSteps());
        values.put("workspaceMaxEntries", profile.workspaceMaxEntries());
        values.put("harnessMaxChecks", profile.harnessMaxChecks());
        values.put("context", profile.context());
        values.put("notes", profile.notes());
        return SdkMaps.orderedCopy(values);
    }

    public static Map<String, Object> readinessProfile(WayangPlatformReadinessProfileDescriptor profile) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("profileId", profile.profileId());
        values.put("description", profile.description());
        values.put("defaultProfile", profile.defaultProfile());
        values.put("productionProfile", profile.productionProfile());
        values.put("componentCount", profile.componentCount());
        values.put("readinessIds", profile.readinessIds());
        return SdkMaps.orderedCopy(values);
    }

    public static Map<String, Object> sdkBoundary(WayangSdkBoundary boundary) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("id", boundary.id());
        values.put("name", boundary.name());
        values.put("intendedPackage", boundary.intendedPackage());
        values.put("responsibility", boundary.responsibility());
        values.put("classPrefixes", boundary.classPrefixes());
        values.put("contractSchemas", boundary.contractSchemas());
        values.put("dependsOn", boundary.dependsOn());
        return SdkMaps.orderedCopy(values);
    }

    public static Map<String, Object> readinessProfileValidationIssue(
            WayangPlatformReadinessProfileValidationIssue issue) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("kind", issue.kind());
        values.put("message", issue.message());
        values.put("profileId", issue.profileId());
        values.put("readinessId", issue.readinessId());
        return SdkMaps.orderedCopy(values);
    }

    public static Map<String, Object> readinessProfileValidationPolicy(
            WayangPlatformReadinessProfileValidationPolicySummary policy) {
        WayangPlatformReadinessProfileValidationPolicySummary model = policy == null
                ? WayangPlatformReadinessProfileValidationPolicySummary.empty()
                : policy;
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("policyId", model.policyId());
        values.put("strict", model.strict());
        values.put("knownReadinessCount", model.knownReadinessCount());
        values.put("requireDefaultProfile", model.requireDefaultProfile());
        values.put("requireProductionProfile", model.requireProductionProfile());
        values.put("requireFullReadinessCoverage", model.requireFullReadinessCoverage());
        return SdkMaps.orderedCopy(values);
    }

    public static Map<String, Object> readinessProfileValidationPolicyDescriptor(
            WayangPlatformReadinessProfileValidationPolicyDescriptor policy) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("policyId", policy.policyId());
        values.put("description", policy.description());
        values.put("defaultPolicy", policy.defaultPolicy());
        values.put("strict", policy.strict());
        values.put("knownReadinessCount", policy.knownReadinessCount());
        values.put("requireDefaultProfile", policy.requireDefaultProfile());
        values.put("requireProductionProfile", policy.requireProductionProfile());
        values.put("requireFullReadinessCoverage", policy.requireFullReadinessCoverage());
        return SdkMaps.orderedCopy(values);
    }

    public static Map<String, Object> readinessProfileSourceStatus(
            WayangPlatformReadinessProfileSourceStatus source) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("sourceId", source.sourceId());
        values.put("sourceType", source.sourceType());
        values.put("location", source.location());
        values.put("selected", source.selected());
        values.put("fallback", source.fallback());
        values.put("available", source.available());
        values.put("valid", source.valid());
        values.put("profileCount", source.profileCount());
        values.put("issueCount", source.issueCount());
        values.put("message", source.message());
        return SdkMaps.orderedCopy(values);
    }


    public static Map<String, Object> surface(
            ProductSurface surface,
            ProductSurfacePolicy policy,
            List<ProductProfile> profiles) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("id", surface.id());
        values.put("name", surface.name());
        values.put("role", surface.role());
        values.put("engineCapabilities", surface.engineCapabilities());
        values.put("adapterBoundaries", surface.adapterBoundaries());
        if (policy != null) {
            values.put("policy", policy(policy));
        }
        values.put("profiles", SdkLists.copy(profiles).stream()
                .map(ProductProfile::id)
                .toList());
        return SdkMaps.orderedCopy(values);
    }

    public static Map<String, Object> policy(ProductSurfacePolicy policy) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("surfaceId", policy.surfaceId());
        values.put("memoryPreferred", policy.memoryPreferred());
        values.put("workspacePreferred", policy.workspacePreferred());
        values.put("harnessPreferred", policy.harnessPreferred());
        values.put("workflowPreferred", policy.workflowPreferred());
        values.put("suggestedSkills", policy.suggestedSkills());
        values.put("requiredContextKeys", policy.requiredContextKeys());
        values.put("routingHints", policy.routingHints());
        return SdkMaps.orderedCopy(values);
    }

    private static WayangPlatformStatus normalize(WayangPlatformStatus status) {
        return status == null
                ? new WayangPlatformStatus(null, null, null, null, null, null, null, 0, List.of())
                : status;
    }

    private static WayangPlatformReadinessProfileValidationReport normalizeReadinessProfileValidation(
            WayangPlatformReadinessProfileValidationReport report) {
        return report == null
                ? new WayangPlatformReadinessProfileValidationReport(
                        0,
                        List.of(),
                        WayangPlatformReadinessProfileValidationPolicySummary.empty(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of())
                : report;
    }

    private static Map<String, ProductSurfacePolicy> policiesBySurface(List<ProductSurfacePolicy> policies) {
        Map<String, ProductSurfacePolicy> indexed = new LinkedHashMap<>();
        for (ProductSurfacePolicy policy : SdkLists.copy(policies)) {
            indexed.put(policy.surfaceId(), policy);
        }
        return indexed;
    }

    private static Map<String, List<ProductProfile>> profilesBySurface(List<ProductProfile> profiles) {
        Map<String, List<ProductProfile>> indexed = new LinkedHashMap<>();
        for (ProductProfile profile : SdkLists.copy(profiles)) {
            indexed.compute(profile.surfaceId(), (surfaceId, existing) -> {
                List<ProductProfile> current = existing == null ? List.of() : existing;
                java.util.ArrayList<ProductProfile> next = new java.util.ArrayList<>(current);
                next.add(profile);
                return List.copyOf(next);
            });
        }
        return indexed;
    }

}
