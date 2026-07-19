package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SkillManagementAdminEventViewsTest {

    @Test
    void mapsEventPageToStableAdminProjection() {
        SkillManagementEvent event = TestSkillManagementAdminFixtures.event(
                0,
                SkillManagementEventOperation.CREATE_SKILL,
                "planner",
                true,
                Map.ofEntries(
                        Map.entry("operationId", " create-1 "),
                        Map.entry("parentOperationId", " bootstrap-1 "),
                        Map.entry("revision", "1")));
        SkillManagementEventPage page = TestSkillManagementAdminFixtures.eventPage(3, event);

        SkillManagementAdminEventPage view = SkillManagementAdminEventViews.eventPage(page);

        assertThat(view.matchedEvents()).isEqualTo(3);
        assertThat(view.returnedEvents()).isEqualTo(1);
        assertThat(view.truncated()).isTrue();
        assertThat(view.summary().totalEvents()).isEqualTo(1);
        assertThat(view.summary().operationCounts()).containsEntry("CREATE_SKILL", 1);
        assertThat(view.summary().skillCounts()).containsEntry("planner", 1);
        assertThat(view.events()).hasSize(1);
        assertThat(view.events().get(0).occurredAt()).isEqualTo("2026-01-01T00:00:00Z");
        assertThat(view.events().get(0).operation()).isEqualTo("CREATE_SKILL");
        assertThat(view.events().get(0).operationId()).isEqualTo("create-1");
        assertThat(view.events().get(0).parentOperationId()).isEqualTo("bootstrap-1");
        assertThat(view.events().get(0).attributes()).containsEntry("revision", "1");
        assertThat(view.events().get(0).attributes()).containsEntry("operationId", " create-1 ");
    }
}
