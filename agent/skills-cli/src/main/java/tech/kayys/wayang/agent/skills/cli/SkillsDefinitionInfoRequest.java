package tech.kayys.wayang.agent.skills.cli;

/**
 * Input envelope for showing one skill definition.
 */
record SkillsDefinitionInfoRequest(String skillId) {

    SkillsDefinitionInfoRequest {
        skillId = skillId == null ? "" : skillId;
    }

    static SkillsDefinitionInfoRequest fromOptions(String skillId) {
        return new SkillsDefinitionInfoRequest(skillId);
    }
}
