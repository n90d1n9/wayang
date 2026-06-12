package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SkillManagementEventSinkFactoryTest {

    @Test
    void overrideSkipsConfiguredEventStoreCreationAndValidation() {
        SkillManagementEventSink override = event -> {
        };
        SkillManagementEventSinkFactory factory = new SkillManagementEventSinkFactory(
                new SkillManagementEventStoreFactory(),
                override);

        assertThat(factory.create(SkillManagementEventStoreConfig.objectStorage("tenant-a/events")))
                .isSameAs(override);
        assertThat(factory.validate(SkillManagementEventStoreConfig.objectStorage("tenant-a/events"))
                .validConfiguration())
                .isTrue();
        assertThat(factory.override()).isSameAs(override);
    }

    @Test
    void delegatesToConfiguredEventStoreWithoutOverride() {
        SkillManagementEventSinkFactory factory = new SkillManagementEventSinkFactory(
                new SkillManagementEventStoreFactory(),
                null);

        assertThat(factory.create(SkillManagementEventStoreConfig.memory()))
                .isInstanceOf(InMemorySkillManagementEventSink.class);
        assertThat(factory.validate(SkillManagementEventStoreConfig.memory()).validConfiguration())
                .isTrue();
        assertThat(factory.override()).isNull();
    }

    @Test
    void overridePruneValidationUsesEffectiveSinkCapabilities() {
        SkillManagementEventSink writeOnlySink = event -> {
        };
        SkillManagementEventSinkFactory factory = new SkillManagementEventSinkFactory(
                new SkillManagementEventStoreFactory(),
                writeOnlySink);

        SkillStoreConfigValidationResult validation =
                factory.validatePruneSupport(SkillManagementEventStoreConfig.memory());

        assertThat(validation.errors())
                .containsExactly(SkillManagementEventPruner.PRUNE_EVENTS_CAPABILITY_REQUIRED);
    }
}
