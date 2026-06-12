package tech.kayys.wayang.gollek.sdk;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class WayangPlatformJsonSchemaProperties {

    private WayangPlatformJsonSchemaProperties() {
    }

    static Map<String, Object> compactStatusProperties() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("product", WayangJsonSchemaDocuments.stringProperty());
        properties.put("version", WayangJsonSchemaDocuments.stringProperty());
        properties.put("gollek", WayangJsonSchemaDocuments.stringProperty());
        properties.put("gamelan", WayangJsonSchemaDocuments.stringProperty());
        properties.put("agentCore", WayangJsonSchemaDocuments.stringProperty());
        properties.put("rag", WayangJsonSchemaDocuments.stringProperty());
        properties.put("mcp", WayangJsonSchemaDocuments.stringProperty());
        properties.put("activeSkills", WayangJsonSchemaDocuments.nonNegativeIntegerProperty());
        return properties;
    }

    static Map<String, Object> statusProperty() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("product", WayangJsonSchemaDocuments.stringProperty());
        properties.put("version", WayangJsonSchemaDocuments.stringProperty());
        properties.put("components", WayangJsonSchemaDocuments.arrayProperty(componentProperty()));
        properties.put("activeSkills", WayangJsonSchemaDocuments.nonNegativeIntegerProperty());
        properties.put("notes", WayangJsonSchemaDocuments.stringArrayProperty());
        return WayangJsonSchemaDocuments.objectProperty(
                List.of("product", "version", "components", "activeSkills", "notes"),
                true,
                properties);
    }

    static Map<String, Object> catalogProperties() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("product", WayangJsonSchemaDocuments.stringProperty());
        properties.put("coreEngine", WayangJsonSchemaDocuments.stringProperty());
        properties.put("surfaces", WayangJsonSchemaDocuments.arrayProperty(surfaceProperty()));
        properties.put("profiles", WayangJsonSchemaDocuments.arrayProperty(profileProperty()));
        return properties;
    }

    static Map<String, Object> profileListProperties() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("product", WayangJsonSchemaDocuments.stringProperty());
        properties.put("surfaceId", WayangJsonSchemaDocuments.nullableStringProperty());
        properties.put("totalProfiles", WayangJsonSchemaDocuments.nonNegativeIntegerProperty());
        properties.put("profiles", WayangJsonSchemaDocuments.arrayProperty(profileProperty()));
        return properties;
    }

    static Map<String, Object> profileDetailProperties() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("product", WayangJsonSchemaDocuments.stringProperty());
        properties.put("profileId", WayangJsonSchemaDocuments.stringProperty());
        properties.put("profile", profileProperty());
        return properties;
    }

    static Map<String, Object> sdkBoundaryCatalogProperties() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("product", WayangJsonSchemaDocuments.stringProperty());
        properties.put("rootPackage", WayangJsonSchemaDocuments.stringProperty());
        properties.put("defaultBoundaryId", WayangJsonSchemaDocuments.stringProperty());
        properties.put("totalBoundaries", WayangJsonSchemaDocuments.nonNegativeIntegerProperty());
        properties.put("boundaryIds", WayangJsonSchemaDocuments.stringArrayProperty());
        properties.put("boundaries", WayangJsonSchemaDocuments.arrayProperty(sdkBoundaryProperty()));
        return properties;
    }

    static Map<String, Object> sdkBoundaryDetailProperties() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("product", WayangJsonSchemaDocuments.stringProperty());
        properties.put("boundaryId", WayangJsonSchemaDocuments.stringProperty());
        properties.put("boundary", sdkBoundaryProperty());
        return properties;
    }

    static Map<String, Object> readinessProfileListProperties() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("product", WayangJsonSchemaDocuments.stringProperty());
        properties.put("totalProfiles", WayangJsonSchemaDocuments.nonNegativeIntegerProperty());
        properties.put("defaultProfileId", WayangJsonSchemaDocuments.stringProperty());
        properties.put("productionProfileId", WayangJsonSchemaDocuments.stringProperty());
        properties.put("profileIds", WayangJsonSchemaDocuments.stringArrayProperty());
        properties.put("profiles", WayangJsonSchemaDocuments.arrayProperty(readinessProfileProperty()));
        return properties;
    }

    static Map<String, Object> readinessProfileDetailProperties() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("product", WayangJsonSchemaDocuments.stringProperty());
        properties.put("profileId", WayangJsonSchemaDocuments.stringProperty());
        properties.put("profile", readinessProfileProperty());
        return properties;
    }

    static Map<String, Object> readinessProfileValidationProperties() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("product", WayangJsonSchemaDocuments.stringProperty());
        properties.put("valid", WayangJsonSchemaDocuments.booleanProperty());
        properties.put("issueCount", WayangJsonSchemaDocuments.nonNegativeIntegerProperty());
        properties.put("totalProfiles", WayangJsonSchemaDocuments.nonNegativeIntegerProperty());
        properties.put("profileIds", WayangJsonSchemaDocuments.stringArrayProperty());
        properties.put("validationPolicy", readinessProfileValidationPolicyProperty());
        properties.put("defaultProfileCount", WayangJsonSchemaDocuments.nonNegativeIntegerProperty());
        properties.put("defaultProfileIds", WayangJsonSchemaDocuments.stringArrayProperty());
        properties.put("productionProfileCount", WayangJsonSchemaDocuments.nonNegativeIntegerProperty());
        properties.put("productionProfileIds", WayangJsonSchemaDocuments.stringArrayProperty());
        properties.put("knownReadinessIds", WayangJsonSchemaDocuments.stringArrayProperty());
        properties.put("coveredReadinessCount", WayangJsonSchemaDocuments.nonNegativeIntegerProperty());
        properties.put("coveredReadinessIds", WayangJsonSchemaDocuments.stringArrayProperty());
        properties.put("uncoveredReadinessCount", WayangJsonSchemaDocuments.nonNegativeIntegerProperty());
        properties.put("uncoveredReadinessIds", WayangJsonSchemaDocuments.stringArrayProperty());
        properties.put("issues", WayangJsonSchemaDocuments.arrayProperty(readinessProfileValidationIssueProperty()));
        return properties;
    }

    static Map<String, Object> readinessProfileValidationPolicyListProperties() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("product", WayangJsonSchemaDocuments.stringProperty());
        properties.put("totalPolicies", WayangJsonSchemaDocuments.nonNegativeIntegerProperty());
        properties.put("defaultPolicyId", WayangJsonSchemaDocuments.stringProperty());
        properties.put("policyIds", WayangJsonSchemaDocuments.stringArrayProperty());
        properties.put("policies", WayangJsonSchemaDocuments.arrayProperty(
                readinessProfileValidationPolicyDescriptorProperty()));
        return properties;
    }

    static Map<String, Object> readinessProfileRegistryResolutionProperties() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("product", WayangJsonSchemaDocuments.stringProperty());
        properties.put("valid", WayangJsonSchemaDocuments.booleanProperty());
        properties.put("activeSourceId", WayangJsonSchemaDocuments.stringProperty());
        properties.put("activeSourceType", WayangJsonSchemaDocuments.stringProperty());
        properties.put("activeSourceLocation", WayangJsonSchemaDocuments.stringProperty());
        properties.put("fallbackUsed", WayangJsonSchemaDocuments.booleanProperty());
        properties.put("sourceCount", WayangJsonSchemaDocuments.nonNegativeIntegerProperty());
        properties.put("sources", WayangJsonSchemaDocuments.arrayProperty(readinessProfileSourceStatusProperty()));
        properties.put("totalProfiles", WayangJsonSchemaDocuments.nonNegativeIntegerProperty());
        properties.put("profileIds", WayangJsonSchemaDocuments.stringArrayProperty());
        properties.put("profiles", WayangJsonSchemaDocuments.arrayProperty(readinessProfileProperty()));
        properties.put("validation", readinessProfileValidationProperty());
        return properties;
    }

    static Map<String, Object> readinessProfileRegistryConfigDiagnosticsProperties() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("product", WayangJsonSchemaDocuments.stringProperty());
        properties.put("valid", WayangJsonSchemaDocuments.booleanProperty());
        properties.put("issueCount", WayangJsonSchemaDocuments.nonNegativeIntegerProperty());
        properties.put("config", readinessProfileRegistryConfigProperty());
        properties.put("issues", WayangJsonSchemaDocuments.arrayProperty(
                readinessProfileRegistryConfigIssueProperty()));
        return properties;
    }

    static Map<String, Object> catalogProperty() {
        return WayangJsonSchemaDocuments.objectProperty(
                List.of("product", "coreEngine", "surfaces", "profiles"),
                true,
                catalogProperties());
    }

    private static Map<String, Object> componentProperty() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("name", WayangJsonSchemaDocuments.stringProperty());
        properties.put("role", WayangJsonSchemaDocuments.stringProperty());
        properties.put("state", WayangJsonSchemaDocuments.stringProperty());
        properties.put("endpoint", WayangJsonSchemaDocuments.stringProperty());
        properties.put("healthPercent", boundedIntegerProperty(0, 100));
        return WayangJsonSchemaDocuments.objectProperty(
                List.of("name", "role", "state", "endpoint", "healthPercent"),
                true,
                properties);
    }

    private static Map<String, Object> surfaceProperty() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("id", WayangJsonSchemaDocuments.stringProperty());
        properties.put("name", WayangJsonSchemaDocuments.stringProperty());
        properties.put("role", WayangJsonSchemaDocuments.stringProperty());
        properties.put("engineCapabilities", WayangJsonSchemaDocuments.stringArrayProperty());
        properties.put("adapterBoundaries", WayangJsonSchemaDocuments.stringArrayProperty());
        properties.put("policy", policyProperty());
        properties.put("profiles", WayangJsonSchemaDocuments.stringArrayProperty());
        return WayangJsonSchemaDocuments.objectProperty(
                List.of("id", "name", "role", "engineCapabilities", "adapterBoundaries", "profiles"),
                true,
                properties);
    }

    private static Map<String, Object> policyProperty() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("surfaceId", WayangJsonSchemaDocuments.stringProperty());
        properties.put("memoryPreferred", WayangJsonSchemaDocuments.booleanProperty());
        properties.put("workspacePreferred", WayangJsonSchemaDocuments.booleanProperty());
        properties.put("harnessPreferred", WayangJsonSchemaDocuments.booleanProperty());
        properties.put("workflowPreferred", WayangJsonSchemaDocuments.booleanProperty());
        properties.put("suggestedSkills", WayangJsonSchemaDocuments.stringArrayProperty());
        properties.put("requiredContextKeys", WayangJsonSchemaDocuments.stringArrayProperty());
        properties.put("routingHints", WayangJsonSchemaDocuments.stringArrayProperty());
        return WayangJsonSchemaDocuments.objectProperty(
                List.of(
                        "surfaceId",
                        "memoryPreferred",
                        "workspacePreferred",
                        "harnessPreferred",
                        "workflowPreferred",
                        "suggestedSkills",
                        "requiredContextKeys",
                        "routingHints"),
                true,
                properties);
    }

    private static Map<String, Object> profileProperty() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("id", WayangJsonSchemaDocuments.stringProperty());
        properties.put("name", WayangJsonSchemaDocuments.stringProperty());
        properties.put("surfaceId", WayangJsonSchemaDocuments.stringProperty());
        properties.put("description", WayangJsonSchemaDocuments.stringProperty());
        properties.put("starterPrompt", WayangJsonSchemaDocuments.stringProperty());
        properties.put("workflowId", WayangJsonSchemaDocuments.stringProperty());
        properties.put("skills", WayangJsonSchemaDocuments.stringArrayProperty());
        properties.put("memoryEnabled", WayangJsonSchemaDocuments.booleanProperty());
        properties.put("workspaceEnabled", WayangJsonSchemaDocuments.booleanProperty());
        properties.put("harnessEnabled", WayangJsonSchemaDocuments.booleanProperty());
        properties.put("harnessIncludeOptional", WayangJsonSchemaDocuments.booleanProperty());
        properties.put("requireReady", WayangJsonSchemaDocuments.booleanProperty());
        properties.put("maxSteps", WayangJsonSchemaDocuments.positiveIntegerProperty());
        properties.put("workspaceMaxEntries", WayangJsonSchemaDocuments.positiveIntegerProperty());
        properties.put("harnessMaxChecks", WayangJsonSchemaDocuments.positiveIntegerProperty());
        properties.put("context", WayangJsonSchemaDocuments.openObjectProperty());
        properties.put("notes", WayangJsonSchemaDocuments.stringArrayProperty());
        return WayangJsonSchemaDocuments.objectProperty(
                List.of(
                        "id",
                        "name",
                        "surfaceId",
                        "description",
                        "starterPrompt",
                        "workflowId",
                        "skills",
                        "memoryEnabled",
                        "workspaceEnabled",
                        "harnessEnabled",
                        "harnessIncludeOptional",
                        "requireReady",
                        "maxSteps",
                        "workspaceMaxEntries",
                        "harnessMaxChecks",
                        "context",
                        "notes"),
                true,
                properties);
    }

    private static Map<String, Object> sdkBoundaryProperty() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("id", WayangJsonSchemaDocuments.stringProperty());
        properties.put("name", WayangJsonSchemaDocuments.stringProperty());
        properties.put("intendedPackage", WayangJsonSchemaDocuments.stringProperty());
        properties.put("responsibility", WayangJsonSchemaDocuments.stringProperty());
        properties.put("classPrefixes", WayangJsonSchemaDocuments.stringArrayProperty());
        properties.put("contractSchemas", WayangJsonSchemaDocuments.stringArrayProperty());
        properties.put("dependsOn", WayangJsonSchemaDocuments.stringArrayProperty());
        return WayangJsonSchemaDocuments.objectProperty(
                List.of(
                        "id",
                        "name",
                        "intendedPackage",
                        "responsibility",
                        "classPrefixes",
                        "contractSchemas",
                        "dependsOn"),
                true,
                properties);
    }

    private static Map<String, Object> readinessProfileProperty() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("profileId", WayangJsonSchemaDocuments.stringProperty());
        properties.put("description", WayangJsonSchemaDocuments.stringProperty());
        properties.put("defaultProfile", WayangJsonSchemaDocuments.booleanProperty());
        properties.put("productionProfile", WayangJsonSchemaDocuments.booleanProperty());
        properties.put("componentCount", WayangJsonSchemaDocuments.nonNegativeIntegerProperty());
        properties.put("readinessIds", WayangJsonSchemaDocuments.stringArrayProperty());
        return WayangJsonSchemaDocuments.objectProperty(
                List.of(
                        "profileId",
                        "description",
                        "defaultProfile",
                        "productionProfile",
                        "componentCount",
                        "readinessIds"),
                true,
                properties);
    }

    private static Map<String, Object> readinessProfileValidationIssueProperty() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("kind", WayangJsonSchemaDocuments.stringProperty());
        properties.put("message", WayangJsonSchemaDocuments.stringProperty());
        properties.put("profileId", WayangJsonSchemaDocuments.stringProperty());
        properties.put("readinessId", WayangJsonSchemaDocuments.stringProperty());
        return WayangJsonSchemaDocuments.objectProperty(
                List.of("kind", "message", "profileId", "readinessId"),
                true,
                properties);
    }

    private static Map<String, Object> readinessProfileValidationPolicyProperty() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("policyId", WayangJsonSchemaDocuments.stringProperty());
        properties.put("strict", WayangJsonSchemaDocuments.booleanProperty());
        properties.put("knownReadinessCount", WayangJsonSchemaDocuments.nonNegativeIntegerProperty());
        properties.put("requireDefaultProfile", WayangJsonSchemaDocuments.booleanProperty());
        properties.put("requireProductionProfile", WayangJsonSchemaDocuments.booleanProperty());
        properties.put("requireFullReadinessCoverage", WayangJsonSchemaDocuments.booleanProperty());
        return WayangJsonSchemaDocuments.objectProperty(
                List.of(
                        "policyId",
                        "strict",
                        "knownReadinessCount",
                        "requireDefaultProfile",
                        "requireProductionProfile",
                        "requireFullReadinessCoverage"),
                true,
                properties);
    }

    private static Map<String, Object> readinessProfileValidationPolicyDescriptorProperty() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("policyId", WayangJsonSchemaDocuments.stringProperty());
        properties.put("description", WayangJsonSchemaDocuments.stringProperty());
        properties.put("defaultPolicy", WayangJsonSchemaDocuments.booleanProperty());
        properties.put("strict", WayangJsonSchemaDocuments.booleanProperty());
        properties.put("knownReadinessCount", WayangJsonSchemaDocuments.nonNegativeIntegerProperty());
        properties.put("requireDefaultProfile", WayangJsonSchemaDocuments.booleanProperty());
        properties.put("requireProductionProfile", WayangJsonSchemaDocuments.booleanProperty());
        properties.put("requireFullReadinessCoverage", WayangJsonSchemaDocuments.booleanProperty());
        return WayangJsonSchemaDocuments.objectProperty(
                List.of(
                        "policyId",
                        "description",
                        "defaultPolicy",
                        "strict",
                        "knownReadinessCount",
                        "requireDefaultProfile",
                        "requireProductionProfile",
                        "requireFullReadinessCoverage"),
                true,
                properties);
    }

    private static Map<String, Object> readinessProfileValidationProperty() {
        return WayangJsonSchemaDocuments.objectProperty(
                List.of(
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
                        "issues"),
                true,
                readinessProfileValidationProperties());
    }

    private static Map<String, Object> readinessProfileSourceStatusProperty() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("sourceId", WayangJsonSchemaDocuments.stringProperty());
        properties.put("sourceType", WayangJsonSchemaDocuments.stringProperty());
        properties.put("location", WayangJsonSchemaDocuments.stringProperty());
        properties.put("selected", WayangJsonSchemaDocuments.booleanProperty());
        properties.put("fallback", WayangJsonSchemaDocuments.booleanProperty());
        properties.put("available", WayangJsonSchemaDocuments.booleanProperty());
        properties.put("valid", WayangJsonSchemaDocuments.booleanProperty());
        properties.put("profileCount", WayangJsonSchemaDocuments.nonNegativeIntegerProperty());
        properties.put("issueCount", WayangJsonSchemaDocuments.nonNegativeIntegerProperty());
        properties.put("message", WayangJsonSchemaDocuments.stringProperty());
        return WayangJsonSchemaDocuments.objectProperty(
                List.of(
                        "sourceId",
                        "sourceType",
                        "location",
                        "selected",
                        "fallback",
                        "available",
                        "valid",
                        "profileCount",
                        "issueCount",
                        "message"),
                true,
                properties);
    }

    private static Map<String, Object> readinessProfileRegistryConfigProperty() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("mode", WayangJsonSchemaDocuments.stringProperty());
        properties.put("filePath", WayangJsonSchemaDocuments.stringProperty());
        properties.put("databaseUrl", WayangJsonSchemaDocuments.stringProperty());
        properties.put("objectStorage", objectStorageConfigProperty());
        properties.put("fallbackToBuiltIn", WayangJsonSchemaDocuments.booleanProperty());
        properties.put("validationPolicyId", WayangJsonSchemaDocuments.stringProperty());
        return WayangJsonSchemaDocuments.objectProperty(
                List.of("mode", "fallbackToBuiltIn", "validationPolicyId"),
                true,
                properties);
    }

    private static Map<String, Object> objectStorageConfigProperty() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("provider", WayangJsonSchemaDocuments.stringProperty());
        properties.put("endpoint", WayangJsonSchemaDocuments.stringProperty());
        properties.put("bucket", WayangJsonSchemaDocuments.stringProperty());
        properties.put("region", WayangJsonSchemaDocuments.stringProperty());
        properties.put("keyPrefix", WayangJsonSchemaDocuments.stringProperty());
        properties.put("pathStyleAccess", WayangJsonSchemaDocuments.booleanProperty());
        properties.put("credentialsRef", WayangJsonSchemaDocuments.stringProperty());
        return WayangJsonSchemaDocuments.objectProperty(
                List.of("provider", "pathStyleAccess"),
                true,
                properties);
    }

    private static Map<String, Object> readinessProfileRegistryConfigIssueProperty() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("code", WayangJsonSchemaDocuments.stringProperty());
        properties.put("field", WayangJsonSchemaDocuments.stringProperty());
        properties.put("message", WayangJsonSchemaDocuments.stringProperty());
        return WayangJsonSchemaDocuments.objectProperty(
                List.of("code", "field", "message"),
                true,
                properties);
    }

    private static Map<String, Object> boundedIntegerProperty(int minimum, int maximum) {
        Map<String, Object> values = WayangJsonSchemaDocuments.nonNegativeIntegerProperty();
        values.put("minimum", minimum);
        values.put("maximum", maximum);
        return values;
    }
}
