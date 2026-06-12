package tech.kayys.wayang.agent.skills.analytics;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UsageAnalyticsServiceTest {

    @Test
    void aggregatesSkillUsageMetrics() {
        UsageAnalyticsService service = new UsageAnalyticsService();

        service.recordExecution("planner", 100, true).await().indefinitely();
        service.recordExecution("planner", 300, false).await().indefinitely();
        service.recordExecution("coder", 50, true).await().indefinitely();

        SkillUsageMetrics planner = service.getSkillMetrics("planner").await().indefinitely();

        assertThat(planner.totalExecutions()).isEqualTo(2);
        assertThat(planner.successfulExecutions()).isEqualTo(1);
        assertThat(planner.failedExecutions()).isEqualTo(1);
        assertThat(planner.averageDurationMs()).isEqualTo(200.0);
        assertThat(planner.failureRate()).isEqualTo(0.5);

        assertThat(service.popularSkills(1).await().indefinitely())
                .singleElement()
                .extracting(SkillUsageMetrics::skillId)
                .isEqualTo("planner");
        assertThat(service.highestFailureRateSkills(1).await().indefinitely())
                .singleElement()
                .extracting(SkillUsageMetrics::skillId)
                .isEqualTo("planner");
    }
}
