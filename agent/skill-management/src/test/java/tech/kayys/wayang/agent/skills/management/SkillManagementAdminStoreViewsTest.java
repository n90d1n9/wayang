package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SkillManagementAdminStoreViewsTest {

    @Test
    void mapsHybridLifecycleStoreChildrenToStableAdminProjection() {
        InMemorySkillLifecycleStateStore primary = new InMemorySkillLifecycleStateStore();
        InMemorySkillLifecycleStateStore fallback = new InMemorySkillLifecycleStateStore();
        primary.save(SkillLifecycleState.created("planner"));
        fallback.save(SkillLifecycleState.created("writer").withStatus(SkillLifecycleStatus.DISABLED));
        SkillLifecycleStateStoreInspection inspection = new SkillLifecycleStateStoreInspector().inspect(
                "hybrid",
                new HybridSkillLifecycleStateStore(primary, fallback));

        SkillManagementAdminStoreStatus view = SkillManagementAdminStoreViews.lifecycleStore(inspection);

        assertThat(view.children()).hasSize(2);
        assertThat(view.children().get(0).name()).isEqualTo("primary");
        assertThat(view.children().get(0).itemIds()).containsExactly("planner");
        assertThat(view.children().get(1).name()).isEqualTo("fallback");
        assertThat(view.children().get(1).itemIds()).containsExactly("writer");
    }

    @Test
    void mapsLifecycleStoreChildrenAndStatusCounts() {
        SkillLifecycleStateStoreInspection primary = SkillLifecycleStateStoreInspection.ready(
                "primary",
                "memory",
                List.of("planner"),
                Map.of(SkillLifecycleStatus.ACTIVE, 1),
                List.of());
        SkillLifecycleStateStoreInspection fallback = SkillLifecycleStateStoreInspection.ready(
                "fallback",
                "memory",
                List.of("writer"),
                Map.of(SkillLifecycleStatus.DISABLED, 1),
                List.of());
        SkillLifecycleStateStoreInspection hybrid = SkillLifecycleStateStoreInspection.ready(
                "hybrid",
                "hybrid",
                List.of("planner", "writer"),
                Map.of(
                        SkillLifecycleStatus.ACTIVE, 1,
                        SkillLifecycleStatus.DISABLED, 1),
                List.of(primary, fallback));

        SkillManagementAdminStoreStatus view = SkillManagementAdminStoreViews.lifecycleStore(hybrid);

        assertThat(view.name()).isEqualTo("hybrid");
        assertThat(view.status()).isEqualTo("READY");
        assertThat(view.statusCounts()).containsEntry("ACTIVE", 1)
                .containsEntry("DISABLED", 1);
        assertThat(view.capabilities()).contains("read", "write", "delete", "list");
        assertThat(view.children()).hasSize(2);
        assertThat(view.children().get(0).itemIds()).containsExactly("planner");
        assertThat(view.children().get(1).itemIds()).containsExactly("writer");
    }

    @Test
    void mapsHybridArtifactStoreChildrenToStableAdminProjection() {
        InMemorySkillArtifactStore primary = new InMemorySkillArtifactStore();
        InMemorySkillArtifactStore fallback = new InMemorySkillArtifactStore();
        primary.putArtifact(SkillArtifact.of(SkillArtifactReference.ragIndex("planner", "kb", "v2"),
                new byte[] {2}));
        fallback.putArtifact(SkillArtifact.of(SkillArtifactReference.ragIndex("planner", "kb", "v1"),
                new byte[] {1}));
        SkillArtifactStoreInspection inspection = new SkillArtifactStoreInspector().inspect(
                "hybrid",
                new HybridSkillArtifactStore(primary, fallback));

        SkillManagementAdminStoreStatus view = SkillManagementAdminStoreViews.artifactStore(inspection);

        assertThat(view.children()).hasSize(2);
        assertThat(view.children().get(0).name()).isEqualTo("primary");
        assertThat(view.children().get(0).itemIds()).containsExactly("planner:rag-index:kb:v2");
        assertThat(view.children().get(1).name()).isEqualTo("fallback");
        assertThat(view.children().get(1).itemIds()).containsExactly("planner:rag-index:kb:v1");
    }

    @Test
    void mapsEventStoreSummaryToItemIdsAndStatusCounts() {
        SkillManagementEvent event = TestSkillManagementAdminFixtures.event(
                0,
                SkillManagementEventOperation.CREATE_SKILL,
                "planner",
                true,
                Map.of());
        SkillManagementEventPage page = TestSkillManagementAdminFixtures.eventPage(3, event);
        SkillManagementEventStoreInspection inspection = SkillManagementEventStoreInspection.ready(
                "events",
                "memory",
                page,
                List.of());

        SkillManagementAdminStoreStatus view = SkillManagementAdminStoreViews.eventStore(inspection);

        assertThat(view.name()).isEqualTo("events");
        assertThat(view.itemCount()).isEqualTo(3);
        assertThat(view.itemIds()).containsExactly("planner");
        assertThat(view.statusCounts()).containsEntry("CREATE_SKILL", 1);
        assertThat(view.capabilities()).contains("query-events");
    }

    @Test
    void adminStoreStatusNormalizesCountsAndLists() {
        SkillManagementAdminStoreStatus child = new SkillManagementAdminStoreStatus(
                "child",
                "memory",
                "READY",
                true,
                1,
                List.of("child-skill"),
                Map.of("ACTIVE", 1),
                "",
                List.of(),
                List.of("read"));

        SkillManagementAdminStoreStatus view = new SkillManagementAdminStoreStatus(
                "store",
                "hybrid",
                "",
                false,
                -7,
                java.util.Arrays.asList("writer", null, "planner", "writer"),
                Map.of("DISABLED", -2, "ACTIVE", 3),
                null,
                java.util.Arrays.asList(child, null),
                java.util.Arrays.asList("write", null, "read", "write"));

        assertThat(view.status()).isEqualTo("UNKNOWN");
        assertThat(view.itemCount()).isZero();
        assertThat(view.itemIds()).containsExactly("planner", "writer");
        assertThat(view.statusCounts())
                .containsEntry("ACTIVE", 3)
                .containsEntry("DISABLED", 0);
        assertThat(view.failure()).isEmpty();
        assertThat(view.children()).containsExactly(child);
        assertThat(view.capabilities()).containsExactly("read", "write");
    }
}
