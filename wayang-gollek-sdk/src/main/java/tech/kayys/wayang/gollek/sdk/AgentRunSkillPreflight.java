package tech.kayys.wayang.gollek.sdk;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class AgentRunSkillPreflight {

    private AgentRunSkillPreflight() {
    }

    public static AgentRunSkillAssessment assess(SkillRegistry registry, AgentRunRequest request) {
        SkillRegistry source = registry == null ? SkillRegistry.create() : registry;
        AgentRunRequest normalized = AgentRunRequest.builder(request).build();
        List<String> requested = requestedSkills(normalized.skills());
        List<String> resolved = new ArrayList<>();
        List<String> unknown = new ArrayList<>();
        List<String> unavailable = new ArrayList<>();
        List<String> incompatible = new ArrayList<>();

        for (String skillRef : requested) {
            source.find(skillRef).ifPresentOrElse(skill -> {
                addIfAbsent(resolved, skill.id());
                if (!skill.availableForRuns()) {
                    addIfAbsent(unavailable, skill.id());
                }
                if (!skill.supportsSurface(normalized.surfaceId())) {
                    addIfAbsent(incompatible, skill.id());
                }
            }, () -> unknown.add(skillRef));
        }

        List<String> recommendations = recommendations(normalized.surfaceId(), unknown, unavailable, incompatible);
        boolean ready = unknown.isEmpty() && unavailable.isEmpty() && incompatible.isEmpty();
        return new AgentRunSkillAssessment(
                normalized.surfaceId(),
                ready,
                requested,
                resolved,
                unknown,
                unavailable,
                incompatible,
                recommendations);
    }

    private static List<String> requestedSkills(List<String> skills) {
        Set<String> requested = new LinkedHashSet<>();
        for (String skill : SdkLists.copy(skills)) {
            String normalized = SdkText.trimToEmpty(skill);
            if (!normalized.isEmpty()) {
                requested.add(normalized);
            }
        }
        return requested.isEmpty() ? List.of() : List.copyOf(requested);
    }

    private static List<String> recommendations(
            String surfaceId,
            List<String> unknown,
            List<String> unavailable,
            List<String> incompatible) {
        List<String> recommendations = new ArrayList<>();
        if (!unknown.isEmpty()) {
            recommendations.add("Register or remove unknown skills: " + String.join(", ", unknown) + ".");
        }
        if (!unavailable.isEmpty()) {
            recommendations.add("Use active or preview skills instead of unavailable skills: "
                    + String.join(", ", unavailable) + ".");
        }
        if (!incompatible.isEmpty()) {
            recommendations.add("Choose skills that support surface '" + surfaceId + "': "
                    + String.join(", ", incompatible) + ".");
        }
        return recommendations.isEmpty() ? List.of() : List.copyOf(recommendations);
    }

    private static void addIfAbsent(List<String> values, String value) {
        if (!values.contains(value)) {
            values.add(value);
        }
    }
}
