package tech.kayys.wayang.gollek.sdk;

import java.util.ArrayList;
import java.util.List;

public final class SurfacePolicyPreflight {

    private SurfacePolicyPreflight() {
    }

    public static SurfacePolicyAssessment assess(AgentRunRequest request) {
        AgentRunRequest normalized = AgentRunRequest.builder(request).build();
        ProductSurfacePolicy policy = WayangProductCatalog.policyFor(normalized.surfaceId());
        List<String> satisfied = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        for (String key : policy.requiredContextKeys()) {
            if (hasContext(normalized, key)) {
                satisfied.add(key);
            } else {
                missing.add(key);
            }
        }

        List<String> recommendations = new ArrayList<>();
        for (String key : missing) {
            addMissingContextRecommendation(recommendations, key);
        }
        if (policy.memoryPreferred() && !normalized.memoryEnabled()) {
            recommendations.add("Enable memory context for this surface.");
        }
        if (policy.workspacePreferred() && !normalized.workspaceEnabled()) {
            addIfAbsent(recommendations, "Attach workspace context with --workspace <path>.");
        }
        if (policy.harnessPreferred() && !normalized.harnessEnabled()) {
            recommendations.add("Attach planned verification checks with --harness.");
        }
        if (policy.workflowPreferred() && normalized.workflowId().isBlank()) {
            addIfAbsent(recommendations, "Set a workflow id with --workflow <id>.");
        }
        if (normalized.skills().isEmpty() && !policy.suggestedSkills().isEmpty()) {
            recommendations.add("Consider surface skills: " + String.join(", ", policy.suggestedSkills()) + ".");
        }

        return new SurfacePolicyAssessment(
                policy.surfaceId(),
                missing.isEmpty(),
                satisfied,
                missing,
                recommendations,
                policy.routingHints());
    }

    private static boolean hasContext(AgentRunRequest request, String key) {
        return switch (key) {
            case "surfaceId" -> !request.surfaceId().isBlank();
            case "tenantId" -> !request.tenantId().isBlank();
            case "workflowId" -> !request.workflowId().isBlank();
            case "workspace" -> request.workspaceEnabled();
            case "harness" -> request.harnessEnabled();
            case "memory" -> request.memoryEnabled();
            default -> request.skills().contains(key);
        };
    }

    private static void addMissingContextRecommendation(List<String> recommendations, String key) {
        switch (key) {
            case "workspace" -> recommendations.add("Attach workspace context with --workspace <path>.");
            case "harness" -> recommendations.add("Attach planned verification checks with --harness.");
            case "workflowId" -> recommendations.add("Set a workflow id with --workflow <id>.");
            case "tenantId" -> recommendations.add("Set a tenant id with --tenant <id>.");
            case "memory" -> recommendations.add("Enable memory context for this surface.");
            default -> recommendations.add("Provide required context key: " + key + ".");
        }
    }

    private static void addIfAbsent(List<String> recommendations, String recommendation) {
        if (!recommendations.contains(recommendation)) {
            recommendations.add(recommendation);
        }
    }
}
