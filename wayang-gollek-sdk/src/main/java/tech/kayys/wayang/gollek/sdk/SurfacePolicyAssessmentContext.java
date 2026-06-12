package tech.kayys.wayang.gollek.sdk;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class SurfacePolicyAssessmentContext {

    private SurfacePolicyAssessmentContext() {
    }

    public static Map<String, Object> from(SurfacePolicyAssessment assessment) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("surfaceId", assessment.surfaceId());
        context.put("ready", assessment.ready());
        context.put("satisfiedContextKeys", assessment.satisfiedContextKeys());
        context.put("missingContextKeys", assessment.missingContextKeys());
        context.put("recommendations", assessment.recommendations());
        context.put("routingHints", assessment.routingHints());
        return Collections.unmodifiableMap(context);
    }
}
