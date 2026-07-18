package tech.kayys.wayang.agent.core.skills.integration;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Result of applying a skill lifecycle refresh request across integration caches.
 */
public record SkillIntegrationRefreshResult(
        SkillIntegrationRefreshRequest request,
        List<SkillIntegrationRefreshImpact> impacts) {

    public SkillIntegrationRefreshResult {
        request = request == null ? SkillIntegrationRefreshRequest.empty() : request;
        impacts = impacts == null ? List.of() : List.copyOf(impacts);
    }

    public static SkillIntegrationRefreshResult empty() {
        return new SkillIntegrationRefreshResult(SkillIntegrationRefreshRequest.empty(), List.of());
    }

    public boolean hasRequestedChanges() {
        return request.hasChanges();
    }

    public boolean hasIntegrationWork() {
        return impacts.stream().anyMatch(SkillIntegrationRefreshImpact::hasWork);
    }

    public boolean hasSkippedSkills() {
        return impacts.stream().anyMatch(SkillIntegrationRefreshImpact::hasSkipped);
    }

    public Optional<SkillIntegrationRefreshImpact> impact(String integrationKey) {
        if (integrationKey == null || integrationKey.isBlank()) {
            return Optional.empty();
        }
        String normalized = integrationKey.trim();
        return impacts.stream()
                .filter(impact -> Objects.equals(impact.integrationKey(), normalized))
                .findFirst();
    }

    public List<String> refreshedIntegrationKeys() {
        return impacts.stream()
                .filter(SkillIntegrationRefreshImpact::hasWork)
                .map(SkillIntegrationRefreshImpact::integrationKey)
                .toList();
    }
}
