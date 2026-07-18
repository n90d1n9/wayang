package tech.kayys.wayang.agent.skills.cli;

/**
 * Input envelope for listing skill definitions.
 */
record SkillsDefinitionListRequest(
        String category,
        boolean includeDisabled) {

    SkillsDefinitionListRequest {
        category = category == null ? "" : category.trim();
    }

    static SkillsDefinitionListRequest fromOptions(String category, boolean includeDisabled) {
        return new SkillsDefinitionListRequest(category, includeDisabled);
    }
}
