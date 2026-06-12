package tech.kayys.wayang.agent.hermes;

import tech.kayys.wayang.agent.spi.AgentMemoryConfig;
import tech.kayys.wayang.agent.spi.AgentRequest;
import tech.kayys.wayang.agent.spi.OrchestrationStrategy;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Builds the stable Hermes-mode prompt fragment and request metadata.
 */
public final class HermesPromptAssembler {

    private final HermesAgentModeConfig config;
    private final HermesRuntimeCapabilities capabilities;
    private final HermesRequestPlanner requestPlanner;

    public HermesPromptAssembler(HermesAgentModeConfig config) {
        this.config = config == null ? HermesAgentModeConfig.defaults() : config;
        this.capabilities = this.config.runtimeCapabilities();
        this.requestPlanner = new HermesRequestPlanner(this.config);
    }

    public String baseSystemPrompt() {
        StringBuilder builder = new StringBuilder();
        builder.append("You are running in Wayang Hermes Agent mode.\n\n");
        builder.append("Operating contract:\n");
        builder.append("- Keep continuity across sessions using the injected memory snapshot and request metadata.\n");
        builder.append("- Prefer reusable skills before solving repeatable work from scratch.\n");
        builder.append("- When a successful workflow is complex, preserve the procedure as a portable learned skill.\n");
        builder.append("- Treat MCP tools and dynamic skills as capability providers; inspect only what the task needs.\n");
        builder.append("- For large workstreams, delegate isolated subtasks through the orchestration layer when available.\n");
        builder.append("- Keep execution observable and resumable; surface assumptions, blockers, and verification results.\n\n");
        builder.append("Enabled capabilities:\n");
        appendCapability(builder, "persistent-memory", capabilities.supportsPersistentMemory());
        appendCapability(builder, "skill-learning", capabilities.supportsSkillLearning());
        appendCapability(builder, "skill-self-improvement", capabilities.supportsSkillSelfImprovement());
        appendCapability(builder, "mcp", capabilities.supportsMcpTooling());
        appendCapability(builder, "gateway", capabilities.supportsGateway());
        appendCapability(builder, "cron", capabilities.supportsCron());
        appendCapability(builder, "sub-agents", capabilities.supportsSubAgents());
        builder.append("\nDefault toolsets: ").append(String.join(", ", capabilities.defaultToolsets())).append('\n');
        builder.append("Execution backends: ").append(String.join(", ", capabilities.executionBackends())).append('\n');
        return builder.toString().trim();
    }

    public String systemPrompt(HermesMemorySnapshot snapshot) {
        String memoryPrompt = config.persistentMemoryEnabled() && snapshot != null
                ? snapshot.render(config.memoryEntryLimit())
                : "";
        if (memoryPrompt.isBlank()) {
            return baseSystemPrompt();
        }
        return baseSystemPrompt() + "\n\n" + memoryPrompt;
    }

    public AgentRequest enrich(AgentRequest request, HermesMemorySnapshot snapshot) {
        return enrich(request, snapshot, requestPlanner.plan(request), null);
    }

    public AgentRequest enrich(
            AgentRequest request,
            HermesMemorySnapshot snapshot,
            HermesRequestPlan requestPlan,
            HermesDirectiveDispatchReport dispatchReport) {
        HermesRequestPlan effectivePlan = requestPlan == null ? this.requestPlanner.plan(request) : requestPlan;
        Map<String, Object> metadataContract = HermesMetadataContract.current().toMetadata();
        Map<String, Object> context = new LinkedHashMap<>(request.context());
        context.put(HermesAgentMode.CONTEXT_MODE_KEY, HermesAgentMode.MODE_ID);
        context.put(HermesAgentMode.CONTEXT_FEATURES_KEY, capabilities.enabledFeatures());
        context.put(HermesMetadataKeys.CONTEXT_METADATA_CONTRACT, metadataContract);
        context.put(HermesMetadataKeys.CONTEXT_CONFIG, config.toMetadata());
        context.put(HermesMetadataKeys.CONTEXT_CAPABILITIES, capabilities.toMetadata());
        context.putAll(effectivePlan.contextMetadata());
        if (dispatchReport != null) {
            context.put(HermesMetadataKeys.CONTEXT_DIRECTIVE_DISPATCH_REPORT, dispatchReport.toMetadata());
        }

        Map<String, Object> parameters = new LinkedHashMap<>(request.parameters());
        parameters.putIfAbsent(HermesMetadataKeys.PARAM_TOOLSETS, capabilities.defaultToolsets());
        parameters.putIfAbsent(HermesMetadataKeys.PARAM_EXECUTION_BACKENDS, capabilities.executionBackends());
        parameters.putIfAbsent(HermesMetadataKeys.PARAM_EXECUTION_BACKEND, effectivePlan.executionPlan().backend());
        parameters.putAll(effectivePlan.parameterMetadata());
        if (dispatchReport != null) {
            parameters.put(HermesMetadataKeys.PARAM_DIRECTIVE_DISPATCH_REPORT, dispatchReport.toMetadata());
        }
        parameters.putIfAbsent(HermesMetadataKeys.PARAM_METADATA_CONTRACT, metadataContract);
        parameters.putIfAbsent(HermesMetadataKeys.PARAM_REQUIRE_TOOL_CALLING, capabilities.requiresToolCalling());
        parameters.putIfAbsent(HermesMetadataKeys.PARAM_PREFER_LOCAL_PROVIDERS, capabilities.prefersLocalProviders());

        String existingSystemPrompt = request.systemPrompt() == null ? "" : request.systemPrompt().trim();
        String hermesPrompt = systemPrompt(snapshot);
        String mergedSystemPrompt = existingSystemPrompt.isBlank()
                ? hermesPrompt
                : existingSystemPrompt + "\n\n" + hermesPrompt;

        return new AgentRequest(
                request.requestId(),
                request.prompt(),
                mergedSystemPrompt,
                OrchestrationStrategy.HERMES_AGENT,
                request.allowedSkills(),
                context,
                parameters,
                request.tenantId(),
                request.sessionId(),
                request.userId(),
                request.stream(),
                request.verbose(),
                request.timeout(),
                memoryConfig(request),
                request.modelId(),
                request.timestamp());
    }

    private static AgentMemoryConfig memoryConfig(AgentRequest request) {
        return request.memoryConfig() == null ? AgentMemoryConfig.defaults() : request.memoryConfig();
    }

    private static void appendCapability(StringBuilder builder, String name, boolean enabled) {
        builder.append("- ").append(name).append(": ").append(enabled ? "enabled" : "disabled").append('\n');
    }
}
