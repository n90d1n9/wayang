package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SkillManagementRuntimeDependenciesTest {

    @Test
    void normalizesNullRuntimeDependencyOptionals() {
        SkillManagementServiceConfig config = SkillManagementServiceConfig.defaults();

        SkillManagementRuntimeDependencies dependencies =
                SkillManagementRuntimeDependencies.of(config, null, null);

        assertThat(dependencies.config()).isSameAs(config);
        assertThat(dependencies.objectStorageService()).isEmpty();
        assertThat(dependencies.jdbcDataSource()).isEmpty();
    }

    @Test
    void createsDefaultRuntimeFactoryWhenNoExternalDependenciesArePresent() {
        SkillManagementServiceConfig config = SkillManagementServiceConfig.defaults();
        SkillManagementRuntimeDependencies dependencies =
                SkillManagementRuntimeDependencies.of(config, null, null);

        SkillManagementServiceFactory factory = dependencies.serviceFactory(new TestSkillRegistry());

        assertThat(factory.validate(config).validConfiguration()).isTrue();
        assertThat(factory.create(config).inspectManagement().await().indefinitely().ready()).isTrue();
    }
}
