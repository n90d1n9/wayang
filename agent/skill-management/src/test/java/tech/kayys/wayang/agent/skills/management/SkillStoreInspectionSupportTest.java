package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class SkillStoreInspectionSupportTest {

    @Test
    void requiresInspectionTargets() {
        assertThat(SkillStoreInspectionSupport.require("store", "store")).isEqualTo("store");
        assertThatNullPointerException()
                .isThrownBy(() -> SkillStoreInspectionSupport.require(null, "store"))
                .withMessage("store");
    }

    @Test
    void filtersAndSortsNonBlankSkillIds() {
        assertThat(SkillStoreInspectionSupport.sortedNonBlankIds(Stream.of(
                "writer",
                "",
                null,
                "planner",
                "  ")))
                .containsExactly("planner", "writer");
    }

    @Test
    void normalizesInspectionValues() {
        assertThat(SkillStoreInspectionSupport.count(-2)).isZero();
        assertThat(SkillStoreInspectionSupport.countAtLeast(-2, 3)).isEqualTo(3);
        assertThat(SkillStoreInspectionSupport.text(null)).isEmpty();
        assertThat(SkillStoreInspectionSupport.ids(java.util.Arrays.asList("writer", "", null, "planner")))
                .containsExactly("writer", "planner");
        assertThat(SkillStoreInspectionSupport.children(java.util.Arrays.asList("primary", null, "fallback")))
                .containsExactly("primary", "fallback");
        assertThat(SkillStoreInspectionSupport.counts(Map.of("enabled", -1, "disabled", 2)))
                .containsEntry("enabled", 0)
                .containsEntry("disabled", 2);
        assertThat(SkillStoreInspectionSupport.lifecycleStatusCounts(Map.of(
                SkillLifecycleStatus.ACTIVE,
                2,
                SkillLifecycleStatus.DISABLED,
                -1)))
                .containsEntry(SkillLifecycleStatus.ACTIVE, 2)
                .doesNotContainKey(SkillLifecycleStatus.DISABLED);
    }

    @Test
    void inspectionRecordsUseSharedNormalizationRules() {
        SkillDefinitionStoreInspection definitionChild = SkillDefinitionStoreInspection.unavailable(
                "definition-child",
                "memory",
                "",
                List.of());
        SkillDefinitionStoreInspection definition = new SkillDefinitionStoreInspection(
                "definitions",
                "memory",
                SkillDefinitionStoreHealthStatus.READY,
                -1,
                java.util.Arrays.asList("writer", "", null, "planner"),
                null,
                java.util.Arrays.asList(definitionChild, null),
                null);

        assertThat(definition.skillCount()).isEqualTo(2);
        assertThat(definition.skillIds()).containsExactly("writer", "planner");
        assertThat(definition.failure()).isEmpty();
        assertThat(definition.children()).containsExactly(definitionChild);
        assertThat(definition.capabilities().names()).isEmpty();

        SkillArtifactStoreInspection artifact = new SkillArtifactStoreInspection(
                "artifacts",
                "memory",
                SkillArtifactStoreHealthStatus.READY,
                -1,
                java.util.Arrays.asList("planner:package:package:v1", "", null),
                Map.of("package", -1, "rag-index", 2),
                null,
                java.util.Collections.singletonList(null),
                null);
        assertThat(artifact.artifactCount()).isEqualTo(1);
        assertThat(artifact.artifactReferences()).containsExactly("planner:package:package:v1");
        assertThat(artifact.kindCounts())
                .containsEntry("package", 0)
                .containsEntry("rag-index", 2);

        SkillLifecycleStateStoreInspection lifecycle = new SkillLifecycleStateStoreInspection(
                "lifecycle",
                "memory",
                SkillLifecycleStateStoreHealthStatus.READY,
                -1,
                java.util.Arrays.asList("planner", null, "writer"),
                Map.of(SkillLifecycleStatus.ACTIVE, 2, SkillLifecycleStatus.DISABLED, -1),
                null);
        assertThat(lifecycle.stateCount()).isEqualTo(2);
        assertThat(lifecycle.skillIds()).containsExactly("planner", "writer");
        assertThat(lifecycle.statusCounts()).containsEntry(SkillLifecycleStatus.ACTIVE, 2);
        assertThat(lifecycle.statusCounts()).doesNotContainKey(SkillLifecycleStatus.DISABLED);

        SkillManagementEventStoreInspection events = new SkillManagementEventStoreInspection(
                "events",
                "memory",
                SkillManagementEventStoreHealthStatus.READY,
                -1,
                2,
                false,
                null,
                null,
                java.util.Collections.singletonList(null),
                null);
        assertThat(events.matchedEvents()).isEqualTo(2);
        assertThat(events.returnedEvents()).isEqualTo(2);
        assertThat(events.truncated()).isFalse();
        assertThat(events.summary().totalEvents()).isZero();
    }

    @Test
    void buildsPrimaryFallbackChildren() {
        List<String> children = SkillStoreInspectionSupport.primaryFallbackChildren(
                "a",
                "b",
                (name, value) -> name + ":" + value);

        assertThat(children).containsExactly("primary:a", "fallback:b");
    }

    @Test
    void namesIndexedChildren() {
        assertThat(SkillStoreInspectionSupport.indexedChildName(0, 2, "sink")).isEqualTo("primary");
        assertThat(SkillStoreInspectionSupport.indexedChildName(1, 2, "sink")).isEqualTo("fallback");
        assertThat(SkillStoreInspectionSupport.indexedChildName(2, 3, "sink")).isEqualTo("sink-3");
    }
}
