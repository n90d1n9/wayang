package tech.kayys.wayang.agent.hermes;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.spi.AgentRequest;
import tech.kayys.wayang.agent.spi.OrchestrationStrategy;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HermesPromptAssemblerTest {

    @Test
    void enrichesRequestWithModePromptMemoryAndMetadata() {
        HermesPromptAssembler assembler = new HermesPromptAssembler(HermesAgentModeConfig.defaults());
        AgentRequest request = AgentRequest.builder()
                .prompt("Prepare a release report")
                .systemPrompt("Base rules")
                .tenantId("tenant-a")
                .sessionId("session-a")
                .userId("user-a")
                .context(Map.of("platform", "telegram", "chatId", "chat-a"))
                .parameter(HermesAgentMode.PARAM_LEARN_KEY, true)
                .build();
        HermesMemorySnapshot snapshot = new HermesMemorySnapshot(
                "Project uses Maven modules.",
                "User prefers concise reports.",
                List.of("Last release used smoke tests."),
                Map.of());

        AgentRequest enriched = assembler.enrich(request, snapshot);

        assertThat(enriched.strategy()).isEqualTo(OrchestrationStrategy.HERMES_AGENT);
        assertThat(enriched.systemPrompt())
                .contains("Base rules")
                .contains("Wayang Hermes Agent mode")
                .contains("Project uses Maven modules.")
                .contains("User prefers concise reports.");
        assertThat(enriched.context())
                .containsEntry(HermesAgentMode.CONTEXT_MODE_KEY, HermesAgentMode.MODE_ID)
                .containsKey(HermesAgentMode.CONTEXT_FEATURES_KEY)
                .containsKey(HermesMetadataKeys.CONTEXT_METADATA_CONTRACT)
                .containsKey(HermesMetadataKeys.CONTEXT_CAPABILITIES)
                .containsKeys(HermesMetadataKeys.CONTEXT_PLAN_KEYS.toArray(String[]::new))
                .containsKeys(HermesMetadataKeys.CONTEXT_DIRECTIVE_KEYS.toArray(String[]::new));
        assertThat(enriched.parameters())
                .containsEntry(HermesMetadataKeys.PARAM_REQUIRE_TOOL_CALLING, true)
                .containsEntry(HermesMetadataKeys.PARAM_EXECUTION_BACKEND, "local")
                .containsKeys(HermesMetadataKeys.PARAMETER_PLAN_KEYS.toArray(String[]::new))
                .containsKeys(HermesMetadataKeys.PARAMETER_DIRECTIVE_KEYS.toArray(String[]::new))
                .containsKey(HermesMetadataKeys.PARAM_TOOLSETS)
                .containsKey(HermesMetadataKeys.PARAM_METADATA_CONTRACT);
        @SuppressWarnings("unchecked")
        Map<String, Object> metadataContract =
                (Map<String, Object>) enriched.context().get(HermesMetadataKeys.CONTEXT_METADATA_CONTRACT);
        assertThat(metadataContract)
                .containsEntry("id", HermesMetadataContract.CURRENT_ID)
                .containsEntry("schemaVersion", HermesMetadataContract.CURRENT_SCHEMA_VERSION)
                .containsEntry("mode", HermesAgentMode.MODE_ID)
                .containsEntry("contextPlanKeys", HermesMetadataKeys.CONTEXT_PLAN_KEYS)
                .containsEntry("parameterPlanKeys", HermesMetadataKeys.PARAMETER_PLAN_KEYS)
                .containsEntry("contextDirectiveKeys", HermesMetadataKeys.CONTEXT_DIRECTIVE_KEYS)
                .containsEntry("parameterDirectiveKeys", HermesMetadataKeys.PARAMETER_DIRECTIVE_KEYS)
                .containsEntry("contextRuntimeKeys", HermesMetadataKeys.CONTEXT_RUNTIME_KEYS)
                .containsEntry("parameterRuntimeKeys", HermesMetadataKeys.PARAMETER_RUNTIME_KEYS);
        assertThat(enriched.parameters().get(HermesMetadataKeys.PARAM_METADATA_CONTRACT))
                .isEqualTo(metadataContract);
        @SuppressWarnings("unchecked")
        Map<String, Object> executionPlan =
                (Map<String, Object>) enriched.context().get(HermesMetadataKeys.CONTEXT_EXECUTION_PLAN);
        assertThat(executionPlan)
                .containsEntry("backend", "local")
                .containsEntry("executable", true);
        @SuppressWarnings("unchecked")
        Map<String, Object> executionDirective =
                (Map<String, Object>) enriched.context().get(HermesMetadataKeys.CONTEXT_EXECUTION_DIRECTIVE);
        assertThat(executionDirective)
                .containsEntry("executable", true)
                .containsEntry("backendSupported", true)
                .containsEntry("active", true)
                .containsEntry("operation", "dispatch")
                .containsEntry("backend", "local")
                .containsEntry("adapterType", "local-terminal")
                .containsEntry("tenantId", "tenant-a")
                .containsEntry("sessionId", "session-a")
                .containsEntry("userId", "user-a");
        @SuppressWarnings("unchecked")
        Map<String, Object> gatewayContext =
                (Map<String, Object>) enriched.context().get(HermesMetadataKeys.CONTEXT_GATEWAY_CONTEXT);
        assertThat(gatewayContext)
                .containsEntry("platform", "telegram")
                .containsEntry("channelId", "chat-a")
                .containsEntry("conversationId", "session-a")
                .containsEntry("userId", "user-a")
                .containsEntry("supportedPlatform", true);
        @SuppressWarnings("unchecked")
        Map<String, Object> gatewayDirective =
                (Map<String, Object>) enriched.context().get(HermesMetadataKeys.CONTEXT_GATEWAY_DIRECTIVE);
        assertThat(gatewayDirective)
                .containsEntry("gatewayEnabled", true)
                .containsEntry("supportedPlatform", true)
                .containsEntry("active", true)
                .containsEntry("operation", "deliver")
                .containsEntry("platform", "telegram")
                .containsEntry("destinationType", "conversation")
                .containsEntry("destinationId", "session-a")
                .containsEntry("tenantId", "tenant-a")
                .containsEntry("userId", "user-a");
        @SuppressWarnings("unchecked")
        Map<String, Object> automationIntent =
                (Map<String, Object>) enriched.context().get(HermesMetadataKeys.CONTEXT_AUTOMATION_INTENT);
        assertThat(automationIntent)
                .containsEntry("schedulerEnabled", true)
                .containsEntry("scheduled", false)
                .containsEntry("scheduleType", "none")
                .containsEntry("source", "none");
        @SuppressWarnings("unchecked")
        Map<String, Object> automationDirective =
                (Map<String, Object>) enriched.context().get(HermesMetadataKeys.CONTEXT_AUTOMATION_DIRECTIVE);
        assertThat(automationDirective)
                .containsEntry("schedulerEnabled", true)
                .containsEntry("active", false)
                .containsEntry("operation", "none")
                .containsEntry("scheduleType", "none")
                .containsEntry("tenantId", "tenant-a")
                .containsEntry("sessionId", "session-a")
                .containsEntry("userId", "user-a");
        @SuppressWarnings("unchecked")
        Map<String, Object> delegationPlan =
                (Map<String, Object>) enriched.context().get(HermesMetadataKeys.CONTEXT_DELEGATION_PLAN);
        assertThat(delegationPlan)
                .containsEntry("delegationEnabled", true)
                .containsEntry("delegated", false)
                .containsEntry("maxSubAgents", 4)
                .containsEntry("source", "none");
        @SuppressWarnings("unchecked")
        Map<String, Object> delegationDirective =
                (Map<String, Object>) enriched.context().get(HermesMetadataKeys.CONTEXT_DELEGATION_DIRECTIVE);
        assertThat(delegationDirective)
                .containsEntry("delegationEnabled", true)
                .containsEntry("requested", false)
                .containsEntry("delegated", false)
                .containsEntry("active", false)
                .containsEntry("operation", "none")
                .containsEntry("subAgentCount", 0)
                .containsEntry("maxSubAgents", 4)
                .containsEntry("tenantId", "tenant-a")
                .containsEntry("sessionId", "session-a")
                .containsEntry("userId", "user-a");
        @SuppressWarnings("unchecked")
        Map<String, Object> providerRoutingPlan =
                (Map<String, Object>) enriched.context().get(HermesMetadataKeys.CONTEXT_PROVIDER_ROUTING_PLAN);
        assertThat(providerRoutingPlan)
                .containsEntry("selectedProvider", "auto")
                .containsEntry("fallbackProvider", "auto")
                .containsEntry("toolCallingRequired", true)
                .containsEntry("source", "none");
        @SuppressWarnings("unchecked")
        Map<String, Object> providerRoutingDirective =
                (Map<String, Object>) enriched.context().get(HermesMetadataKeys.CONTEXT_PROVIDER_ROUTING_DIRECTIVE);
        assertThat(providerRoutingDirective)
                .containsEntry("active", true)
                .containsEntry("operation", "route")
                .containsEntry("selectedProvider", "auto")
                .containsEntry("fallbackProvider", "auto")
                .containsEntry("routingMode", "auto")
                .containsEntry("toolCallingRequired", true)
                .containsEntry("tenantId", "tenant-a")
                .containsEntry("sessionId", "session-a")
                .containsEntry("userId", "user-a");
        @SuppressWarnings("unchecked")
        Map<String, Object> memoryReflectionPlan =
                (Map<String, Object>) enriched.context().get(HermesMetadataKeys.CONTEXT_MEMORY_REFLECTION_PLAN);
        assertThat(memoryReflectionPlan)
                .containsEntry("memoryEnabled", true)
                .containsEntry("reflect", false)
                .containsEntry("scope", "session")
                .containsEntry("source", "none");
        @SuppressWarnings("unchecked")
        Map<String, Object> memoryReflectionDirective =
                (Map<String, Object>) enriched.context().get(HermesMetadataKeys.CONTEXT_MEMORY_REFLECTION_DIRECTIVE);
        assertThat(memoryReflectionDirective)
                .containsEntry("active", false)
                .containsEntry("operation", "none")
                .containsEntry("scope", "session")
                .containsEntry("subjectId", "session-a")
                .containsEntry("tenantId", "tenant-a")
                .containsEntry("userId", "user-a");
        @SuppressWarnings("unchecked")
        Map<String, Object> trajectoryExportPlan =
                (Map<String, Object>) enriched.context().get(HermesMetadataKeys.CONTEXT_TRAJECTORY_EXPORT_PLAN);
        assertThat(trajectoryExportPlan)
                .containsEntry("exportEnabled", false)
                .containsEntry("export", false)
                .containsEntry("destination", "none")
                .containsEntry("source", "disabled");
        @SuppressWarnings("unchecked")
        Map<String, Object> trajectoryExportDirective =
                (Map<String, Object>) enriched.context().get(HermesMetadataKeys.CONTEXT_TRAJECTORY_EXPORT_DIRECTIVE);
        assertThat(trajectoryExportDirective)
                .containsEntry("exportEnabled", false)
                .containsEntry("requested", false)
                .containsEntry("active", false)
                .containsEntry("operation", "none")
                .containsEntry("format", "jsonl")
                .containsEntry("destination", "none")
                .containsEntry("tenantId", "tenant-a")
                .containsEntry("sessionId", "session-a")
                .containsEntry("userId", "user-a");
        @SuppressWarnings("unchecked")
        Map<String, Object> skillLineagePlan =
                (Map<String, Object>) enriched.context().get(HermesMetadataKeys.CONTEXT_SKILL_LINEAGE_PLAN);
        assertThat(skillLineagePlan)
                .containsEntry("lineageEnabled", true)
                .containsEntry("requested", false)
                .containsEntry("inspect", false)
                .containsEntry("active", false)
                .containsEntry("operation", "none")
                .containsEntry("source", "none");
        @SuppressWarnings("unchecked")
        Map<String, Object> skillLineageDirective =
                (Map<String, Object>) enriched.context().get(HermesMetadataKeys.CONTEXT_SKILL_LINEAGE_DIRECTIVE);
        assertThat(skillLineageDirective)
                .containsEntry("active", false)
                .containsEntry("operation", "none")
                .containsEntry("skillId", "")
                .containsEntry("target", "");
        assertThat(HermesAgentMode.matches(enriched)).isTrue();
    }

    @Test
    void enrichesRequestWithDirectiveDispatchReportWhenProvided() {
        HermesAgentModeConfig config = HermesAgentModeConfig.defaults();
        HermesPromptAssembler assembler = new HermesPromptAssembler(config);
        AgentRequest request = AgentRequest.builder()
                .requestId("req-dispatch")
                .prompt("Run this task")
                .tenantId("tenant-a")
                .build();
        HermesRequestPlan plan = new HermesRequestPlanner(config).plan(request);
        HermesDirectiveDispatchReport report = new HermesDirectiveDispatcher(HermesRuntimePorts.noop())
                .dispatch(plan);

        AgentRequest enriched = assembler.enrich(request, HermesMemorySnapshot.empty(), plan, report);

        assertThat(enriched.context())
                .containsKey(HermesMetadataKeys.CONTEXT_DIRECTIVE_DISPATCH_REPORT);
        assertThat(enriched.parameters())
                .containsKey(HermesMetadataKeys.PARAM_DIRECTIVE_DISPATCH_REPORT);
        assertThat(enriched.context().get(HermesMetadataKeys.CONTEXT_DIRECTIVE_DISPATCH_REPORT))
                .isEqualTo(enriched.parameters().get(HermesMetadataKeys.PARAM_DIRECTIVE_DISPATCH_REPORT));
        @SuppressWarnings("unchecked")
        Map<String, Object> dispatchMetadata =
                (Map<String, Object>) enriched.context().get(HermesMetadataKeys.CONTEXT_DIRECTIVE_DISPATCH_REPORT);
        assertThat(dispatchMetadata)
                .containsEntry("successful", true)
                .containsEntry("dispatchedCount", 3L)
                .containsEntry("skippedCount", 5L);
    }
}
