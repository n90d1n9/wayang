package tech.kayys.wayang.gollek.sdk;

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
