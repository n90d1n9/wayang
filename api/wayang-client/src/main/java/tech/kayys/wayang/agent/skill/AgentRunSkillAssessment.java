package tech.kayys.wayang.agent.skill;

import java.util.List;

import tech.kayys.wayang.client.SdkLists;
import tech.kayys.wayang.client.WayangProductCatalog;

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
