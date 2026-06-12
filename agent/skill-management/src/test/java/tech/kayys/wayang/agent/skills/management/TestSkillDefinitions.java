package tech.kayys.wayang.agent.skills.management;

import tech.kayys.wayang.agent.spi.skills.SkillDefinition;

import java.util.List;
import java.util.Map;

/**
 * Shared valid skill definitions for skill-management tests.
 */
final class TestSkillDefinitions {

    private TestSkillDefinitions() {
    }

    static SkillDefinition basic(String id) {
        return builder(id).build();
    }

    static SkillDefinition named(String id, String name) {
        return builder(id)
                .name(name)
                .build();
    }

    static SkillDefinition categorized(String id, String category) {
        return builder(id)
                .category(category)
                .build();
    }

    static SkillDefinition namedCategory(String id, String name, String category) {
        return builder(id)
                .name(name)
                .category(category)
                .build();
    }

    static SkillDefinition withTools(
            String id,
            String name,
            String category,
            String description,
            List<String> tools) {
        return builder(id)
                .name(name)
                .category(category)
                .description(description)
                .tools(tools)
                .build();
    }

    static SkillDefinition withMetadata(String id, Map<String, Object> metadata) {
        return builder(id)
                .metadata(metadata)
                .build();
    }

    static SkillDefinition withSystemPromptAndMetadata(
            String id,
            String systemPrompt,
            Map<String, Object> metadata) {
        return builder(id)
                .systemPrompt(systemPrompt)
                .metadata(metadata)
                .build();
    }

    static SkillDefinition.Builder builder(String id) {
        return SkillDefinition.builder()
                .id(id)
                .name(id)
                .description("Test skill")
                .category("TEST")
                .systemPrompt("Do the thing.");
    }
}
