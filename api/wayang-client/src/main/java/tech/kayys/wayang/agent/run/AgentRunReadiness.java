package tech.kayys.wayang.agent.run;

import tech.kayys.wayang.agent.skill.AgentRunSkillAssessment;
import tech.kayys.wayang.agent.skill.AgentRunSkillPreflight;
import tech.kayys.wayang.client.WayangProductCatalog;
import tech.kayys.wayang.policy.SurfacePolicyAssessment;
import tech.kayys.wayang.policy.SurfacePolicyPreflight;
import tech.kayys.wayang.skill.SkillRegistry;
import tech.kayys.wayang.skill.WayangSkillCatalog;


public record AgentRunReadiness(
        String surfaceId,
        boolean ready,
        SurfacePolicyAssessment surfacePolicyAssessment,
        AgentRunSkillAssessment skillAssessment) {

    public AgentRunReadiness {
        surfaceId = WayangProductCatalog.normalizeSurfaceId(surfaceId);
        surfacePolicyAssessment = surfacePolicyAssessment == null
                ? SurfacePolicyPreflight.assess(AgentRunRequest.builder().surfaceId(surfaceId).build())
                : surfacePolicyAssessment;
        skillAssessment = skillAssessment == null
                ? AgentRunSkillPreflight.assess(
                        WayangSkillCatalog.defaultRegistry(),
                        AgentRunRequest.builder().surfaceId(surfaceId).build())
                : skillAssessment;
        ready = surfacePolicyAssessment.ready() && skillAssessment.ready();
    }

    public static AgentRunReadiness assess(
            SkillRegistry registry,
            AgentRunRequest request) {
        AgentRunRequest normalized = AgentRunRequest.builder(request).build();
        SurfacePolicyAssessment surface = SurfacePolicyPreflight.assess(normalized);
        AgentRunSkillAssessment skills = AgentRunSkillPreflight.assess(registry, normalized);
        return new AgentRunReadiness(normalized.surfaceId(), surface.ready() && skills.ready(), surface, skills);
    }
}
