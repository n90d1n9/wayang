package tech.kayys.wayang.gollek.sdk;

import java.util.List;

final class WayangContractJsonSchemas {

    private WayangContractJsonSchemas() {
    }

    static WayangContractJsonSchema schema(WayangContractDescriptor contract) {
        WayangContractDescriptor normalized = contract == null ? WayangContractDescriptors.empty() : contract;
        if (AgentRunLifecycleJsonSchema.matches(normalized)) {
            return AgentRunLifecycleJsonSchema.schema(normalized);
        }
        if (AgentRunPlanningJsonSchema.matches(normalized)) {
            return AgentRunPlanningJsonSchema.schema(normalized);
        }
        if (WayangReadinessJsonSchema.matches(normalized)) {
            return WayangReadinessJsonSchema.schema(normalized);
        }
        if (WayangContractCoverageJsonSchema.matches(normalized)) {
            return WayangContractCoverageJsonSchema.schema(normalized);
        }
        if (WayangCommandDiscoveryJsonSchema.matches(normalized)) {
            return WayangCommandDiscoveryJsonSchema.schema(normalized);
        }
        if (WayangWorkbenchJsonSchema.matches(normalized)) {
            return WayangWorkbenchJsonSchema.schema(normalized);
        }
        if (WayangPlatformJsonSchema.matches(normalized)) {
            return WayangPlatformJsonSchema.schema(normalized);
        }
        if (WayangStandardAlignmentJsonSchema.matches(normalized)) {
            return WayangStandardAlignmentJsonSchema.schema(normalized);
        }
        if (WayangStandardCatalogJsonSchema.matches(normalized)) {
            return WayangStandardCatalogJsonSchema.schema(normalized);
        }
        if (WayangSkillJsonSchema.matches(normalized)) {
            return WayangSkillJsonSchema.schema(normalized);
        }
        if (WayangProviderCapabilityJsonSchema.matches(normalized)) {
            return WayangProviderCapabilityJsonSchema.schema(normalized);
        }
        return WayangContractEnvelopeJsonSchema.schema(normalized);
    }

    static WayangContractJsonSchemaBundle bundle(WayangContractDiscovery discovery) {
        WayangContractDiscovery normalized = discovery == null
                ? WayangContractDiscovery.of(WayangContractQuery.all(), List.of(), 0)
                : discovery;
        return new WayangContractJsonSchemaBundle(
                normalized,
                normalized.contracts().stream()
                        .map(WayangContractJsonSchemas::schema)
                        .toList());
    }
}
