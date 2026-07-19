package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SkillStoreFactorySupportTest {

    @Test
    void copiesCustomStoreMapsDefensively() {
        Map<String, String> stores = new LinkedHashMap<>();
        stores.put("primary", "store-a");

        Map<String, String> copied = SkillStoreFactorySupport.customStores(stores);
        stores.put("fallback", "store-b");

        assertThat(copied).containsOnly(Map.entry("primary", "store-a"));
    }

    @Test
    void normalizesNullCustomStoreMapsToEmpty() {
        assertThat(SkillStoreFactorySupport.customStores(null)).isEmpty();
    }

    @Test
    void resolvesNamedCustomStores() {
        String store = SkillStoreFactorySupport.customStore(
                Map.of("audit-events", "sink"),
                "audit-events",
                "event store");

        assertThat(store).isEqualTo("sink");
    }

    @Test
    void rejectsMissingCustomStoresWithContext() {
        assertThatThrownBy(() -> SkillStoreFactorySupport.customStore(
                Map.of(),
                "missing",
                "event store"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("No custom event store registered for: missing");
    }

    @Test
    void requiresDependenciesWithKindContext() {
        assertThat(SkillStoreFactorySupport.requiredDependency(
                "registry",
                "registry",
                SkillDefinitionStoreConfig.Kind.REGISTRY,
                "Skill store"))
                .isEqualTo("registry");
        assertThatThrownBy(() -> SkillStoreFactorySupport.requiredDependency(
                null,
                "jdbcDataSource",
                SkillDefinitionStoreConfig.Kind.JDBC,
                "Skill store"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Skill store kind JDBC requires dependency: jdbcDataSource");
    }

    @Test
    void requiresDependenciesWithExplicitMessage() {
        assertThat(SkillStoreFactorySupport.requiredDependency(
                "datasource",
                "JDBC event store requires a DataSource",
                "jdbcDataSource"))
                .isEqualTo("datasource");
        assertThatThrownBy(() -> SkillStoreFactorySupport.requiredDependency(
                null,
                "JDBC event store requires a DataSource",
                "jdbcDataSource"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("JDBC event store requires a DataSource");
    }

    @Test
    void describesDependencyRequirementsWithKindContext() {
        SkillStoreFactorySupport.DependencyRequirement<Object> requirement =
                SkillStoreFactorySupport.dependencyRequirement(
                        null,
                        "objectStore",
                        SkillArtifactStoreConfig.Kind.OBJECT_STORAGE,
                        "Artifact store");

        assertThat(requirement.name()).isEqualTo("objectStore");
        assertThat(requirement.missingMessage())
                .isEqualTo("Artifact store kind OBJECT_STORAGE requires dependency: objectStore");
        assertThat(SkillStoreFactorySupport.validateRequiredDependency(requirement).errors())
                .containsExactly("Artifact store kind OBJECT_STORAGE requires dependency: objectStore");
    }

    @Test
    void requiresDependencyRequirementsWithExplicitMessage() {
        SkillStoreFactorySupport.DependencyRequirement<String> requirement =
                SkillStoreFactorySupport.dependencyRequirement(
                        "datasource",
                        "jdbcDataSource",
                        "JDBC event store requires a DataSource");

        assertThat(SkillStoreFactorySupport.requiredDependency(requirement)).isEqualTo("datasource");
        assertThat(SkillStoreFactorySupport.validateRequiredDependency(requirement).validConfiguration())
                .isTrue();
    }

    @Test
    void validatesPrimaryFallbackChildrenInOrder() {
        SkillStoreConfigValidationResult result = SkillStoreFactorySupport.validatePrimaryFallback(
                "primary",
                "fallback",
                name -> SkillStoreConfigValidationResult.error(name + " unavailable"));

        assertThat(result.errors()).containsExactly("primary unavailable", "fallback unavailable");
    }

    @Test
    void skipsMissingPrimaryFallbackChildren() {
        AtomicInteger calls = new AtomicInteger();

        SkillStoreConfigValidationResult result = SkillStoreFactorySupport.validatePrimaryFallback(
                null,
                "fallback",
                name -> {
                    calls.incrementAndGet();
                    return SkillStoreConfigValidationResult.valid();
                });

        assertThat(result.validConfiguration()).isTrue();
        assertThat(calls).hasValue(1);
    }

    @Test
    void createsPrimaryFallbackChildrenInOrder() {
        List<String> created = new ArrayList<>();

        SkillStoreFactorySupport.PrimaryFallback<String> result = SkillStoreFactorySupport.createPrimaryFallback(
                "primary",
                "fallback",
                name -> {
                    created.add(name);
                    return name + "-store";
                });

        assertThat(result.primary()).isEqualTo("primary-store");
        assertThat(result.fallback()).isEqualTo("fallback-store");
        assertThat(created).containsExactly("primary", "fallback");
    }

    @Test
    void composesPrimaryFallbackChildrenInOrder() {
        List<String> created = new ArrayList<>();

        String result = SkillStoreFactorySupport.createPrimaryFallback(
                "primary",
                "fallback",
                name -> {
                    created.add(name);
                    return name + "-store";
                },
                (primary, fallback) -> primary + " -> " + fallback);

        assertThat(result).isEqualTo("primary-store -> fallback-store");
        assertThat(created).containsExactly("primary", "fallback");
    }
}
