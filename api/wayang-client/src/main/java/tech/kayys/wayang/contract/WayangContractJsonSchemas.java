package tech.kayys.wayang.contract;

import java.util.List;

import tech.kayys.wayang.agent.lifecycle.AgentRunLifecycleJsonSchema;
import tech.kayys.wayang.agent.planner.AgentRunPlanningJsonSchema;
import tech.kayys.wayang.alignment.WayangStandardAlignmentJsonSchema;
import tech.kayys.wayang.capability.WayangProviderCapabilityJsonSchema;
import tech.kayys.wayang.catalog.WayangStandardCatalogJsonSchema;
import tech.kayys.wayang.client.WayangPlatformJsonSchema;
import tech.kayys.wayang.command.WayangCommandDiscoveryJsonSchema;
import tech.kayys.wayang.readiness.WayangReadinessJsonSchema;
import tech.kayys.wayang.skill.WayangSkillJsonSchema;
import tech.kayys.wayang.workbench.WayangWorkbenchJsonSchema;

public final class WayangContractJsonSchemas {

    private WayangContractJsonSchemas() {
    }

    public static WayangContractJsonSchema schema(WayangContractDescriptor contract) {
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

    public static WayangContractJsonSchemaBundle bundle(WayangContractDiscovery discovery) {
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
