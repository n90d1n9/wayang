package tech.kayys.wayang.gollek.sdk;

import tech.kayys.wayang.agent.spi.AgentRequest;

import java.util.List;
import java.util.Map;

public record AgentRunPreview(
        String requestId,
        String tenantId,
        String modelId,
        String workflowId,
        String surfaceId,
        String sessionId,
        String userId,
        boolean systemPromptPresent,
        int promptCharacters,
        int systemPromptCharacters,
        boolean memoryEnabled,
        int maxSteps,
        List<String> skills,
        Map<String, Object> context,
        Map<String, Object> parameters,
        SurfacePolicyAssessment surfacePolicyAssessment,
        AgentRunSkillAssessment skillAssessment) {

    public AgentRunPreview {
        requestId = SdkText.trimToDefault(requestId, "preview");
        tenantId = SdkText.trimToDefault(tenantId, "default");
        modelId = SdkText.trimToEmpty(modelId);
        workflowId = SdkText.trimToDefault(workflowId, "agent-direct");
        surfaceId = WayangProductCatalog.normalizeSurfaceId(surfaceId);
        sessionId = SdkText.trimToEmpty(sessionId);
        userId = SdkText.trimToEmpty(userId);
        promptCharacters = Math.max(0, promptCharacters);
        systemPromptCharacters = Math.max(0, systemPromptCharacters);
        maxSteps = maxSteps > 0 ? maxSteps : 12;
        skills = SdkLists.copy(skills);
        context = SdkMaps.copy(context);
        parameters = SdkMaps.copy(parameters);
        surfacePolicyAssessment = surfacePolicyAssessment == null
                ? SurfacePolicyPreflight.assess(AgentRunRequest.builder().surfaceId(surfaceId).build())
                : surfacePolicyAssessment;
        skillAssessment = skillAssessment == null
                ? AgentRunSkillPreflight.assess(
                        WayangSkillCatalog.defaultRegistry(),
                        AgentRunRequest.builder().surfaceId(surfaceId).skills(skills).build())
                : skillAssessment;
    }

    public static AgentRunPreview from(AgentRunRequest request, AgentRequest coreRequest) {
        AgentRunRequest normalized = AgentRunRequest.builder(request).build();
        return from(normalized, coreRequest, SurfacePolicyPreflight.assess(normalized));
    }

    public static AgentRunPreview from(
            AgentRunRequest request,
            AgentRequest coreRequest,
            SurfacePolicyAssessment assessment) {
        return from(
                request,
                coreRequest,
                assessment,
                AgentRunSkillPreflight.assess(WayangSkillCatalog.defaultRegistry(), request));
    }

    public static AgentRunPreview from(
            AgentRunRequest request,
            AgentRequest coreRequest,
            SurfacePolicyAssessment assessment,
            AgentRunSkillAssessment skillAssessment) {
        AgentRequest core = coreRequest == null
                ? new WayangAgentRequestMapper().toAgentRequest(request)
                : coreRequest;
        Map<String, Object> context = core.context();
        String workflowId = String.valueOf(context.getOrDefault("workflowId", "agent-direct"));
        String surfaceId = String.valueOf(context.getOrDefault("surfaceId", request.surfaceId()));
        String systemPrompt = core.systemPrompt() == null ? "" : core.systemPrompt();
        String prompt = core.prompt() == null ? "" : core.prompt();
        return new AgentRunPreview(
                core.requestId(),
                core.tenantId(),
                core.modelId() == null ? "" : core.modelId(),
                workflowId,
                surfaceId,
                core.sessionId(),
                core.userId(),
                !systemPrompt.isBlank(),
                prompt.length(),
                systemPrompt.length(),
                core.memoryConfig().conversationEnabled(),
                core.getMaxSteps(),
                core.allowedSkills(),
                context,
                core.parameters(),
                assessment,
                skillAssessment);
    }

    public boolean ready() {
        return surfacePolicyAssessment.ready() && skillAssessment.ready();
    }

    public boolean workspaceAttached() {
        return context.containsKey("workspace");
    }

    public boolean harnessAttached() {
        return context.containsKey("harness");
    }

    public List<String> contextKeys() {
        return List.copyOf(context.keySet());
    }
}
