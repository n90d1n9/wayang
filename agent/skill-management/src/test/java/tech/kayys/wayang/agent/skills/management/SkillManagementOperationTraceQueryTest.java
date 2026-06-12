package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class SkillManagementOperationTraceQueryTest {

    @Test
    void normalizesLimitsAndExposesStatusFilter() {
        SkillManagementOperationTraceQuery query =
                SkillManagementOperationTraceQuery.deploymentsByStatus(
                        -1,
                        SkillManagementEventQuery.MAX_LIMIT + 1,
                        SkillManagementOperationTraceStatus.FAILED);

        assertThat(query.operationLimit()).isEqualTo(SkillManagementEventQuery.DEFAULT_LIMIT);
        assertThat(query.childEventLimit()).isEqualTo(SkillManagementEventQuery.MAX_LIMIT);
        assertThat(query.status()).isEqualTo(SkillManagementOperationTraceStatus.FAILED);
        assertThat(query.statusFilter()).isEqualTo(SkillManagementOperationTraceStatus.FAILED.name());
    }

    @Test
    void leavesStatusFilterEmptyWhenAllStatusesAreAllowed() {
        SkillManagementOperationTraceQuery query =
                SkillManagementOperationTraceQuery.deployments(10, 10);

        assertThat(query.status()).isNull();
        assertThat(query.statusFilter()).isEmpty();
    }

    @Test
    void acceptsApiFriendlyStatusNames() {
        SkillManagementOperationTraceQuery query =
                SkillManagementOperationTraceQuery.deploymentsByStatusName(
                        10,
                        10,
                        "root-missing");

        assertThat(query.status()).isEqualTo(SkillManagementOperationTraceStatus.ROOT_MISSING);
        assertThat(query.statusFilter()).isEqualTo(SkillManagementOperationTraceStatus.ROOT_MISSING.name());
    }

    @Test
    void treatsBlankStatusNameAsNoFilter() {
        SkillManagementOperationTraceQuery query =
                SkillManagementOperationTraceQuery.deploymentsByStatusName(10, 10, " ");

        assertThat(query.status()).isNull();
        assertThat(query.statusFilter()).isEmpty();
    }

    @Test
    void rejectsUnknownStatusNames() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> SkillManagementOperationTraceQuery.deploymentsByStatusName(
                        10,
                        10,
                        "stale"))
                .withMessageContaining("Unknown operation trace status: stale");
    }

    @Test
    void matchesAllStatusesWhenStatusFilterIsUnset() {
        SkillManagementOperationTraceQuery query =
                SkillManagementOperationTraceQuery.deployments(10, 10);

        assertThat(query.matches(healthyTrace())).isTrue();
        assertThat(query.matches(failedTrace())).isTrue();
        assertThat(query.matches(null)).isFalse();
    }

    @Test
    void matchesOnlyRequestedStatusWhenStatusFilterIsSet() {
        SkillManagementOperationTraceQuery query =
                SkillManagementOperationTraceQuery.deploymentsByStatus(
                        10,
                        10,
                        SkillManagementOperationTraceStatus.FAILED);

        assertThat(query.matches(failedTrace())).isTrue();
        assertThat(query.matches(healthyTrace())).isFalse();
        assertThat(query.matches(rootMissingTrace())).isFalse();
    }

    private static SkillManagementAdminOperationTrace healthyTrace() {
        return new SkillManagementAdminOperationTrace(
                "deploy-1",
                event("DEPLOYMENT", "deploy-1", "", true),
                List.of());
    }

    private static SkillManagementAdminOperationTrace failedTrace() {
        return new SkillManagementAdminOperationTrace(
                "deploy-1",
                event("DEPLOYMENT", "deploy-1", "", true),
                List.of(event("MAINTENANCE", "maintenance-1", "deploy-1", false)));
    }

    private static SkillManagementAdminOperationTrace rootMissingTrace() {
        return new SkillManagementAdminOperationTrace("deploy-1", null, List.of());
    }

    private static SkillManagementAdminEvent event(
            String operation,
            String operationId,
            String parentOperationId,
            boolean success) {
        return new SkillManagementAdminEvent(
                "2026-01-01T00:00:00Z",
                operation,
                "",
                operationId,
                parentOperationId,
                success,
                Map.of());
    }
}
