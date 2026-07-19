package tech.kayys.wayang.agent.skill;

import java.util.List;

import tech.kayys.wayang.client.SdkLists;
import tech.kayys.wayang.client.SdkText;

public record AgentSkillFacetSummary(
        String name,
        int count,
        List<String> skillIds) {

    public AgentSkillFacetSummary {
        name = SdkText.trimToEmpty(name);
        count = Math.max(0, count);
        skillIds = SdkLists.copy(skillIds);
    }
}
