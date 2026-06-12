package tech.kayys.wayang.agent.skills.cli;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.skills.management.SkillManagementServiceConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SkillsPersistenceConfigSourceTest {

    @Test
    void resolvesDefaultSourceFromProvidedConfig() {
        SkillManagementServiceConfig config = SkillManagementServiceConfig.defaults();

        SkillsPersistenceConfigSource source = SkillsPersistenceConfigSource.resolve(
                "",
                false,
                config);

        assertThat(source.source()).isEqualTo("default");
        assertThat(source.profile()).isEmpty();
        assertThat(source.runtime()).isFalse();
        assertThat(source.config()).isSameAs(config);
    }

    @Test
    void resolvesProfileSourceThroughAliases() {
        SkillsPersistenceConfigSource source = SkillsPersistenceConfigSource.resolve(
                "rustfs",
                false,
                SkillManagementServiceConfig.defaults());

        assertThat(source.source()).isEqualTo("profile");
        assertThat(source.profile()).isEqualTo("object-storage");
        assertThat(source.runtime()).isFalse();
        assertThat(source.config().persistenceStrategy().kindLabel()).isEqualTo("object-storage");
    }

    @Test
    void resolvesRuntimeSource() {
        SkillsPersistenceConfigSource source = SkillsPersistenceConfigSource.resolve(
                "",
                true,
                SkillManagementServiceConfig.defaults());

        assertThat(source.source()).isEqualTo("runtime");
        assertThat(source.profile()).isEmpty();
        assertThat(source.runtime()).isTrue();
        assertThat(source.config()).isNotNull();
    }

    @Test
    void rejectsConflictingSources() {
        assertThatThrownBy(() -> SkillsPersistenceConfigSource.resolve(
                "hybrid",
                true,
                SkillManagementServiceConfig.defaults()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Choose only one skill persistence config source: --profile or --runtime.");
    }
}
