package tech.kayys.wayang.agent.skills.cli;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SkillsConfigCatalogServiceTest {

    private final SkillsConfigCatalogService service = new SkillsConfigCatalogService();

    @Test
    void defaultsSelectFullCatalog() {
        SkillsConfigCatalogRequest request = SkillsConfigCatalogRequest.defaults();

        assertThat(request.groupName()).isEmpty();
        assertThat(request.hasGroup()).isFalse();
        assertThat(service.catalog(request).groups()).hasSize(6);
        assertThat(service.catalog(request).hintCount()).isEqualTo(33);
    }

    @Test
    void normalizesAndSelectsSingleGroup() {
        SkillsConfigCatalogRequest request = SkillsConfigCatalogRequest.forGroup(" profile-options ");

        assertThat(request.groupName()).isEqualTo("profile-options");
        assertThat(request.hasGroup()).isTrue();
        assertThat(service.catalog(request).groups())
                .extracting(group -> group.name())
                .containsExactly("profile-options");
        assertThat(service.catalog(request).hintCount()).isEqualTo(4);
    }

    @Test
    void propagatesUnknownGroupErrors() {
        assertThatThrownBy(() -> service.catalog(SkillsConfigCatalogRequest.forGroup("missing-group")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unknown runtime config hint group: missing-group");
    }
}
