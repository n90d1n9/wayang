package tech.kayys.wayang.readiness;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import tech.kayys.wayang.agent.skill.AgentSkillDiscovery;
import tech.kayys.wayang.agent.skill.AgentSkillDiscoveryService;
import tech.kayys.wayang.agent.skill.AgentSkillQuery;
import tech.kayys.wayang.agent.skill.AgentSkillState;
import tech.kayys.wayang.client.SdkCounts;
import tech.kayys.wayang.client.SdkFacets;
import tech.kayys.wayang.client.WayangReadinessAttributeMaps;
import tech.kayys.wayang.client.WayangReadinessReports;
import tech.kayys.wayang.client.WayangSkillCatalog;
import tech.kayys.wayang.skill.RegisteredSkill;

/**
 * Readiness assessor for skill catalog discovery.
 * Evaluates whether agent skills are available and discoverable.
 */
public class SkillCatalogAssessor extends ComponentReadinessAssessor {

    public static final String READINESS_ID = "wayang.skill-catalog.readiness";

    @Override
    protected String getId() {
        return READINESS_ID;
    }

    @Override
    protected String getSource() {
        return "skills";
    }

    @Override
    protected String buildProbeName() {
        return "skills.catalog_discovery";
    }

    @Override
    protected List<Map<String, Object>> validate(Object input) {
        AgentSkillDiscovery discovery = (AgentSkillDiscovery) input;
        AgentSkillDiscovery resolved = discovery == null
                ? AgentSkillDiscoveryService.create()
                        .discover(WayangSkillCatalog.defaultRegistry(), AgentSkillQuery.all())
                : discovery;

        Map<String, Object> attributes = buildAttributes(resolved);
        return issues(resolved, attributes);
    }

    @Override
    protected Map<String, Object> buildAttributes(Object input) {
        AgentSkillDiscovery discovery = (AgentSkillDiscovery) input;
        AgentSkillDiscovery resolved = discovery == null
                ? AgentSkillDiscoveryService.create()
                        .discover(WayangSkillCatalog.defaultRegistry(), AgentSkillQuery.all())
                : discovery;

        List<RegisteredSkill> skills = resolved.skills();
        return WayangReadinessAttributeMaps.ordered(
                "totalSkills", resolved.totalSkills(),
                "matchingSkills", resolved.matchingSkills(),
                "availableSkills", availableSkillCount(skills),
                "skillIds", resolved.skillIds(),
                "availableSkillIds", availableSkillIds(skills),
                "categories", resolved.categories(),
                "categoryCounts", resolved.categoryCounts(),
                "sources", resolved.sources(),
                "sourceCounts", resolved.sourceCounts(),
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
