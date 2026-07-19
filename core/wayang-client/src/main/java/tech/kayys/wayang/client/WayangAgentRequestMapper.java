package tech.kayys.wayang.client;

import tech.kayys.wayang.agent.run.AgentRunRequest;
import tech.kayys.wayang.agent.spi.AgentMemoryConfig;
import tech.kayys.wayang.agent.spi.AgentRequest;
import tech.kayys.wayang.harness.HarnessContext;
import tech.kayys.wayang.harness.HarnessPlan;
import tech.kayys.wayang.policy.SurfacePolicyAssessment;
import tech.kayys.wayang.policy.SurfacePolicyAssessmentContext;
import tech.kayys.wayang.policy.SurfacePolicyContext;
import tech.kayys.wayang.policy.SurfacePolicyPreflight;

public final class WayangAgentRequestMapper {

    public AgentRequest toAgentRequest(AgentRunRequest request) {
        return toAgentRequest(request, null);
    }

    public AgentRequest toAgentRequest(AgentRunRequest request, WorkspaceSnapshot workspace) {
        return toAgentRequest(request, workspace, null);
    }

    public AgentRequest toAgentRequest(AgentRunRequest request, WorkspaceSnapshot workspace, HarnessPlan harness) {
        AgentRunRequest normalized = AgentRunRequest.builder(request).build();
        ProductSurfacePolicy surfacePolicy = WayangProductCatalog.policyFor(normalized.surfaceId());
        SurfacePolicyAssessment surfaceAssessment = SurfacePolicyPreflight.assess(normalized);
        AgentRequest.Builder builder = AgentRequest.builder()
                .prompt(normalized.prompt())
                .tenantId(normalized.tenantId())
                .maxSteps(normalized.maxSteps())
                .context(normalized.context())
                .memoryConfig(memoryConfig(normalized));

        if (!normalized.sessionId().isBlank()) {
            builder.sessionId(normalized.sessionId());
        }
        if (!normalized.userId().isBlank()) {
            builder.userId(normalized.userId());
        }
        if (!normalized.systemPrompt().isBlank()) {
            builder.systemPrompt(normalized.systemPrompt());
        }
        if (!normalized.modelId().isBlank()) {
            builder.modelId(normalized.modelId());
        }
        if (!normalized.workflowId().isBlank()) {
            builder.context("workflowId", normalized.workflowId());
        }
        builder.context("surfaceId", surfacePolicy.surfaceId());
        builder.context("surfacePolicy", SurfacePolicyContext.from(surfacePolicy));
        builder.context("surfacePolicyAssessment", SurfacePolicyAssessmentContext.from(surfaceAssessment));
        if (workspace != null) {
            builder.context("workspace", WorkspaceContext.from(workspace));
        }
        if (harness != null) {
            builder.context("harness", HarnessContext.from(harness));
        }
        normalized.skills().forEach(builder::skill);
        return builder.build();
    }

    private AgentMemoryConfig memoryConfig(AgentRunRequest request) {
        return new AgentMemoryConfig(
                request.memoryEnabled(),
                10,
                request.memoryEnabled(),
                request.tenantId());
    }
}
