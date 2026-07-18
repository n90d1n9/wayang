package tech.kayys.wayang.client;

import java.util.List;
import java.util.Map;

import tech.kayys.wayang.contract.WayangContractDescriptor;
import tech.kayys.wayang.contract.WayangContractJsonSchema;

public final class WayangPlatformJsonSchema {

    private WayangPlatformJsonSchema() {
    }

    public static boolean matches(WayangContractDescriptor contract) {
        return WayangPlatformContract.SCHEMA.equals(contract.schema());
    }

    public static WayangContractJsonSchema schema(WayangContractDescriptor contract) {
        return WayangJsonSchemaDocuments.envelopeSchema(
                contract,
                required(contract.envelope()),
                properties(contract.envelope()));
    }

    private static List<String> required(String envelope) {
        return switch (envelope) {
            case WayangPlatformContract.PLATFORM_STATUS -> List.of(
                    "product",
                    "version",
                    "gollek",
                    "gamelan",
                    "agentCore",
                    "rag",
                    "mcp",
                    "activeSkills");
            case WayangPlatformContract.PRODUCT_CATALOG -> List.of(
                    "product",
                    "coreEngine",
                    "surfaces",
                    "profiles");
            case WayangPlatformContract.PROFILE_LIST -> List.of(
                    "product",
                    "surfaceId",
                    "totalProfiles",
                    "profiles");
            case WayangPlatformContract.PROFILE_DETAIL -> List.of(
                    "product",
                    "profileId",
                    "profile");
            case WayangPlatformContract.SDK_BOUNDARY_CATALOG -> List.of(
                    "product",
                    "rootPackage",
                    "defaultBoundaryId",
                    "totalBoundaries",
                    "boundaryIds",
                    "boundaries");
            case WayangPlatformContract.SDK_BOUNDARY_DETAIL -> List.of(
                    "product",
                    "boundaryId",
                    "boundary");
            case WayangPlatformContract.READINESS_PROFILE_LIST -> List.of(
                    "product",
                    "totalProfiles",
                    "defaultProfileId",
                    "productionProfileId",
                    "profileIds",
                    "profiles");
            case WayangPlatformContract.READINESS_PROFILE_DETAIL -> List.of(
                    "product",
                    "profileId",
                    "profile");
            case WayangPlatformContract.READINESS_PROFILE_VALIDATION -> List.of(
                    "product",
                    "valid",
                    "issueCount",
                    "totalProfiles",
                    "profileIds",
                    "validationPolicy",
                    "defaultProfileCount",
                    "defaultProfileIds",
                    "productionProfileCount",
                    "productionProfileIds",
                    "knownReadinessIds",
                    "coveredReadinessCount",
                    "coveredReadinessIds",
                    "uncoveredReadinessCount",
                    "uncoveredReadinessIds",
                    "issues");
            case WayangPlatformContract.READINESS_PROFILE_VALIDATION_POLICY_LIST -> List.of(
                    "product",
                    "totalPolicies",
                    "defaultPolicyId",
                    "policyIds",
                    "policies");
            case WayangPlatformContract.READINESS_PROFILE_REGISTRY_RESOLUTION -> List.of(
                    "product",
                    "valid",
                    "activeSourceId",
                    "activeSourceType",
                    "activeSourceLocation",
                    "fallbackUsed",
                    "sourceCount",
                    "sources",
                    "totalProfiles",
                    "profileIds",
                    "profiles",
                    "validation");
            case WayangPlatformContract.READINESS_PROFILE_REGISTRY_CONFIG_DIAGNOSTICS -> List.of(
                    "product",
                    "valid",
                    "issueCount",
                    "config",
                    "issues");
            default -> List.of();
        };
    }

    private static Map<String, Object> properties(String envelope) {
        return switch (envelope) {
            case WayangPlatformContract.PLATFORM_STATUS -> WayangPlatformJsonSchemaProperties.compactStatusProperties();
            case WayangPlatformContract.PRODUCT_CATALOG -> WayangPlatformJsonSchemaProperties.catalogProperties();
            case WayangPlatformContract.PROFILE_LIST -> WayangPlatformJsonSchemaProperties.profileListProperties();
            case WayangPlatformContract.PROFILE_DETAIL -> WayangPlatformJsonSchemaProperties.profileDetailProperties();
            case WayangPlatformContract.SDK_BOUNDARY_CATALOG ->
                    WayangPlatformJsonSchemaProperties.sdkBoundaryCatalogProperties();
            case WayangPlatformContract.SDK_BOUNDARY_DETAIL ->
                    WayangPlatformJsonSchemaProperties.sdkBoundaryDetailProperties();
            case WayangPlatformContract.READINESS_PROFILE_LIST ->
                    WayangPlatformJsonSchemaProperties.readinessProfileListProperties();
            case WayangPlatformContract.READINESS_PROFILE_DETAIL ->
                    WayangPlatformJsonSchemaProperties.readinessProfileDetailProperties();
            case WayangPlatformContract.READINESS_PROFILE_VALIDATION ->
                    WayangPlatformJsonSchemaProperties.readinessProfileValidationProperties();
            case WayangPlatformContract.READINESS_PROFILE_VALIDATION_POLICY_LIST ->
                    WayangPlatformJsonSchemaProperties.readinessProfileValidationPolicyListProperties();
            case WayangPlatformContract.READINESS_PROFILE_REGISTRY_RESOLUTION ->
                    WayangPlatformJsonSchemaProperties.readinessProfileRegistryResolutionProperties();
            case WayangPlatformContract.READINESS_PROFILE_REGISTRY_CONFIG_DIAGNOSTICS ->
                    WayangPlatformJsonSchemaProperties.readinessProfileRegistryConfigDiagnosticsProperties();
            default -> Map.of();
        };
    }
}
