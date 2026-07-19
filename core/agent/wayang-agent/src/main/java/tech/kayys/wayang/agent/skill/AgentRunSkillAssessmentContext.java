package tech.kayys.wayang.agent.skill;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class AgentRunSkillAssessmentContext {

    private AgentRunSkillAssessmentContext() {
    }

    public static Map<String, Object> from(AgentRunSkillAssessment assessment) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("surfaceId", assessment.surfaceId());
        context.put("ready", assessment.ready());
        context.put("requestedSkills", assessment.requestedSkills());
        context.put("resolvedSkillIds", assessment.resolvedSkillIds());
        context.put("unknownSkills", assessment.unknownSkills());
        context.put("unavailableSkillIds", assessment.unavailableSkillIds());
        context.put("incompatibleSkillIds", assessment.incompatibleSkillIds());
        context.put("recommendations", assessment.recommendations());
        return Collections.unmodifiableMap(context);
    }
}
