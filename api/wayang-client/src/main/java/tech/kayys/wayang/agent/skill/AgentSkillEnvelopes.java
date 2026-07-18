package tech.kayys.wayang.agent.skill;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import tech.kayys.wayang.client.SdkMaps;
import tech.kayys.wayang.client.SdkText;
import tech.kayys.wayang.skill.RegisteredSkill;

/**
 * Wire envelope factory for skill discovery and detail payloads.
 *
 * <p>Skill envelopes live in the SDK so shell, TUI, HTTP, and embedded agent
 * products share one ordered JSON contract and one fallback discovery model.</p>
 */
public final class AgentSkillEnvelopes {

    private AgentSkillEnvelopes() {
    }

    public static Map<String, Object> discovery(String productName, AgentSkillDiscovery discovery) {
        AgentSkillDiscovery model = normalize(discovery);
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("product", SdkText.trimToEmpty(productName));
        values.put("query", query(model.query()));
        values.put("search", SdkText.blankToNull(model.search()));
        values.put("totalSkills", model.totalSkills());
        values.put("matchingSkills", model.matchingSkills());
        values.put("categories", model.categories());
        values.put("categoryCounts", model.categoryCounts());
        values.put("categorySummaries", model.categorySummaries().stream()
                .map(AgentSkillEnvelopes::facetSummary)
                .toList());
        values.put("sources", model.sources());
        values.put("sourceCounts", model.sourceCounts());
        values.put("sourceSummaries", model.sourceSummaries().stream()
                .map(AgentSkillEnvelopes::facetSummary)
                .toList());
        values.put("skillIds", model.skillIds());
        values.put("skills", model.skills().stream()
                .map(AgentSkillEnvelopes::skill)
                .toList());
        return SdkMaps.orderedCopy(values);
    }

    public static Map<String, Object> detail(String productName, RegisteredSkill skill) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("product", SdkText.trimToEmpty(productName));
        values.put("skillId", skill.id());
        values.put("skill", skill(skill));
        return SdkMaps.orderedCopy(values);
    }

    public static Map<String, Object> query(AgentSkillQuery query) {
        AgentSkillQuery normalized = query == null ? AgentSkillQuery.all() : query;
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("surfaceId", SdkText.blankToNull(normalized.surfaceId()));
        values.put("profileId", SdkText.blankToNull(normalized.profileId()));
        values.put("resolvedSurfaceId", SdkText.blankToNull(normalized.resolvedSurfaceId()));
        values.put("category", SdkText.blankToNull(normalized.category()));
        values.put("source", SdkText.blankToNull(normalized.source()));
        values.put("state", normalized.state() == null ? null : normalized.state().name());
        values.put("skillId", SdkText.blankToNull(normalized.skillId()));
        values.put("tag", SdkText.blankToNull(normalized.tag()));
        values.put("inputKey", SdkText.blankToNull(normalized.inputKey()));
        values.put("outputKey", SdkText.blankToNull(normalized.outputKey()));
        values.put("filtered", normalized.filtered());
        return SdkMaps.orderedCopy(values);
    }

    public static Map<String, Object> skill(RegisteredSkill skill) {
        AgentSkillDescriptor descriptor = skill.descriptor();
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("id", skill.id());
        values.put("name", descriptor.name());
        values.put("description", descriptor.description());
        values.put("category", descriptor.category());
        values.put("source", descriptor.source());
        values.put("version", descriptor.version());
        values.put("state", skill.state().name());
        values.put("availableForRuns", skill.availableForRuns());
        values.put("surfaceIds", descriptor.surfaceIds());
        values.put("inputKeys", descriptor.inputKeys());
        values.put("outputKeys", descriptor.outputKeys());
        values.put("tags", descriptor.tags());
        values.put("aliases", skill.aliases());
        values.put("metadata", descriptor.metadata());
        return SdkMaps.orderedCopy(values);
    }

    public static Map<String, Object> facetSummary(AgentSkillFacetSummary summary) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("name", summary.name());
        values.put("count", summary.count());
        values.put("skillIds", summary.skillIds());
        return SdkMaps.orderedCopy(values);
    }

    public static AgentSkillDiscovery normalize(AgentSkillDiscovery discovery) {
        return discovery == null
                ? AgentSkillDiscovery.of(AgentSkillQuery.all(), "", List.of(), 0)
                : discovery;
    }

}
