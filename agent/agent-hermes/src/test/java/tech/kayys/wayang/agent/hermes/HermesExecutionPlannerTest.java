package tech.kayys.wayang.agent.hermes;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.spi.AgentRequest;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HermesExecutionPlannerTest {

    @Test
    void respectsConfiguredExplicitBackend() {
        HermesExecutionPlanner planner = new HermesExecutionPlanner(HermesAgentModeConfig.defaults());

        HermesExecutionPlan plan = planner.plan(AgentRequest.builder()
                .prompt("Run smoke tests")
                .parameter(HermesMetadataKeys.PARAM_EXECUTION_BACKEND, "docker")
                .build());

        assertThat(plan.backend()).isEqualTo("docker");
        assertThat(plan.requestedBackend()).isEqualTo("docker");
        assertThat(plan.explicitRequest()).isTrue();
        assertThat(plan.reason()).isEqualTo("explicit backend requested");
    }

    @Test
    void fallsBackWhenExplicitBackendIsUnavailable() {
        HermesExecutionPlanner planner = new HermesExecutionPlanner(HermesAgentModeConfig.builder()
                .executionBackends(List.of("local", "docker"))
                .build());

        HermesExecutionPlan plan = planner.plan(AgentRequest.builder()
                .prompt("Run cloud job")
                .parameter(HermesMetadataKeys.PARAM_EXECUTION_BACKEND, "modal")
                .build());

        assertThat(plan.backend()).isEqualTo("local");
        assertThat(plan.requestedBackend()).isEqualTo("modal");
        assertThat(plan.reason()).isEqualTo("requested backend unavailable; selected fallback");
    }

    @Test
    void prefersDockerForIsolatedWork() {
        HermesExecutionPlanner planner = new HermesExecutionPlanner(HermesAgentModeConfig.defaults());

        HermesExecutionPlan plan = planner.plan(AgentRequest.builder()
                .prompt("Run this untrusted installer in a sandbox")
                .build());

        assertThat(plan.backend()).isEqualTo("docker");
        assertThat(plan.isolationRequired()).isTrue();
        assertThat(plan.reason()).isEqualTo("isolated execution preferred");
    }

    @Test
    void prefersRemoteAndServerlessBackendsWhenRequested() {
        HermesExecutionPlanner planner = new HermesExecutionPlanner(HermesAgentModeConfig.defaults());

        HermesExecutionPlan remote = planner.plan(AgentRequest.builder()
                .prompt("Deploy this patch to a remote VPS over SSH")
                .build());
        HermesExecutionPlan serverless = planner.plan(AgentRequest.builder()
                .prompt("Run this batch in serverless Modal")
                .build());

        assertThat(remote.backend()).isEqualTo("ssh");
        assertThat(remote.remotePreferred()).isTrue();
        assertThat(serverless.backend()).isEqualTo("modal");
        assertThat(serverless.serverlessPreferred()).isTrue();
    }

    @Test
    void reportsNoBackendWhenNoneAreConfigured() {
        HermesExecutionPlanner planner = new HermesExecutionPlanner(HermesAgentModeConfig.builder()
                .executionBackends(List.of())
                .build());

        HermesExecutionPlan plan = planner.plan(AgentRequest.builder()
                .prompt("Run task")
                .build());

        assertThat(plan.backend()).isEqualTo("none");
        assertThat(plan.executable()).isFalse();
        assertThat(plan.toMetadata()).containsEntry("executable", false);
    }

    @Test
    void rejectsInvalidBooleanHints() {
        HermesExecutionPlanner planner = new HermesExecutionPlanner(HermesAgentModeConfig.defaults());

        assertThatThrownBy(() -> planner.plan(AgentRequest.builder()
                .prompt("Run task")
                .parameters(Map.of("sandbox", "maybe"))
                .build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("execution boolean");
    }
}
