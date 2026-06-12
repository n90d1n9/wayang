package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SkillStoreProviderRegistryTest {

    @Test
    void createsAndValidatesRegisteredProviders() {
        SkillStoreProviderRegistry<TestKind, String, String> registry = SkillStoreProviderRegistry
                .<TestKind, String, String>builder()
                .register(
                        TestKind.MEMORY,
                        config -> "store:" + config,
                        config -> SkillStoreConfigValidationResult.valid())
                .build();

        assertThat(registry.supports(TestKind.MEMORY)).isTrue();
        assertThat(registry.kinds()).containsExactly(TestKind.MEMORY);
        assertThat(registry.create(TestKind.MEMORY, "definitions")).isEqualTo("store:definitions");
        assertThat(registry.validate(TestKind.MEMORY, "definitions").validConfiguration()).isTrue();
    }

    @Test
    void rejectsUnregisteredProvidersWithKindContext() {
        SkillStoreProviderRegistry<TestKind, String, String> registry = SkillStoreProviderRegistry
                .<TestKind, String, String>builder()
                .build();

        assertThatThrownBy(() -> registry.create(TestKind.OBJECT, "definitions"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("No skill store provider registered for kind: OBJECT");
    }

    @Test
    void snapshotsRegisteredProviders() {
        SkillStoreProviderRegistry.Builder<TestKind, String, String> builder = SkillStoreProviderRegistry
                .<TestKind, String, String>builder()
                .register(
                        TestKind.MEMORY,
                        config -> "memory",
                        config -> SkillStoreConfigValidationResult.valid());
        SkillStoreProviderRegistry<TestKind, String, String> registry = builder.build();

        builder.register(
                TestKind.OBJECT,
                config -> "object",
                config -> SkillStoreConfigValidationResult.valid());

        assertThat(registry.kinds()).containsExactly(TestKind.MEMORY);
    }

    @Test
    void preservesProviderRegistrationOrder() {
        SkillStoreProviderRegistry<TestKind, String, String> registry = SkillStoreProviderRegistry
                .<TestKind, String, String>builder()
                .register(
                        TestKind.MEMORY,
                        config -> "memory",
                        config -> SkillStoreConfigValidationResult.valid())
                .register(
                        TestKind.OBJECT,
                        config -> "object",
                        config -> SkillStoreConfigValidationResult.valid())
                .build();

        assertThat(registry.kinds()).containsExactly(TestKind.MEMORY, TestKind.OBJECT);
    }

    @Test
    void rejectsDuplicateProviderKinds() {
        SkillStoreProviderRegistry.Builder<TestKind, String, String> builder = SkillStoreProviderRegistry
                .<TestKind, String, String>builder()
                .register(
                        TestKind.MEMORY,
                        config -> "memory",
                        config -> SkillStoreConfigValidationResult.valid());

        assertThatThrownBy(() -> builder.register(
                TestKind.MEMORY,
                config -> "replacement",
                config -> SkillStoreConfigValidationResult.valid()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Duplicate skill store provider registered for kind: MEMORY");
    }

    private enum TestKind {
        MEMORY,
        OBJECT
    }
}
