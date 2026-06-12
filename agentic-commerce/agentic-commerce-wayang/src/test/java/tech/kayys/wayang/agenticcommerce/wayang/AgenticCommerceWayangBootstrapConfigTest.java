package tech.kayys.wayang.agenticcommerce.wayang;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgenticCommerceWayangBootstrapConfigTest {

    @Test
    void defaultsInstallAndRequireFullCheckoutBootstrap() {
        AgenticCommerceWayangBootstrapConfig config = AgenticCommerceWayangBootstrapConfig.defaults();

        assertThat(config.skillIds()).containsExactlyElementsOf(AgenticCommerceWayang.checkoutSkillIds());
        assertThat(config.includeDefinitions()).isTrue();
        assertThat(config.includeRuntimeSkills()).isTrue();
        assertThat(config.requireSkillRegistration()).isTrue();
        assertThat(config.requireSmokeProbe()).isTrue();
        assertThat(config.requireBindingRoutes()).isTrue();
        assertThat(config.toMap())
                .containsEntry("includeDefinitions", true)
                .containsEntry("includeRuntimeSkills", true)
                .containsEntry("requireSkillRegistration", true);
    }

    @Test
    void bindsFromMapWithAliasesAndNormalizesSkillIds() {
        AgenticCommerceWayangBootstrapConfig config = AgenticCommerceWayangBootstrapConfig.fromMap(Map.of(
                "skills",
                List.of(
                        AgenticCommerceWayang.SKILL_CREATE_CHECKOUT,
                        AgenticCommerceWayang.SKILL_CREATE_CHECKOUT,
                        " ",
                        AgenticCommerceWayang.SKILL_RETRIEVE_CHECKOUT),
                "definitions",
                "yes",
                "runtimeSkills",
                "no",
                "requireSkills",
                "false",
                "smokeRequired",
                "1",
                "bindingRequired",
                "0"));

        assertThat(config.skillIds())
                .containsExactly(
                        AgenticCommerceWayang.SKILL_CREATE_CHECKOUT,
                        AgenticCommerceWayang.SKILL_RETRIEVE_CHECKOUT);
        assertThat(config.includeDefinitions()).isTrue();
        assertThat(config.includeRuntimeSkills()).isFalse();
        assertThat(config.requireSkillRegistration()).isFalse();
        assertThat(config.requireSmokeProbe()).isTrue();
        assertThat(config.requireBindingRoutes()).isFalse();
    }

    @Test
    void rejectsBootstrapWithoutDefinitionsOrRuntimeSkills() {
        assertThatThrownBy(() -> AgenticCommerceWayangBootstrapConfig.builder()
                .includeDefinitions(false)
                .includeRuntimeSkills(false)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("definitions or runtime skills");
    }
}
