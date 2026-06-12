package tech.kayys.wayang.agent.hermes;

import java.util.List;

/**
 * Stable metadata keys emitted by Hermes mode into request context and parameters.
 */
public final class HermesMetadataKeys {

    public static final String CONTEXT_METADATA_CONTRACT = "hermesMetadataContract";
    public static final String CONTEXT_CONFIG = "hermesConfig";
    public static final String CONTEXT_CAPABILITIES = "hermesCapabilities";
    public static final String CONTEXT_EXECUTION_PLAN = "hermesExecutionPlan";
    public static final String CONTEXT_EXECUTION_DIRECTIVE = "hermesExecutionDirective";
    public static final String CONTEXT_GATEWAY_CONTEXT = "hermesGatewayContext";
    public static final String CONTEXT_GATEWAY_DIRECTIVE = "hermesGatewayDirective";
    public static final String CONTEXT_AUTOMATION_INTENT = "hermesAutomationIntent";
    public static final String CONTEXT_AUTOMATION_DIRECTIVE = "hermesAutomationDirective";
    public static final String CONTEXT_DELEGATION_PLAN = "hermesDelegationPlan";
    public static final String CONTEXT_DELEGATION_DIRECTIVE = "hermesDelegationDirective";
    public static final String CONTEXT_PROVIDER_ROUTING_PLAN = "hermesProviderRoutingPlan";
    public static final String CONTEXT_PROVIDER_ROUTING_DIRECTIVE = "hermesProviderRoutingDirective";
    public static final String CONTEXT_MEMORY_REFLECTION_PLAN = "hermesMemoryReflectionPlan";
    public static final String CONTEXT_MEMORY_REFLECTION_DIRECTIVE = "hermesMemoryReflectionDirective";
    public static final String CONTEXT_TRAJECTORY_EXPORT_PLAN = "hermesTrajectoryExportPlan";
    public static final String CONTEXT_TRAJECTORY_EXPORT_DIRECTIVE = "hermesTrajectoryExportDirective";
    public static final String CONTEXT_SKILL_LINEAGE_PLAN = "hermesSkillLineagePlan";
    public static final String CONTEXT_SKILL_LINEAGE_DIRECTIVE = "hermesSkillLineageDirective";
    public static final String CONTEXT_RUNTIME_DIAGNOSTICS = "hermesRuntimeDiagnostics";
    public static final String CONTEXT_DIRECTIVE_DISPATCH_REPORT = "hermesDirectiveDispatchReport";

    public static final String PARAM_TOOLSETS = "toolsets";
    public static final String PARAM_EXECUTION_BACKENDS = "executionBackends";
    public static final String PARAM_EXECUTION_BACKEND = "executionBackend";
    public static final String PARAM_REQUIRE_TOOL_CALLING = "requireToolCalling";
    public static final String PARAM_PREFER_LOCAL_PROVIDERS = "preferLocalProviders";
    public static final String PARAM_METADATA_CONTRACT = "metadataContract";
    public static final String PARAM_EXECUTION_PLAN = "executionPlan";
    public static final String PARAM_EXECUTION_DIRECTIVE = "executionDirective";
    public static final String PARAM_GATEWAY_CONTEXT = "gatewayContext";
    public static final String PARAM_GATEWAY_DIRECTIVE = "gatewayDirective";
    public static final String PARAM_AUTOMATION_INTENT = "automationIntent";
    public static final String PARAM_AUTOMATION_DIRECTIVE = "automationDirective";
    public static final String PARAM_DELEGATION_PLAN = "delegationPlan";
    public static final String PARAM_DELEGATION_DIRECTIVE = "delegationDirective";
    public static final String PARAM_PROVIDER_ROUTING_PLAN = "providerRoutingPlan";
    public static final String PARAM_PROVIDER_ROUTING_DIRECTIVE = "providerRoutingDirective";
    public static final String PARAM_MEMORY_REFLECTION_PLAN = "memoryReflectionPlan";
    public static final String PARAM_MEMORY_REFLECTION_DIRECTIVE = "memoryReflectionDirective";
    public static final String PARAM_TRAJECTORY_EXPORT_PLAN = "trajectoryExportPlan";
    public static final String PARAM_TRAJECTORY_EXPORT_DIRECTIVE = "trajectoryExportDirective";
    public static final String PARAM_SKILL_LINEAGE_PLAN = "skillLineagePlan";
    public static final String PARAM_SKILL_LINEAGE_DIRECTIVE = "skillLineageDirective";
    public static final String PARAM_RUNTIME_DIAGNOSTICS = "runtimeDiagnostics";
    public static final String PARAM_DIRECTIVE_DISPATCH_REPORT = "directiveDispatchReport";

    public static final String METADATA_MODE = "mode";
    public static final String METADATA_FEATURES = "features";
    public static final String METADATA_CONFIG = "config";
    public static final String METADATA_CAPABILITIES = "capabilities";
    public static final String METADATA_CONTRACT = "metadataContract";
    public static final String METADATA_RUNTIME_DIAGNOSTICS = "runtimeDiagnostics";
    public static final String METADATA_LEARNING_AUDIT_RETENTION_OBSERVATION =
            "learningAuditRetentionObservation";
    public static final String METADATA_TEMPERATURE = "temperature";
    public static final String METADATA_TOOL_CHOICE = "tool_choice";
    public static final String METADATA_PREFER_LOCAL = "prefer_local";

    public static final List<String> CONTEXT_PLAN_KEYS = List.of(
            CONTEXT_EXECUTION_PLAN,
            CONTEXT_GATEWAY_CONTEXT,
            CONTEXT_AUTOMATION_INTENT,
            CONTEXT_DELEGATION_PLAN,
            CONTEXT_PROVIDER_ROUTING_PLAN,
            CONTEXT_MEMORY_REFLECTION_PLAN,
            CONTEXT_TRAJECTORY_EXPORT_PLAN,
            CONTEXT_SKILL_LINEAGE_PLAN);

    public static final List<String> PARAMETER_PLAN_KEYS = List.of(
            PARAM_EXECUTION_PLAN,
            PARAM_GATEWAY_CONTEXT,
            PARAM_AUTOMATION_INTENT,
            PARAM_DELEGATION_PLAN,
            PARAM_PROVIDER_ROUTING_PLAN,
            PARAM_MEMORY_REFLECTION_PLAN,
            PARAM_TRAJECTORY_EXPORT_PLAN,
            PARAM_SKILL_LINEAGE_PLAN);

    public static final List<String> CONTEXT_DIRECTIVE_KEYS = List.of(
            CONTEXT_EXECUTION_DIRECTIVE,
            CONTEXT_GATEWAY_DIRECTIVE,
            CONTEXT_AUTOMATION_DIRECTIVE,
            CONTEXT_DELEGATION_DIRECTIVE,
            CONTEXT_PROVIDER_ROUTING_DIRECTIVE,
            CONTEXT_MEMORY_REFLECTION_DIRECTIVE,
            CONTEXT_TRAJECTORY_EXPORT_DIRECTIVE,
            CONTEXT_SKILL_LINEAGE_DIRECTIVE);

    public static final List<String> PARAMETER_DIRECTIVE_KEYS = List.of(
            PARAM_EXECUTION_DIRECTIVE,
            PARAM_GATEWAY_DIRECTIVE,
            PARAM_AUTOMATION_DIRECTIVE,
            PARAM_DELEGATION_DIRECTIVE,
            PARAM_PROVIDER_ROUTING_DIRECTIVE,
            PARAM_MEMORY_REFLECTION_DIRECTIVE,
            PARAM_TRAJECTORY_EXPORT_DIRECTIVE,
            PARAM_SKILL_LINEAGE_DIRECTIVE);

    public static final List<String> CONTEXT_RUNTIME_KEYS = List.of(
            CONTEXT_RUNTIME_DIAGNOSTICS,
            CONTEXT_DIRECTIVE_DISPATCH_REPORT);

    public static final List<String> PARAMETER_RUNTIME_KEYS = List.of(
            PARAM_RUNTIME_DIAGNOSTICS,
            PARAM_DIRECTIVE_DISPATCH_REPORT);

    public static final List<String> DIRECTIVE_DISPATCH_REPORT_FIELDS = List.of(
            "successful",
            "outcome",
            "totalCount",
            "activeCount",
            "dispatchedCount",
            "skippedCount",
            "unavailableCount",
            "failedCount",
            "unsuccessfulCount",
            "attentionCount",
            "attention",
            "remediationPlan",
            "statusCounts",
            "summary",
            "runtimePorts",
            "results");

    public static final List<String> DIRECTIVE_DISPATCH_SUMMARY_FIELDS = List.of(
            "successful",
            "outcome",
            "totalCount",
            "activeCount",
            "dispatchedCount",
            "skippedCount",
            "unavailableCount",
            "failedCount",
            "unsuccessfulCount",
            "attentionCount",
            "attention",
            "remediationPlan",
            "statusCounts");

    public static final List<String> DIRECTIVE_DISPATCH_ATTENTION_FIELDS = List.of(
            "port",
            "operation",
            "target",
            "status",
            "reason",
            "severity",
            "recommendedAction",
            "retryable",
            "metadata");

    public static final List<String> DIRECTIVE_DISPATCH_REMEDIATION_FIELDS = List.of(
            "required",
            "strategy",
            "actionCount",
            "criticalCount",
            "retryableCount",
            "actions");

    public static final List<String> DIRECTIVE_DISPATCH_REMEDIATION_ACTION_FIELDS = List.of(
            "port",
            "operation",
            "target",
            "status",
            "action",
            "severity",
            "retryable",
            "reason",
            "metadata");

    public static final List<String> DIRECTIVE_DISPATCH_RESULT_FIELDS = List.of(
            "port",
            "operation",
            "target",
            "active",
            "dispatched",
            "successful",
            "status",
            "reason",
            "metadata");

    public static final List<String> RUNTIME_PORT_DESCRIPTOR_FIELDS = List.of(
            "port",
            "adapterId",
            "adapterType",
            "configured",
            "noop",
            "ready",
            "status",
            "reason",
            "metadata");

    public static final List<String> RUNTIME_EVENT_FIELDS = List.of(
            "eventId",
            "type",
            "requestId",
            "tenantId",
            "sessionId",
            "userId",
            "outcome",
            "occurredAt",
            "metadata");

    private HermesMetadataKeys() {
    }
}
