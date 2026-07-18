package tech.kayys.wayang.agent.skills.audit;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SkillAuditServiceTest {

    @Test
    void recordsAndFiltersAuditEventsByLevel() {
        SkillAuditService service = new SkillAuditService();

        service.setAuditLevel(SkillAuditLevel.SECURITY);
        service.logSuccess(SkillAuditEventType.SKILL_CREATED, "user-1", "planner", "create")
                .await().indefinitely();
        service.logFailure(SkillAuditEventType.VALIDATION_FAILED, "user-1", "planner", "validate", "bad prompt")
                .await().indefinitely();
        service.logAccessDenied("user-2", "planner", "missing permission").await().indefinitely();

        assertThat(service.size()).isEqualTo(2);
        assertThat(service.eventsForSkill("planner").await().indefinitely())
                .extracting(SkillAuditEvent::eventType)
                .containsExactly(SkillAuditEventType.VALIDATION_FAILED, SkillAuditEventType.ACCESS_DENIED);
        assertThat(service.eventsForUser("user-2").await().indefinitely())
                .singleElement()
                .extracting(SkillAuditEvent::status)
                .isEqualTo(SkillAuditStatus.DENIED);
    }

    @Test
    void latestEventsReturnsNewestFirst() {
        SkillAuditService service = new SkillAuditService();

        service.logSuccess(SkillAuditEventType.SKILL_CREATED, "user-1", "one", "create").await().indefinitely();
        service.logSuccess(SkillAuditEventType.SKILL_UPDATED, "user-1", "two", "update").await().indefinitely();

        assertThat(service.latestEvents(1).await().indefinitely())
                .singleElement()
                .extracting(SkillAuditEvent::skillId)
                .isEqualTo("two");
    }
}
