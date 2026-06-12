package tech.kayys.wayang.gollek.sdk;

import tech.kayys.wayang.agent.spi.AgentRequest;

public record AgentRunPreparation(
        AgentRunRequest request,
        WorkspaceSnapshot workspace,
        HarnessPlan harness,
        AgentRequest coreRequest,
        AgentRunReadiness readiness) {

    public AgentRunPreparation {
        request = AgentRunRequest.builder(request).build();
        coreRequest = coreRequest == null
                ? new WayangAgentRequestMapper().toAgentRequest(request, workspace, harness)
                : coreRequest;
        readiness = readiness == null
                ? AgentRunReadiness.assess(WayangSkillCatalog.defaultRegistry(), request)
                : readiness;
    }

    public AgentRunPreview preview() {
        return AgentRunPreview.from(
                request,
                coreRequest,
                readiness.surfacePolicyAssessment(),
                readiness.skillAssessment());
    }

    public boolean ready() {
        return readiness.ready();
    }
}
