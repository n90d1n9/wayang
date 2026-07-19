package tech.kayys.wayang.agent.skills.cli;

import tech.kayys.wayang.agent.spi.skills.SkillDefinition;

/**
 * Input envelope for CLI skill definition commands.
 */
record SkillsDefinitionRequest(
        String id,
        String name,
        String description,
        String category,
        String systemPrompt) {

    static SkillsDefinitionRequest fromOptions(
            String id,
            String name,
            String description,
            String category,
            String systemPrompt) {
        return new SkillsDefinitionRequest(id, name, description, category, systemPrompt);
    }

    SkillDefinition registrationDefinition() {
        return SkillDefinition.builder()
                .id(id)
                .name(defaultName())
                .description(nullToEmpty(description))
                .category(defaultCategory())
                .systemPrompt(systemPrompt)
                .build();
    }

    SkillDefinition validationDefinition() {
        return new SkillDefinition(
                blankToNull(id),
                blankToNull(name),
                blankToNull(description),
                defaultCategory(),
                blankToNull(systemPrompt),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
    }

    private String defaultName() {
        return name == null || name.isBlank() ? id : name;
    }

    private String defaultCategory() {
        return category == null || category.isBlank() ? "custom" : category;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
