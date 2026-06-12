package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SkillStoreConfigValidationResultTest {

    @Test
    void reportsValidConfigurationWithoutErrors() {
        SkillStoreConfigValidationResult result = SkillStoreConfigValidationResult.valid();

        assertThat(result.validConfiguration()).isTrue();
        assertThat(result.errors()).isEmpty();
        assertThat(result.message()).isEmpty();
    }

    @Test
    void throwsJoinedValidationErrors() {
        SkillStoreConfigValidationResult result =
                new SkillStoreConfigValidationResult(List.of("first problem", "second problem"));

        assertThat(result.validConfiguration()).isFalse();
        assertThat(result.message()).isEqualTo("first problem; second problem");
        assertThatThrownBy(result::throwIfInvalid)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("first problem; second problem");
    }

    @Test
    void combinesValidationResults() {
        SkillStoreConfigValidationResult result = SkillStoreConfigValidationResult.combine(
                SkillStoreConfigValidationResult.valid(),
                SkillStoreConfigValidationResult.error("definition problem"),
                SkillStoreConfigValidationResult.error("event problem"));

        assertThat(result.validConfiguration()).isFalse();
        assertThat(result.errors()).containsExactly("definition problem", "event problem");
    }
}
