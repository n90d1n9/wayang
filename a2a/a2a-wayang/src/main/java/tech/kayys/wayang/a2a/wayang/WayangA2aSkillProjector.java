package tech.kayys.wayang.a2a.wayang;

import tech.kayys.wayang.a2a.core.A2aAgentSkill;
import tech.kayys.wayang.agent.spi.AgentSkill;
import tech.kayys.wayang.agent.spi.skills.SkillDefinition;
import tech.kayys.wayang.agent.spi.skills.SkillMetadataKeys;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Projects Wayang skill contracts into A2A AgentSkill records.
 */
public final class WayangA2aSkillProjector {

    public A2aAgentSkill fromSkillDefinition(SkillDefinition skill) {
        if (skill == null) {
            throw new IllegalArgumentException("skill must not be null");
        }
        Map<String, Object> metadata = WayangA2aMaps.copyMap(skill.metadata());
        List<String> tags = tagsForDefinition(skill, metadata);
        List<String> outputModes = outputModes(metadata);
        return new A2aAgentSkill(
                WayangA2aMaps.required(skill.id(), "skill.id"),
                fallback(skill.name(), skill.id()),
                fallback(skill.description(), "Wayang skill " + skill.id()),
                tags,
                WayangA2aMaps.firstStringList(metadata, WayangA2a.METADATA_EXAMPLES, "a2a.examples"),
                WayangA2aMaps.firstStringList(metadata, WayangA2a.METADATA_INPUT_MODES, "a2a.inputModes"),
                outputModes,
                WayangA2aMaps.objectList(metadata.get(WayangA2a.METADATA_SECURITY_REQUIREMENTS)));
    }

    public A2aAgentSkill fromAgentSkill(AgentSkill skill) {
        if (skill == null) {
            throw new IllegalArgumentException("skill must not be null");
        }
        List<String> tags = new ArrayList<>();
        tags.addAll(skill.aliases());
        tags.add(skill.category());
        tags.add(skill.id());
        return new A2aAgentSkill(
                WayangA2aMaps.required(skill.id(), "skill.id"),
                fallback(skill.name(), skill.id()),
                fallback(skill.description(), "Wayang skill " + skill.id()),
                cleanTags(tags),
                List.of(),
                List.of(),
                List.of(),
                List.of());
    }

    public List<A2aAgentSkill> fromSkillDefinitions(List<SkillDefinition> skills) {
        if (skills == null || skills.isEmpty()) {
            return List.of();
        }
        return skills.stream().map(this::fromSkillDefinition).toList();
    }

    public List<A2aAgentSkill> fromAgentSkills(List<AgentSkill> skills) {
        if (skills == null || skills.isEmpty()) {
            return List.of();
        }
        return skills.stream().map(this::fromAgentSkill).toList();
    }

    private static List<String> tagsForDefinition(SkillDefinition skill, Map<String, Object> metadata) {
        List<String> tags = new ArrayList<>();
        tags.addAll(SkillMetadataKeys.tags(metadata));
        tags.addAll(SkillMetadataKeys.domains(metadata));
        tags.add(skill.category());
        tags.addAll(skill.tools());
        tags.add(skill.id());
        return cleanTags(tags);
    }

    private static List<String> outputModes(Map<String, Object> metadata) {
        List<String> configured = WayangA2aMaps.firstStringList(metadata, WayangA2a.METADATA_OUTPUT_MODES, "a2a.outputModes");
        if (!configured.isEmpty()) {
            return configured;
        }
        return WayangA2aMaps.firstString(metadata, SkillMetadataKeys.KEY_OUTPUT_FORMAT)
                .map(WayangA2aSkillProjector::outputFormatToMediaType)
                .map(List::of)
                .orElse(List.of());
    }

    private static String outputFormatToMediaType(String value) {
        String normalized = value.toLowerCase();
        return switch (normalized) {
            case "json" -> WayangA2a.DEFAULT_JSON_MEDIA_TYPE;
            case "text", "plain" -> WayangA2a.DEFAULT_TEXT_MEDIA_TYPE;
            default -> value.contains("/") ? value : value;
        };
    }

    private static List<String> cleanTags(List<String> values) {
        List<String> tags = values.stream()
                .map(WayangA2aMaps::optional)
                .filter(value -> value != null && !value.equalsIgnoreCase("null"))
                .distinct()
                .toList();
        return tags.isEmpty() ? List.of("wayang") : tags;
    }

    private static String fallback(String value, String fallback) {
        String normalized = WayangA2aMaps.optional(value);
        return normalized == null ? WayangA2aMaps.required(fallback, "fallback") : normalized;
    }
}
