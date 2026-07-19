package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SkillManagementLifecycleEventAttributesTest {

    @Test
    void transitionAndRevisionProjectLifecycleState() {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        SkillLifecycleState state = new SkillLifecycleState(
                "planner",
                SkillLifecycleStatus.DISABLED,
                now,
                now,
                7);

        assertThat(SkillManagementLifecycleEventAttributes.transition(SkillLifecycleStatus.DEPRECATED))
                .containsOnly(Map.entry("status", "DEPRECATED"));
        assertThat(SkillManagementLifecycleEventAttributes.transition(state))
                .containsEntry("status", "DISABLED")
                .containsEntry("revision", "7");
        assertThat(SkillManagementLifecycleEventAttributes.revision(state))
                .containsOnly(Map.entry("revision", "7"));
    }

    @Test
    void reconcileProjectsLifecycleRepairCounts() {
        SkillLifecycleStateReconcileResult result = new SkillLifecycleStateReconcileResult(
                List.of("planner"),
                List.of("legacy"),
                List.of("planner"),
                List.of("legacy"),
                List.of("planner"),
                List.of());

        Map<String, String> attributes = SkillManagementLifecycleEventAttributes.reconcile(result);

        assertThat(attributes)
                .containsEntry("consistent", "false")
                .containsEntry("created", "1")
                .containsEntry("removed", "0")
                .containsEntry("missing", "1")
                .containsEntry("orphaned", "1");
        assertThatThrownBy(() -> attributes.put("extra", "value"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void bootstrapProjectsReadinessSummary() {
        SkillLifecycleStateReconcileResult reconciliation = TestSkillManagementAdminFixtures.reconciliation();
        SkillManagementBootstrapResult result = new SkillManagementBootstrapResult(
                TestSkillManagementAdminFixtures.service(),
                SkillManagementServiceConfig.defaults(),
                TestSkillManagementAdminFixtures.inspection(reconciliation),
                reconciliation,
                TestSkillManagementAdminFixtures.inspection(reconciliation));

        assertThat(SkillManagementLifecycleEventAttributes.bootstrap(result))
                .containsEntry("ready", "true")
                .containsEntry("changed", "true")
                .containsEntry("lifecycleConsistent", "true");
    }
}
