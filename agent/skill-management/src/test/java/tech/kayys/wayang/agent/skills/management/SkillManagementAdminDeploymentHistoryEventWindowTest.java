package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SkillManagementAdminDeploymentHistoryEventWindowTest {

    @Test
    void preservesDeploymentOnlyPageWindowMetadata() {
        SkillManagementEvent first = deployment("deploy-1");
        SkillManagementEvent second = deployment("deploy-2");
        SkillManagementEventPage page = TestSkillManagementAdminFixtures.eventPage(5, first, second);

        SkillManagementAdminDeploymentHistoryEventWindow window =
                SkillManagementAdminDeploymentHistoryEventWindow.from(page);

        assertThat(window.matchedDeployments()).isEqualTo(5);
        assertThat(window.truncated()).isTrue();
        assertThat(window.deploymentEvents()).containsExactly(first, second);
    }

    @Test
    void treatsMixedEventPagesAsReturnedDeploymentSlices() {
        SkillManagementEvent deployment = deployment("deploy-1");
        SkillManagementEvent maintenance = TestSkillManagementAdminFixtures.event(
                1,
                SkillManagementEventOperation.MAINTENANCE,
                "",
                true,
                Map.of("operationId", "maintenance-1"));
        SkillManagementEventPage page = TestSkillManagementAdminFixtures.eventPage(10, deployment, maintenance);

        SkillManagementAdminDeploymentHistoryEventWindow window =
                SkillManagementAdminDeploymentHistoryEventWindow.from(page);

        assertThat(window.matchedDeployments()).isEqualTo(1);
        assertThat(window.truncated()).isFalse();
        assertThat(window.deploymentEvents()).containsExactly(deployment);
    }

    @Test
    void normalizesExplicitWindowsToDeploymentEvents() {
        SkillManagementEvent deployment = deployment("deploy-1");
        SkillManagementEvent maintenance = TestSkillManagementAdminFixtures.event(
                1,
                SkillManagementEventOperation.MAINTENANCE,
                "",
                true,
                Map.of("operationId", "maintenance-1"));

        SkillManagementAdminDeploymentHistoryEventWindow window =
                new SkillManagementAdminDeploymentHistoryEventWindow(
                        -1,
                        false,
                        Arrays.asList(null, deployment, maintenance));

        assertThat(window.matchedDeployments()).isEqualTo(1);
        assertThat(window.truncated()).isFalse();
        assertThat(window.deploymentEvents()).containsExactly(deployment);
    }

    private static SkillManagementEvent deployment(String operationId) {
        return TestSkillManagementAdminFixtures.event(
                0,
                SkillManagementEventOperation.DEPLOYMENT,
                "",
                true,
                Map.of("operationId", operationId));
    }
}
