package tech.kayys.wayang.agent.hermes;

import tech.kayys.wayang.agent.skills.management.SkillArtifact;
import tech.kayys.wayang.agent.skills.management.SkillArtifactReference;
import tech.kayys.wayang.agent.spi.skills.SkillDefinition;

/**
 * Renders Hermes-learned skills into the portable agentskills SKILL.md artifact.
 */
public final class HermesSkillMarkdownRenderer {

    public SkillArtifact render(SkillDefinition skill, HermesLearningSignal signal) {
        String markdown = """
                ---
                name: %s
                description: %s
                version: %s
                metadata:
                  wayang:
                    mode: hermes-agent
                    learned-from-request: %s
                    revision: %s
                    lineage-root-skill-id: %s
                    merge-strategy: %s
                ---
                # %s

                %s
                """.formatted(
                skill.id(),
                escapeFrontmatter(skill.description()),
                String.valueOf(skill.metadata().getOrDefault("version", "1.0.0")),
                signal.requestId(),
                metadataValue(skill, "hermes.revision", "1"),
                metadataValue(skill, "hermes.lineageRootSkillId", skill.id()),
                metadataValue(skill, "hermes.mergeStrategy", HermesSkillRevisionMetadata.INITIAL_STRATEGY),
                skill.name(),
                skill.systemPrompt());
        return SkillArtifact.text(
                SkillArtifactReference.resource(skill.id(), "SKILL.md", "current"),
                markdown);
    }

    private static String escapeFrontmatter(String value) {
        return HermesText.oneLine(value).replace("\"", "'");
    }

    private static String metadataValue(SkillDefinition skill, String key, String fallback) {
        Object value = skill.metadata().get(key);
        return escapeFrontmatter(value == null ? fallback : value.toString());
    }
}
