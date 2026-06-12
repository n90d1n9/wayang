package tech.kayys.wayang.agent.skills.cli;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.spi.skills.SkillDefinition;

import static org.assertj.core.api.Assertions.assertThat;

class SkillsDefinitionRequestTest {

    @Test
    void buildsRegistrationDefinitionWithDisplayDefaults() {
        SkillDefinition definition = SkillsDefinitionRequest.fromOptions(
                "planner",
                "",
                null,
                "",
                "Plan carefully.")
                .registrationDefinition();

        assertThat(definition.id()).isEqualTo("planner");
        assertThat(definition.name()).isEqualTo("planner");
        assertThat(definition.description()).isEmpty();
        assertThat(definition.category()).isEqualTo("custom");
        assertThat(definition.systemPrompt()).isEqualTo("Plan carefully.");
    }

    @Test
    void buildsValidationDefinitionWithNullableRequiredFields() {
        SkillDefinition definition = SkillsDefinitionRequest.fromOptions(
                "",
                "",
                "",
                "",
                "")
                .validationDefinition();

        assertThat(definition.id()).isNull();
        assertThat(definition.name()).isNull();
        assertThat(definition.description()).isNull();
        assertThat(definition.category()).isEqualTo("custom");
        assertThat(definition.systemPrompt()).isNull();
    }
}
