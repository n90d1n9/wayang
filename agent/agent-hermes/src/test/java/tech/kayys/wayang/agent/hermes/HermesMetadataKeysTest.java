package tech.kayys.wayang.agent.hermes;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;

import static org.assertj.core.api.Assertions.assertThat;

class HermesMetadataKeysTest {

    @Test
    void exposesStableContractKeys() {
        assertThat(HermesMetadataKeys.CONTEXT_METADATA_CONTRACT).isEqualTo("hermesMetadataContract");
        assertThat(HermesMetadataKeys.PARAM_METADATA_CONTRACT).isEqualTo("metadataContract");
        assertThat(HermesMetadataKeys.METADATA_CONTRACT).isEqualTo("metadataContract");
        assertThat(HermesMetadataKeys.CONTEXT_EXECUTION_DIRECTIVE).isEqualTo("hermesExecutionDirective");
        assertThat(HermesMetadataKeys.PARAM_EXECUTION_DIRECTIVE).isEqualTo("executionDirective");
        assertThat(HermesMetadataKeys.CONTEXT_GATEWAY_DIRECTIVE).isEqualTo("hermesGatewayDirective");
        assertThat(HermesMetadataKeys.PARAM_GATEWAY_DIRECTIVE).isEqualTo("gatewayDirective");
        assertThat(HermesMetadataKeys.CONTEXT_AUTOMATION_DIRECTIVE).isEqualTo("hermesAutomationDirective");
        assertThat(HermesMetadataKeys.PARAM_AUTOMATION_DIRECTIVE).isEqualTo("automationDirective");
        assertThat(HermesMetadataKeys.CONTEXT_DELEGATION_DIRECTIVE).isEqualTo("hermesDelegationDirective");
        assertThat(HermesMetadataKeys.PARAM_DELEGATION_DIRECTIVE).isEqualTo("delegationDirective");
        assertThat(HermesMetadataKeys.CONTEXT_PROVIDER_ROUTING_DIRECTIVE)
                .isEqualTo("hermesProviderRoutingDirective");
        assertThat(HermesMetadataKeys.PARAM_PROVIDER_ROUTING_DIRECTIVE)
                .isEqualTo("providerRoutingDirective");
        assertThat(HermesMetadataKeys.CONTEXT_MEMORY_REFLECTION_DIRECTIVE)
                .isEqualTo("hermesMemoryReflectionDirective");
        assertThat(HermesMetadataKeys.PARAM_MEMORY_REFLECTION_DIRECTIVE)
                .isEqualTo("memoryReflectionDirective");
        assertThat(HermesMetadataKeys.CONTEXT_TRAJECTORY_EXPORT_DIRECTIVE)
                .isEqualTo("hermesTrajectoryExportDirective");
        assertThat(HermesMetadataKeys.PARAM_TRAJECTORY_EXPORT_DIRECTIVE)
                .isEqualTo("trajectoryExportDirective");
        assertThat(HermesMetadataKeys.CONTEXT_SKILL_LINEAGE_PLAN)
                .isEqualTo("hermesSkillLineagePlan");
        assertThat(HermesMetadataKeys.PARAM_SKILL_LINEAGE_PLAN)
                .isEqualTo("skillLineagePlan");
        assertThat(HermesMetadataKeys.CONTEXT_SKILL_LINEAGE_DIRECTIVE)
                .isEqualTo("hermesSkillLineageDirective");
        assertThat(HermesMetadataKeys.PARAM_SKILL_LINEAGE_DIRECTIVE)
                .isEqualTo("skillLineageDirective");
        assertThat(HermesMetadataKeys.CONTEXT_RUNTIME_DIAGNOSTICS)
                .isEqualTo("hermesRuntimeDiagnostics");
        assertThat(HermesMetadataKeys.PARAM_RUNTIME_DIAGNOSTICS)
                .isEqualTo("runtimeDiagnostics");
        assertThat(HermesMetadataKeys.METADATA_RUNTIME_DIAGNOSTICS)
                .isEqualTo("runtimeDiagnostics");
        assertThat(HermesMetadataKeys.METADATA_LEARNING_AUDIT_RETENTION_OBSERVATION)
                .isEqualTo("learningAuditRetentionObservation");
        assertThat(HermesMetadataKeys.CONTEXT_DIRECTIVE_DISPATCH_REPORT)
                .isEqualTo("hermesDirectiveDispatchReport");
        assertThat(HermesMetadataKeys.PARAM_DIRECTIVE_DISPATCH_REPORT)
                .isEqualTo("directiveDispatchReport");
    }

    @Test
    void exposesStableContextPlanKeys() {
        assertThat(HermesMetadataKeys.CONTEXT_PLAN_KEYS)
                .containsExactly(
                        "hermesExecutionPlan",
                        "hermesGatewayContext",
                        "hermesAutomationIntent",
                        "hermesDelegationPlan",
                        "hermesProviderRoutingPlan",
                        "hermesMemoryReflectionPlan",
                        "hermesTrajectoryExportPlan",
                        "hermesSkillLineagePlan");
    }

    @Test
    void exposesStableParameterPlanKeys() {
        assertThat(HermesMetadataKeys.PARAMETER_PLAN_KEYS)
                .containsExactly(
                        "executionPlan",
                        "gatewayContext",
                        "automationIntent",
                        "delegationPlan",
                        "providerRoutingPlan",
                        "memoryReflectionPlan",
                        "trajectoryExportPlan",
                        "skillLineagePlan");
    }

    @Test
    void planKeyGroupsHaveNoDuplicates() {
        assertThat(new LinkedHashSet<>(HermesMetadataKeys.CONTEXT_PLAN_KEYS))
                .hasSameSizeAs(HermesMetadataKeys.CONTEXT_PLAN_KEYS);
        assertThat(new LinkedHashSet<>(HermesMetadataKeys.PARAMETER_PLAN_KEYS))
                .hasSameSizeAs(HermesMetadataKeys.PARAMETER_PLAN_KEYS);
        assertThat(new LinkedHashSet<>(HermesMetadataKeys.CONTEXT_DIRECTIVE_KEYS))
                .hasSameSizeAs(HermesMetadataKeys.CONTEXT_DIRECTIVE_KEYS);
        assertThat(new LinkedHashSet<>(HermesMetadataKeys.PARAMETER_DIRECTIVE_KEYS))
                .hasSameSizeAs(HermesMetadataKeys.PARAMETER_DIRECTIVE_KEYS);
        assertThat(new LinkedHashSet<>(HermesMetadataKeys.CONTEXT_RUNTIME_KEYS))
                .hasSameSizeAs(HermesMetadataKeys.CONTEXT_RUNTIME_KEYS);
        assertThat(new LinkedHashSet<>(HermesMetadataKeys.PARAMETER_RUNTIME_KEYS))
                .hasSameSizeAs(HermesMetadataKeys.PARAMETER_RUNTIME_KEYS);
        assertThat(new LinkedHashSet<>(HermesMetadataKeys.DIRECTIVE_DISPATCH_REPORT_FIELDS))
                .hasSameSizeAs(HermesMetadataKeys.DIRECTIVE_DISPATCH_REPORT_FIELDS);
        assertThat(new LinkedHashSet<>(HermesMetadataKeys.DIRECTIVE_DISPATCH_SUMMARY_FIELDS))
                .hasSameSizeAs(HermesMetadataKeys.DIRECTIVE_DISPATCH_SUMMARY_FIELDS);
        assertThat(new LinkedHashSet<>(HermesMetadataKeys.DIRECTIVE_DISPATCH_ATTENTION_FIELDS))
                .hasSameSizeAs(HermesMetadataKeys.DIRECTIVE_DISPATCH_ATTENTION_FIELDS);
        assertThat(new LinkedHashSet<>(HermesMetadataKeys.DIRECTIVE_DISPATCH_REMEDIATION_FIELDS))
                .hasSameSizeAs(HermesMetadataKeys.DIRECTIVE_DISPATCH_REMEDIATION_FIELDS);
        assertThat(new LinkedHashSet<>(HermesMetadataKeys.DIRECTIVE_DISPATCH_REMEDIATION_ACTION_FIELDS))
                .hasSameSizeAs(HermesMetadataKeys.DIRECTIVE_DISPATCH_REMEDIATION_ACTION_FIELDS);
        assertThat(new LinkedHashSet<>(HermesMetadataKeys.DIRECTIVE_DISPATCH_RESULT_FIELDS))
                .hasSameSizeAs(HermesMetadataKeys.DIRECTIVE_DISPATCH_RESULT_FIELDS);
        assertThat(new LinkedHashSet<>(HermesMetadataKeys.RUNTIME_PORT_DESCRIPTOR_FIELDS))
                .hasSameSizeAs(HermesMetadataKeys.RUNTIME_PORT_DESCRIPTOR_FIELDS);
        assertThat(new LinkedHashSet<>(HermesMetadataKeys.RUNTIME_EVENT_FIELDS))
                .hasSameSizeAs(HermesMetadataKeys.RUNTIME_EVENT_FIELDS);
    }

    @Test
    void exposesStableDirectiveKeyGroups() {
        assertThat(HermesMetadataKeys.CONTEXT_DIRECTIVE_KEYS)
                .containsExactly(
                        "hermesExecutionDirective",
                        "hermesGatewayDirective",
                        "hermesAutomationDirective",
                        "hermesDelegationDirective",
                        "hermesProviderRoutingDirective",
                        "hermesMemoryReflectionDirective",
                        "hermesTrajectoryExportDirective",
                        "hermesSkillLineageDirective");
        assertThat(HermesMetadataKeys.PARAMETER_DIRECTIVE_KEYS)
                .containsExactly(
                        "executionDirective",
                        "gatewayDirective",
                        "automationDirective",
                        "delegationDirective",
                        "providerRoutingDirective",
                        "memoryReflectionDirective",
                        "trajectoryExportDirective",
                        "skillLineageDirective");
    }

    @Test
    void exposesStableRuntimeKeyGroups() {
        assertThat(HermesMetadataKeys.CONTEXT_RUNTIME_KEYS)
                .containsExactly("hermesRuntimeDiagnostics", "hermesDirectiveDispatchReport");
        assertThat(HermesMetadataKeys.PARAMETER_RUNTIME_KEYS)
                .containsExactly("runtimeDiagnostics", "directiveDispatchReport");
    }

    @Test
    void exposesStableRuntimeReportFieldGroups() {
        assertThat(HermesMetadataKeys.DIRECTIVE_DISPATCH_REPORT_FIELDS)
                .containsExactly(
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
        assertThat(HermesMetadataKeys.DIRECTIVE_DISPATCH_SUMMARY_FIELDS)
                .containsExactly(
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
        assertThat(HermesMetadataKeys.DIRECTIVE_DISPATCH_ATTENTION_FIELDS)
                .containsExactly(
                        "port",
                        "operation",
                        "target",
                        "status",
                        "reason",
                        "severity",
                        "recommendedAction",
                        "retryable",
                        "metadata");
        assertThat(HermesMetadataKeys.DIRECTIVE_DISPATCH_REMEDIATION_FIELDS)
                .containsExactly(
                        "required",
                        "strategy",
                        "actionCount",
                        "criticalCount",
                        "retryableCount",
                        "actions");
        assertThat(HermesMetadataKeys.DIRECTIVE_DISPATCH_REMEDIATION_ACTION_FIELDS)
                .containsExactly(
                        "port",
                        "operation",
                        "target",
                        "status",
                        "action",
                        "severity",
                        "retryable",
                        "reason",
                        "metadata");
        assertThat(HermesMetadataKeys.DIRECTIVE_DISPATCH_RESULT_FIELDS)
                .containsExactly(
                        "port",
                        "operation",
                        "target",
                        "active",
                        "dispatched",
                        "successful",
                        "status",
                        "reason",
                        "metadata");
        assertThat(HermesMetadataKeys.RUNTIME_PORT_DESCRIPTOR_FIELDS)
                .containsExactly(
                        "port",
                        "adapterId",
                        "adapterType",
                        "configured",
                        "noop",
                        "ready",
                        "status",
                        "reason",
                        "metadata");
        assertThat(HermesMetadataKeys.RUNTIME_EVENT_FIELDS)
                .containsExactly(
                        "eventId",
                        "type",
                        "requestId",
                        "tenantId",
                        "sessionId",
                        "userId",
                        "outcome",
                        "occurredAt",
                        "metadata");
    }
}
