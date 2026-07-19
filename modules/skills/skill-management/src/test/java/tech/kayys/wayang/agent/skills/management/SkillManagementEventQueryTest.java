package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static tech.kayys.wayang.agent.skills.management.SkillManagementEventAttributeKeys.OPERATION_ID;
import static tech.kayys.wayang.agent.skills.management.SkillManagementEventAttributeKeys.PARENT_OPERATION_ID;

class SkillManagementEventQueryTest {

    @Test
    void normalizesFiltersAndLimits() {
        SkillManagementEventQuery query = new SkillManagementEventQuery(
                SkillManagementEventOperation.CREATE_SKILL,
                " planner ",
                Boolean.TRUE,
                " create-1 ",
                " bootstrap-1 ",
                SkillManagementQueryLimits.MAX_LIMIT + 1);

        assertThat(query.skillId()).isEqualTo("planner");
        assertThat(query.operationId()).isEqualTo("create-1");
        assertThat(query.parentOperationId()).isEqualTo("bootstrap-1");
        assertThat(query.limit()).isEqualTo(SkillManagementQueryLimits.MAX_LIMIT);
    }

    @Test
    void matchesOperationSkillAndSuccessFilters() {
        SkillManagementEvent event = event(
                SkillManagementEventOperation.CREATE_SKILL,
                "planner",
                true,
                Map.of());

        assertThat(SkillManagementEventQuery.forOperation(SkillManagementEventOperation.CREATE_SKILL, 10)
                .matches(event)).isTrue();
        assertThat(SkillManagementEventQuery.forOperation(SkillManagementEventOperation.DELETE_SKILL, 10)
                .matches(event)).isFalse();
        assertThat(SkillManagementEventQuery.forSkill("planner", 10).matches(event)).isTrue();
        assertThat(SkillManagementEventQuery.forSkill("writer", 10).matches(event)).isFalse();
        assertThat(new SkillManagementEventQuery(null, "", Boolean.TRUE, 10).matches(event)).isTrue();
        assertThat(SkillManagementEventQuery.failures(10).matches(event)).isFalse();
    }

    @Test
    void matchesOperationCorrelationFilters() {
        SkillManagementEvent root = event(
                SkillManagementEventOperation.DEPLOYMENT,
                "",
                true,
                Map.of(OPERATION_ID, "deployment-1"));
        SkillManagementEvent child = event(
                SkillManagementEventOperation.MAINTENANCE,
                "",
                true,
                Map.of(
                        OPERATION_ID, "maintenance-1",
                        PARENT_OPERATION_ID, "deployment-1"));

        assertThat(SkillManagementEventQuery.forOperationId(" deployment-1 ", 10).matches(root))
                .isTrue();
        assertThat(SkillManagementEventQuery.forOperationId("deployment-1", 10).matches(child))
                .isFalse();
        assertThat(SkillManagementEventQuery.forParentOperationId(" deployment-1 ", 10).matches(child))
                .isTrue();
        assertThat(SkillManagementEventQuery.forParentOperationId("deployment-2", 10).matches(child))
                .isFalse();
    }

    @Test
    void neverMatchesNullEvents() {
        assertThat(SkillManagementEventQuery.latest().matches(null)).isFalse();
    }

    private static SkillManagementEvent event(
            SkillManagementEventOperation operation,
            String skillId,
            boolean success,
            Map<String, String> attributes) {
        return new SkillManagementEvent(
                Instant.parse("2026-01-01T00:00:00Z"),
                operation,
                skillId,
                success,
                attributes);
    }
}
