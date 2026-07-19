package tech.kayys.wayang.policy;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import tech.kayys.wayang.client.ProductSurfacePolicy;

public final class SurfacePolicyContext {

    private SurfacePolicyContext() {
    }

    public static Map<String, Object> from(ProductSurfacePolicy policy) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("surfaceId", policy.surfaceId());
        context.put("memoryPreferred", policy.memoryPreferred());
        context.put("workspacePreferred", policy.workspacePreferred());
        context.put("harnessPreferred", policy.harnessPreferred());
        context.put("workflowPreferred", policy.workflowPreferred());
        context.put("suggestedSkills", policy.suggestedSkills());
        context.put("requiredContextKeys", policy.requiredContextKeys());
        context.put("routingHints", policy.routingHints());
        return Collections.unmodifiableMap(context);
    }
}
