package tech.kayys.wayang.agent.hermes;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Aggregate advisory plan for a Hermes request.
 */
public record HermesRequestPlan(
        HermesExecutionPlan executionPlan,
        HermesExecutionDirective executionDirective,
        HermesGatewayContext gatewayContext,
        HermesGatewayDirective gatewayDirective,
        HermesAutomationIntent automationIntent,
        HermesAutomationDirective automationDirective,
        HermesDelegationPlan delegationPlan,
        HermesDelegationDirective delegationDirective,
        HermesProviderRoutingPlan providerRoutingPlan,
        HermesProviderRoutingDirective providerRoutingDirective,
        HermesMemoryReflectionPlan memoryReflectionPlan,
        HermesMemoryReflectionDirective memoryReflectionDirective,
        HermesTrajectoryExportPlan trajectoryExportPlan,
        HermesTrajectoryExportDirective trajectoryExportDirective,
        HermesSkillLineagePlan skillLineagePlan,
        HermesSkillLineageDirective skillLineageDirective) {

    public HermesRequestPlan {
        executionPlan = Objects.requireNonNull(executionPlan, "executionPlan");
        executionDirective = Objects.requireNonNull(executionDirective, "executionDirective");
        gatewayContext = Objects.requireNonNull(gatewayContext, "gatewayContext");
        gatewayDirective = Objects.requireNonNull(gatewayDirective, "gatewayDirective");
        automationIntent = Objects.requireNonNull(automationIntent, "automationIntent");
        automationDirective = Objects.requireNonNull(automationDirective, "automationDirective");
        delegationPlan = Objects.requireNonNull(delegationPlan, "delegationPlan");
        delegationDirective = Objects.requireNonNull(delegationDirective, "delegationDirective");
        providerRoutingPlan = Objects.requireNonNull(providerRoutingPlan, "providerRoutingPlan");
        providerRoutingDirective = Objects.requireNonNull(providerRoutingDirective, "providerRoutingDirective");
        memoryReflectionPlan = Objects.requireNonNull(memoryReflectionPlan, "memoryReflectionPlan");
        memoryReflectionDirective = Objects.requireNonNull(memoryReflectionDirective, "memoryReflectionDirective");
        trajectoryExportPlan = Objects.requireNonNull(trajectoryExportPlan, "trajectoryExportPlan");
        trajectoryExportDirective = Objects.requireNonNull(trajectoryExportDirective, "trajectoryExportDirective");
        skillLineagePlan = Objects.requireNonNull(skillLineagePlan, "skillLineagePlan");
        skillLineageDirective = Objects.requireNonNull(skillLineageDirective, "skillLineageDirective");
    }

    public Map<String, Object> contextMetadata() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put(HermesMetadataKeys.CONTEXT_EXECUTION_PLAN, executionPlan.toMetadata());
        metadata.put(HermesMetadataKeys.CONTEXT_EXECUTION_DIRECTIVE, executionDirective.toMetadata());
        metadata.put(HermesMetadataKeys.CONTEXT_GATEWAY_CONTEXT, gatewayContext.toMetadata());
        metadata.put(HermesMetadataKeys.CONTEXT_GATEWAY_DIRECTIVE, gatewayDirective.toMetadata());
        metadata.put(HermesMetadataKeys.CONTEXT_AUTOMATION_INTENT, automationIntent.toMetadata());
        metadata.put(HermesMetadataKeys.CONTEXT_AUTOMATION_DIRECTIVE, automationDirective.toMetadata());
        metadata.put(HermesMetadataKeys.CONTEXT_DELEGATION_PLAN, delegationPlan.toMetadata());
        metadata.put(HermesMetadataKeys.CONTEXT_DELEGATION_DIRECTIVE, delegationDirective.toMetadata());
        metadata.put(HermesMetadataKeys.CONTEXT_PROVIDER_ROUTING_PLAN, providerRoutingPlan.toMetadata());
        metadata.put(HermesMetadataKeys.CONTEXT_PROVIDER_ROUTING_DIRECTIVE, providerRoutingDirective.toMetadata());
        metadata.put(HermesMetadataKeys.CONTEXT_MEMORY_REFLECTION_PLAN, memoryReflectionPlan.toMetadata());
        metadata.put(HermesMetadataKeys.CONTEXT_MEMORY_REFLECTION_DIRECTIVE, memoryReflectionDirective.toMetadata());
        metadata.put(HermesMetadataKeys.CONTEXT_TRAJECTORY_EXPORT_PLAN, trajectoryExportPlan.toMetadata());
        metadata.put(HermesMetadataKeys.CONTEXT_TRAJECTORY_EXPORT_DIRECTIVE, trajectoryExportDirective.toMetadata());
        metadata.put(HermesMetadataKeys.CONTEXT_SKILL_LINEAGE_PLAN, skillLineagePlan.toMetadata());
        metadata.put(HermesMetadataKeys.CONTEXT_SKILL_LINEAGE_DIRECTIVE, skillLineageDirective.toMetadata());
        return Map.copyOf(metadata);
    }

    public Map<String, Object> parameterMetadata() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put(HermesMetadataKeys.PARAM_EXECUTION_PLAN, executionPlan.toMetadata());
        metadata.put(HermesMetadataKeys.PARAM_EXECUTION_DIRECTIVE, executionDirective.toMetadata());
        metadata.put(HermesMetadataKeys.PARAM_GATEWAY_CONTEXT, gatewayContext.toMetadata());
        metadata.put(HermesMetadataKeys.PARAM_GATEWAY_DIRECTIVE, gatewayDirective.toMetadata());
        metadata.put(HermesMetadataKeys.PARAM_AUTOMATION_INTENT, automationIntent.toMetadata());
        metadata.put(HermesMetadataKeys.PARAM_AUTOMATION_DIRECTIVE, automationDirective.toMetadata());
        metadata.put(HermesMetadataKeys.PARAM_DELEGATION_PLAN, delegationPlan.toMetadata());
        metadata.put(HermesMetadataKeys.PARAM_DELEGATION_DIRECTIVE, delegationDirective.toMetadata());
        metadata.put(HermesMetadataKeys.PARAM_PROVIDER_ROUTING_PLAN, providerRoutingPlan.toMetadata());
        metadata.put(HermesMetadataKeys.PARAM_PROVIDER_ROUTING_DIRECTIVE, providerRoutingDirective.toMetadata());
        metadata.put(HermesMetadataKeys.PARAM_MEMORY_REFLECTION_PLAN, memoryReflectionPlan.toMetadata());
        metadata.put(HermesMetadataKeys.PARAM_MEMORY_REFLECTION_DIRECTIVE, memoryReflectionDirective.toMetadata());
        metadata.put(HermesMetadataKeys.PARAM_TRAJECTORY_EXPORT_PLAN, trajectoryExportPlan.toMetadata());
        metadata.put(HermesMetadataKeys.PARAM_TRAJECTORY_EXPORT_DIRECTIVE, trajectoryExportDirective.toMetadata());
        metadata.put(HermesMetadataKeys.PARAM_SKILL_LINEAGE_PLAN, skillLineagePlan.toMetadata());
        metadata.put(HermesMetadataKeys.PARAM_SKILL_LINEAGE_DIRECTIVE, skillLineageDirective.toMetadata());
        return Map.copyOf(metadata);
    }
}
