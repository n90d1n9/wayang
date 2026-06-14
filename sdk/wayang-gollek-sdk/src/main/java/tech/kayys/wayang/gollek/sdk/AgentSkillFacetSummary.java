package tech.kayys.wayang.gollek.sdk;

import java.util.List;

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
