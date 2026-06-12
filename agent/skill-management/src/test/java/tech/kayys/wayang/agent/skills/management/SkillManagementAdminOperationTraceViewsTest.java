package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SkillManagementAdminOperationTraceViewsTest {

    @Test
    void mapsOperationTraceToRootAndChildAdminEvents() {
        SkillManagementEvent root = TestSkillManagementAdminFixtures.event(
                0,
                SkillManagementEventOperation.DEPLOYMENT,
                "",
                true,
                Map.of("operationId", "deploy-1"));
        SkillManagementEvent maintenance = TestSkillManagementAdminFixtures.event(
                1,
                SkillManagementEventOperation.MAINTENANCE,
                "",
                true,
                Map.of(
                        "operationId", "maintenance-1",
                        "parentOperationId", "deploy-1"));
        SkillManagementEvent pruneFailure = TestSkillManagementAdminFixtures.event(
                2,
                SkillManagementEventOperation.RECONCILE_LIFECYCLE,
                "",
                false,
                Map.of(
                        "operationId", "reconcile-1",
                        "parentOperationId", "deploy-1"));
        SkillManagementEvent unrelated = TestSkillManagementAdminFixtures.event(
                3,
                SkillManagementEventOperation.MAINTENANCE,
                "",
                true,
                Map.of(
                        "operationId", "maintenance-2",
                        "parentOperationId", "deploy-2"));

        SkillManagementAdminOperationTrace view = SkillManagementAdminOperationTraceViews.operationTrace(
                " deploy-1 ",
                TestSkillManagementAdminFixtures.eventPage(4, root, maintenance, pruneFailure, unrelated));

        assertThat(view.operationId()).isEqualTo("deploy-1");
        assertThat(view.rootEventAvailable()).isTrue();
        assertThat(view.totalEvents()).isEqualTo(3);
        assertThat(view.successfulEvents()).isEqualTo(2);
        assertThat(view.failedEvents()).isEqualTo(1);
        assertThat(view.childEventCount()).isEqualTo(2);
        assertThat(view.healthy()).isFalse();
        assertThat(view.failed()).isTrue();
        assertThat(view.failedChildEvents()).isEqualTo(1);
        assertThat(view.status()).isEqualTo(SkillManagementOperationTraceStatus.FAILED.name());
        assertThat(view.summary().operationCounts())
                .containsEntry("DEPLOYMENT", 1)
                .containsEntry("MAINTENANCE", 1)
                .containsEntry("RECONCILE_LIFECYCLE", 1);
        assertThat(view.summary().failedEvents()).isEqualTo(1);
        assertThat(view.rootEvent().operation()).isEqualTo("DEPLOYMENT");
        assertThat(view.childEvents()).extracting(SkillManagementAdminEvent::operation)
                .containsExactly("MAINTENANCE", "RECONCILE_LIFECYCLE");
    }

    @Test
    void mapsOperationTraceFromRootAndChildEventPages() {
        SkillManagementEvent root = TestSkillManagementAdminFixtures.event(
                0,
                SkillManagementEventOperation.DEPLOYMENT,
                "",
                true,
                Map.of("operationId", "deploy-1"));
        SkillManagementEvent child = TestSkillManagementAdminFixtures.event(
                1,
                SkillManagementEventOperation.MAINTENANCE,
                "",
                false,
                Map.of(
                        "operationId", "maintenance-1",
                        "parentOperationId", "deploy-1"));
        SkillManagementEvent unrelatedChild = TestSkillManagementAdminFixtures.event(
                2,
                SkillManagementEventOperation.MAINTENANCE,
                "",
                true,
                Map.of(
                        "operationId", "maintenance-2",
                        "parentOperationId", "deploy-2"));

        SkillManagementAdminOperationTrace view = SkillManagementAdminOperationTraceViews.operationTrace(
                "deploy-1",
                TestSkillManagementAdminFixtures.eventPage(1, root),
                TestSkillManagementAdminFixtures.eventPage(2, child, unrelatedChild));

        assertThat(view.rootEventAvailable()).isTrue();
        assertThat(view.totalEvents()).isEqualTo(2);
        assertThat(view.successfulEvents()).isEqualTo(1);
        assertThat(view.failedEvents()).isEqualTo(1);
        assertThat(view.healthy()).isFalse();
        assertThat(view.failed()).isTrue();
        assertThat(view.failedChildEvents()).isEqualTo(1);
        assertThat(view.status()).isEqualTo(SkillManagementOperationTraceStatus.FAILED.name());
        assertThat(view.summary().operationCounts())
                .containsEntry("DEPLOYMENT", 1)
                .containsEntry("MAINTENANCE", 1);
        assertThat(view.childEvents()).extracting(SkillManagementAdminEvent::operationId)
                .containsExactly("maintenance-1");
    }
}
