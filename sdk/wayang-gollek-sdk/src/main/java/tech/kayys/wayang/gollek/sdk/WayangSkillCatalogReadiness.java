package tech.kayys.wayang.gollek.sdk;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class WayangSkillCatalogReadiness {

    public static final String READINESS_ID = "wayang.skill-catalog.readiness";

    private WayangSkillCatalogReadiness() {
    }

    public static WayangReadinessReport assess(AgentSkillDiscovery discovery) {
        AgentSkillDiscovery resolved = discovery == null
                ? AgentSkillDiscoveryService.create()
                        .discover(WayangSkillCatalog.defaultRegistry(), AgentSkillQuery.all())
                : discovery;
        Map<String, Object> attributes = attributes(resolved);
        List<Map<String, Object>> issues = issues(resolved, attributes);
        boolean ready = issues.isEmpty();
        return WayangReadinessReport.from(
                READINESS_ID,
                ready,
                WayangReadinessReports.exitCode(ready),
                issues.size(),
                List.of(WayangReadinessReports.probe(
                        "skills.catalog_discovery",
                        true,
                        ready,
                        issues.size(),
                        attributes)),
                issues,
                attributes);
    }

    private static Map<String, Object> attributes(AgentSkillDiscovery discovery) {
        List<RegisteredSkill> skills = discovery.skills();
        return WayangReadinessAttributeMaps.ordered(
                "totalSkills", discovery.totalSkills(),
                "matchingSkills", discovery.matchingSkills(),
                "availableSkills", availableSkillCount(skills),
                "skillIds", discovery.skillIds(),
                "availableSkillIds", availableSkillIds(skills),
                "categories", discovery.categories(),
                "categoryCounts", discovery.categoryCounts(),
                "sources", discovery.sources(),
                "sourceCounts", discovery.sourceCounts(),
                "surfaceIds", surfaceIds(skills),
                "surfaceIdCounts", surfaceIdCounts(skills),
                "stateCounts", stateCounts(skills));
    }

    private static List<Map<String, Object>> issues(
            AgentSkillDiscovery discovery,
            Map<String, Object> attributes) {
        if (discovery.totalSkills() == 0) {
            return List.of(WayangReadinessReports.issue(
                    "skill_catalog_empty",
                    "skills",
                    "Dynamic skill discovery catalog is empty.",
                    attributes));
        }
        if (discovery.matchingSkills() == 0) {
            return List.of(WayangReadinessReports.issue(
                    "skill_catalog_unmatched",
                    "skills",
                    "Dynamic skill discovery did not return any matching skills.",
                    attributes));
        }
        if (availableSkillCount(discovery.skills()) == 0) {
            return List.of(WayangReadinessReports.issue(
                    "skill_catalog_unavailable",
                    "skills",
                    "Dynamic skill discovery has no active or preview skills available for runs.",
                    attributes));
        }
        return List.of();
    }

    private static int availableSkillCount(List<RegisteredSkill> skills) {
        return (int) skills.stream()
                .filter(RegisteredSkill::availableForRuns)
                .count();
    }

    private static List<String> availableSkillIds(List<RegisteredSkill> skills) {
        return skills.stream()
                .filter(RegisteredSkill::availableForRuns)
                .map(RegisteredSkill::id)
                .toList();
    }

    private static List<String> surfaceIds(List<RegisteredSkill> skills) {
        return SdkFacets.flatValues(skills, skill -> skill.descriptor().surfaceIds());
    }

    private static Map<String, Integer> surfaceIdCounts(List<RegisteredSkill> skills) {
        return SdkFacets.flatCounts(skills, skill -> skill.descriptor().surfaceIds());
    }

    private static Map<String, Integer> stateCounts(List<RegisteredSkill> skills) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (RegisteredSkill skill : skills) {
            counts.merge(stateId(skill.state()), 1, Integer::sum);
        }
        return SdkCounts.copyPositiveTextKeys(counts);
    }

    private static String stateId(AgentSkillState state) {
        AgentSkillState resolved = state == null ? AgentSkillState.ACTIVE : state;
        return resolved.name().toLowerCase(Locale.ROOT).replace('_', '-');
    }
}
