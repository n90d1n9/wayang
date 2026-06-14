package tech.kayys.wayang.gollek.sdk;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record AgentSkillDiscovery(
        AgentSkillQuery query,
        String search,
        List<RegisteredSkill> skills,
        List<String> categories,
        Map<String, Integer> categoryCounts,
        List<String> sources,
        Map<String, Integer> sourceCounts,
        List<String> skillIds,
        int totalSkills) {

    public AgentSkillDiscovery {
        query = query == null ? AgentSkillQuery.all() : query;
        search = SdkText.trimToEmpty(search);
        skills = SdkLists.copy(skills);
        categories = SdkLists.copy(categories);
        categoryCounts = SdkCounts.copyPositiveTextKeys(categoryCounts);
        sources = SdkLists.copy(sources);
        sourceCounts = SdkCounts.copyPositiveTextKeys(sourceCounts);
        skillIds = SdkLists.copy(skillIds);
        totalSkills = Math.max(0, totalSkills);
    }

    public static AgentSkillDiscovery of(
            AgentSkillQuery query,
            String search,
            List<RegisteredSkill> skills,
            int totalSkills) {
        List<RegisteredSkill> matches = SdkLists.copy(skills);
        return new AgentSkillDiscovery(
                query,
                search,
                matches,
                SdkFacets.textValues(matches, skill -> Facet.CATEGORY.value(skill)),
                SdkFacets.textCounts(matches, skill -> Facet.CATEGORY.value(skill)),
                SdkFacets.textValues(matches, skill -> Facet.SOURCE.value(skill)),
                SdkFacets.textCounts(matches, skill -> Facet.SOURCE.value(skill)),
                matches.stream().map(RegisteredSkill::id).toList(),
                totalSkills);
    }

    public int matchingSkills() {
        return skills.size();
    }

    public boolean empty() {
        return skills.isEmpty();
    }

    public List<AgentSkillFacetSummary> categorySummaries() {
        return summaries(Facet.CATEGORY);
    }

    public List<AgentSkillFacetSummary> sourceSummaries() {
        return summaries(Facet.SOURCE);
    }

    private List<AgentSkillFacetSummary> summaries(Facet facet) {
        Map<String, List<String>> idsByFacet = new LinkedHashMap<>();
        for (RegisteredSkill skill : skills) {
            idsByFacet.computeIfAbsent(facet.value(skill), ignored -> new ArrayList<>()).add(skill.id());
        }
        return idsByFacet.entrySet().stream()
                .map(entry -> new AgentSkillFacetSummary(
                        entry.getKey(),
                        entry.getValue().size(),
                        entry.getValue()))
                .toList();
    }

    private enum Facet {
        CATEGORY {
            @Override
            String value(RegisteredSkill skill) {
                return skill.descriptor().category();
            }
        },
        SOURCE {
            @Override
            String value(RegisteredSkill skill) {
                return skill.descriptor().source();
            }
        };

        abstract String value(RegisteredSkill skill);
    }
}
