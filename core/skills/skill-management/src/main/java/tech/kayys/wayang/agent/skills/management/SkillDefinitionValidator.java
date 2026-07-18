package tech.kayys.wayang.agent.skills.management;

import tech.kayys.wayang.agent.spi.skills.SkillDefinition;
import tech.kayys.wayang.agent.spi.skills.SkillValidation;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates skill definitions before management mutations.
 */
final class SkillDefinitionValidator {

    SkillValidation validate(SkillDefinition skill) {
        if (skill == null) {
            return SkillValidation.error("Skill definition is required");
        }
        List<String> errors = new ArrayList<>();
        if (isBlank(skill.id())) {
            errors.add("Skill id is required");
        }
        if (isBlank(skill.name())) {
            errors.add("Skill name is required");
        }
        if (isBlank(skill.systemPrompt())) {
            errors.add("System prompt is required");
        }
        if (skill.temperature() != null && (skill.temperature() < 0.0 || skill.temperature() > 2.0)) {
            errors.add("Temperature must be between 0.0 and 2.0");
        }
        if (skill.maxTokens() != null && skill.maxTokens() <= 0) {
            errors.add("Max tokens must be greater than zero");
        }
        return errors.isEmpty() ? SkillValidation.success() : SkillValidation.errors(errors);
    }

    void requireValid(SkillDefinition skill) {
        SkillValidation validation = validate(skill);
        if (!validation.valid()) {
            throw new IllegalArgumentException(String.join("; ", validation.errors()));
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
