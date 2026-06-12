package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.spi.skills.SkillDefinition;
import tech.kayys.wayang.agent.spi.skills.SkillValidation;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SkillDefinitionValidatorTest {

    private final SkillDefinitionValidator validator = new SkillDefinitionValidator();

    @Test
    void acceptsCompleteSkillDefinition() {
        SkillValidation validation = validator.validate(TestSkillDefinitions.builder("planner")
                .name("Planner")
                .description("Plans tasks")
                .category("REASONING")
                .systemPrompt("Plan carefully.")
                .temperature(0.7)
                .maxTokens(512)
                .build());

        assertThat(validation.valid()).isTrue();
        assertThat(validation.errors()).isEmpty();
    }

    @Test
    void rejectsMissingRequiredFieldsAndInvalidGenerationSettings() {
        SkillDefinition invalid = new SkillDefinition(
                "",
                "",
                "Broken",
                "TEST",
                "",
                Map.of(),
                null,
                3.0,
                0,
                null,
                null,
                List.of(),
                null,
                Map.of());

        SkillValidation validation = validator.validate(invalid);

        assertThat(validation.valid()).isFalse();
        assertThat(validation.errors())
                .containsExactly(
                        "Skill id is required",
                        "Skill name is required",
                        "System prompt is required",
                        "Temperature must be between 0.0 and 2.0",
                        "Max tokens must be greater than zero");
    }

    @Test
    void rejectsNullDefinition() {
        SkillValidation validation = validator.validate(null);

        assertThat(validation.valid()).isFalse();
        assertThat(validation.errors()).containsExactly("Skill definition is required");
    }

    @Test
    void requireValidThrowsJoinedValidationErrors() {
        SkillDefinition invalid = new SkillDefinition(
                "planner",
                "",
                "Broken",
                "TEST",
                "",
                Map.of(),
                null,
                null,
                null,
                null,
                null,
                List.of(),
                null,
                Map.of());

        assertThatThrownBy(() -> validator.requireValid(invalid))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Skill name is required")
                .hasMessageContaining("System prompt is required");
    }
}
