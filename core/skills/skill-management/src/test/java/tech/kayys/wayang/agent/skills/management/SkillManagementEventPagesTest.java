package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static tech.kayys.wayang.agent.skills.management.SkillManagementEventAttributeKeys.OPERATION_ID;
import static tech.kayys.wayang.agent.skills.management.SkillManagementEventAttributeKeys.PARENT_OPERATION_ID;

class SkillManagementEventPagesTest {

    @Test
    void buildsLatestMatchingWindow() {
        List<SkillManagementEvent> events = List.of(
                event("2026-01-01T00:00:00Z", SkillManagementEventOperation.CREATE_SKILL, "planner", true),
                event("2026-01-01T00:00:01Z", SkillManagementEventOperation.UPDATE_SKILL, "planner", true),
                event("2026-01-01T00:00:02Z", SkillManagementEventOperation.DELETE_SKILL, "writer", false),
                event("2026-01-01T00:00:03Z", SkillManagementEventOperation.DELETE_SKILL, "planner", false));

        SkillManagementEventPage page = SkillManagementEventPages.from(
                events,
                SkillManagementEventQuery.forSkill("planner", 2));

        assertThat(page.matchedEvents()).isEqualTo(3);
        assertThat(page.events()).extracting(SkillManagementEvent::operation)
                .containsExactly(
                        SkillManagementEventOperation.UPDATE_SKILL,
                        SkillManagementEventOperation.DELETE_SKILL);
        assertThat(page.truncated()).isTrue();
    }

    @Test
    void defaultsNullQueryToLatestWindow() {
        SkillManagementEventPage page = SkillManagementEventPages.from(
                List.of(event(
                        "2026-01-01T00:00:00Z",
                        SkillManagementEventOperation.CREATE_SKILL,
                        "planner",
                        true)),
                null);

        assertThat(page.returnedEvents()).isEqualTo(1);
        assertThat(page.matchedEvents()).isEqualTo(1);
    }

    @Test
    void handlesNullEventsAsEmptyHistory() {
        SkillManagementEventPage page = SkillManagementEventPages.from(null, SkillManagementEventQuery.latest());

        assertThat(page.events()).isEmpty();
        assertThat(page.matchedEvents()).isZero();
    }

    @Test
    void filtersNullEventsBeforeBuildingWindowsAndSummaries() {
        SkillManagementEventPage page = SkillManagementEventPages.from(
                java.util.Arrays.asList(
                        event(
                                "2026-01-01T00:00:00Z",
                                SkillManagementEventOperation.CREATE_SKILL,
                                "planner",
                                true),
                        null,
                        event(
                                "2026-01-01T00:00:01Z",
                                SkillManagementEventOperation.DELETE_SKILL,
                                "planner",
                                false)),
                SkillManagementEventQuery.latest());

        assertThat(page.events()).hasSize(2);
        assertThat(page.matchedEvents()).isEqualTo(2);
        assertThat(page.summary().totalEvents()).isEqualTo(2);
        assertThat(page.summary().successfulEvents()).isEqualTo(1);
        assertThat(page.summary().failedEvents()).isEqualTo(1);
        assertThat(page.summary().skillCounts()).containsEntry("planner", 2);
    }

    @Test
    void filtersByOperationCorrelationAttributes() {
        List<SkillManagementEvent> events = List.of(
                event(
                        "2026-01-01T00:00:00Z",
                        SkillManagementEventOperation.DEPLOYMENT,
                        "",
                        true,
                        Map.of(OPERATION_ID, "deployment-1")),
                event(
                        "2026-01-01T00:00:01Z",
                        SkillManagementEventOperation.MAINTENANCE,
                        "",
                        true,
                        Map.of(
                                OPERATION_ID, "maintenance-1",
                                PARENT_OPERATION_ID, "deployment-1")),
                event(
                        "2026-01-01T00:00:02Z",
                        SkillManagementEventOperation.MAINTENANCE,
                        "",
                        true,
                        Map.of(OPERATION_ID, "maintenance-2")));

        SkillManagementEventPage root = SkillManagementEventPages.from(
                events,
                SkillManagementEventQuery.forOperationId(" deployment-1 ", 10));
        SkillManagementEventPage children = SkillManagementEventPages.from(
                events,
                SkillManagementEventQuery.forParentOperationId("deployment-1", 10));

        assertThat(root.matchedEvents()).isEqualTo(1);
        assertThat(root.events()).extracting(SkillManagementEvent::operation)
                .containsExactly(SkillManagementEventOperation.DEPLOYMENT);
        assertThat(children.matchedEvents()).isEqualTo(1);
        assertThat(children.events()).extracting(SkillManagementEvent::operation)
                .containsExactly(SkillManagementEventOperation.MAINTENANCE);
    }

    @Test
    void eventSummaryNormalizesNegativeCountsAndCountMaps() {
        SkillManagementEventSummary summary = new SkillManagementEventSummary(
                -1,
                -2,
                -3,
                Map.of(
                        "CREATE_SKILL",
                        -4,
                        "DELETE_SKILL",
                        2),
                Map.of(
                        "planner",
                        -5,
                        "writer",
                        1));

        assertThat(summary.totalEvents()).isZero();
        assertThat(summary.successfulEvents()).isZero();
        assertThat(summary.failedEvents()).isZero();
        assertThat(summary.operationCounts())
                .containsEntry("CREATE_SKILL", 0)
                .containsEntry("DELETE_SKILL", 2);
        assertThat(summary.skillCounts())
                .containsEntry("planner", 0)
                .containsEntry("writer", 1);
    }

    @Test
    void treatsNegativeWindowLimitAsEmptyWindow() {
        List<SkillManagementEvent> window = SkillManagementEventPages.latestWindow(
                List.of(event(
                        "2026-01-01T00:00:00Z",
                        SkillManagementEventOperation.CREATE_SKILL,
                        "planner",
                        true)),
                -1);

        assertThat(window).isEmpty();
    }

    private SkillManagementEvent event(
            String occurredAt,
            SkillManagementEventOperation operation,
            String skillId,
            boolean success) {
        return event(occurredAt, operation, skillId, success, Map.of());
    }

    private SkillManagementEvent event(
            String occurredAt,
            SkillManagementEventOperation operation,
            String skillId,
            boolean success,
            Map<String, String> attributes) {
        return new SkillManagementEvent(
                Instant.parse(occurredAt),
                operation,
                skillId,
                success,
                attributes);
    }
}
