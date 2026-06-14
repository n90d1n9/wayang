package tech.kayys.wayang.gollek.sdk;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class AgentRunReadinessContext {

    private AgentRunReadinessContext() {
    }

    public static Map<String, Object> from(AgentRunReadiness readiness) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("surfaceId", readiness.surfaceId());
        context.put("ready", readiness.ready());
        context.put("surfacePolicyAssessment", SurfacePolicyAssessmentContext.from(readiness.surfacePolicyAssessment()));
        context.put("skillAssessment", AgentRunSkillAssessmentContext.from(readiness.skillAssessment()));
        return Collections.unmodifiableMap(context);
    }
}
