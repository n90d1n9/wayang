package tech.kayys.wayang.agent.skills.cli;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SkillsPersistenceConfigValidationRequestTest {

    @Test
    void defaultsToNonDurableDefaultSource() {
        SkillsPersistenceConfigValidationRequest request =
                SkillsPersistenceConfigValidationRequest.defaults();

        assertThat(request.profileName()).isEmpty();
        assertThat(request.runtimeConfig()).isFalse();
        assertThat(request.policy().requireDurable()).isFalse();
    }

    @Test
    void normalizesProfileNameAndDefaultsPolicy() {
        SkillsPersistenceConfigValidationRequest request =
                new SkillsPersistenceConfigValidationRequest(" rustfs ", false, null);

        assertThat(request.profileName()).isEqualTo("rustfs");
        assertThat(request.runtimeConfig()).isFalse();
        assertThat(request.policy().requireDurable()).isFalse();
    }

    @Test
    void buildsDurabilityPolicyFromCliOptions() {
        SkillsPersistenceConfigValidationRequest request =
                SkillsPersistenceConfigValidationRequest.fromOptions(" hybrid ", true, true);

        assertThat(request.profileName()).isEqualTo("hybrid");
        assertThat(request.runtimeConfig()).isTrue();
        assertThat(request.policy().requireDurable()).isTrue();
    }
}
