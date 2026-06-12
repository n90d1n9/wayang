package tech.kayys.wayang.agent.hermes;

import tech.kayys.wayang.agent.spi.AgentRequest;

/**
 * Owns all per-request Hermes advisory planning boundaries.
 */
public final class HermesRequestPlanner {

    private final HermesAgentModeConfig config;
    private final HermesExecutionPlanner executionPlanner;
    private final HermesGatewayContextResolver gatewayContextResolver;
    private final HermesAutomationIntentResolver automationIntentResolver;
    private final HermesDelegationPlanner delegationPlanner;
    private final HermesProviderRoutingResolver providerRoutingResolver;
    private final HermesMemoryReflectionResolver memoryReflectionResolver;
    private final HermesTrajectoryExportResolver trajectoryExportResolver;
    private final HermesSkillLineageResolver skillLineageResolver;

    public HermesRequestPlanner(HermesAgentModeConfig config) {
        HermesAgentModeConfig effectiveConfig = config == null ? HermesAgentModeConfig.defaults() : config;
        this.config = effectiveConfig;
        this.executionPlanner = new HermesExecutionPlanner(effectiveConfig);
        this.gatewayContextResolver = new HermesGatewayContextResolver(effectiveConfig);
        this.automationIntentResolver = new HermesAutomationIntentResolver(effectiveConfig);
        this.delegationPlanner = new HermesDelegationPlanner(effectiveConfig);
        this.providerRoutingResolver = new HermesProviderRoutingResolver(effectiveConfig);
        this.memoryReflectionResolver = new HermesMemoryReflectionResolver(effectiveConfig);
        this.trajectoryExportResolver = new HermesTrajectoryExportResolver(effectiveConfig);
        this.skillLineageResolver = new HermesSkillLineageResolver(effectiveConfig);
    }

    public HermesRequestPlan plan(AgentRequest request) {
        HermesExecutionPlan executionPlan = executionPlanner.plan(request);
        HermesGatewayContext gatewayContext = gatewayContextResolver.resolve(request);
        HermesAutomationIntent automationIntent = automationIntentResolver.resolve(request);
        HermesDelegationPlan delegationPlan = delegationPlanner.plan(request);
        HermesProviderRoutingPlan providerRoutingPlan = providerRoutingResolver.resolve(request);
        HermesMemoryReflectionPlan memoryReflectionPlan = memoryReflectionResolver.resolve(request);
        HermesTrajectoryExportPlan trajectoryExportPlan = trajectoryExportResolver.resolve(request);
        HermesSkillLineagePlan skillLineagePlan = skillLineageResolver.resolve(request);
        return new HermesRequestPlan(
                executionPlan,
                HermesExecutionDirective.from(executionPlan, request, config),
                gatewayContext,
                HermesGatewayDirective.from(gatewayContext, request, config),
                automationIntent,
                HermesAutomationDirective.from(automationIntent, request),
                delegationPlan,
                HermesDelegationDirective.from(delegationPlan, request),
                providerRoutingPlan,
                HermesProviderRoutingDirective.from(providerRoutingPlan, request),
                memoryReflectionPlan,
                HermesMemoryReflectionDirective.from(memoryReflectionPlan, request),
                trajectoryExportPlan,
                HermesTrajectoryExportDirective.from(trajectoryExportPlan, request),
                skillLineagePlan,
                HermesSkillLineageDirective.from(skillLineagePlan));
    }

    public HermesRequestPlan defaultPlan() {
        return plan(null);
    }
}
