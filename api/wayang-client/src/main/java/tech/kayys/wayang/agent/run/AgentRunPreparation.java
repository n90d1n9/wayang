package tech.kayys.wayang.agent.run;

import tech.kayys.wayang.agent.spi.AgentRequest;
import tech.kayys.wayang.client.WayangAgentRequestMapper;
import tech.kayys.wayang.client.WorkspaceSnapshot;
import tech.kayys.wayang.harness.HarnessPlan;
import tech.kayys.wayang.skill.WayangSkillCatalog;

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
