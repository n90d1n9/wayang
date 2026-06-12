package tech.kayys.wayang.gollek.sdk;

import java.util.List;

public record AgentRunSkillAssessment(
        String surfaceId,
        boolean ready,
        List<String> requestedSkills,
        List<String> resolvedSkillIds,
        List<String> unknownSkills,
        List<String> unavailableSkillIds,
        List<String> incompatibleSkillIds,
        List<String> recommendations) {

    public AgentRunSkillAssessment {
        surfaceId = WayangProductCatalog.normalizeSurfaceId(surfaceId);
        requestedSkills = SdkLists.copy(requestedSkills);
        resolvedSkillIds = SdkLists.copy(resolvedSkillIds);
        unknownSkills = SdkLists.copy(unknownSkills);
        unavailableSkillIds = SdkLists.copy(unavailableSkillIds);
        incompatibleSkillIds = SdkLists.copy(incompatibleSkillIds);
        recommendations = SdkLists.copy(recommendations);
    }
}
