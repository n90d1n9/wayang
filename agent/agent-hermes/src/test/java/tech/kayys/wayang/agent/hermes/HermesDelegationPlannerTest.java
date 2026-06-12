package tech.kayys.wayang.agent.hermes;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.spi.AgentRequest;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HermesDelegationPlannerTest {

    @Test
    void resolvesExplicitDelegationAndClampsToConfiguredLimit() {
        HermesDelegationPlanner planner = new HermesDelegationPlanner(HermesAgentModeConfig.builder()
                .maxSubAgents(3)
                .build());

        HermesDelegationPlan plan = planner.plan(AgentRequest.builder()
                .prompt("Ship the release")
                .parameter("subAgentCount", 5)
                .parameter("lanes", "research,implementation,verification,documentation")
                .build());

        assertThat(plan.delegationEnabled()).isTrue();
        assertThat(plan.requested()).isTrue();
        assertThat(plan.delegated()).isTrue();
        assertThat(plan.active()).isTrue();
        assertThat(plan.suggestedSubAgents()).isEqualTo(3);
        assertThat(plan.maxSubAgents()).isEqualTo(3);
        assertThat(plan.lanes()).containsExactly("research", "implementation", "verification");
        assertThat(plan.source()).isEqualTo("explicit");
        assertThat(plan.reason()).contains("clamped");
    }

    @Test
    void infersDelegationFromParallelWorkstreamPrompt() {
        HermesDelegationPlanner planner = new HermesDelegationPlanner(HermesAgentModeConfig.defaults());

        HermesDelegationPlan plan = planner.plan(AgentRequest.builder()
                .prompt("Research, implement, and test this large workstream in parallel")
                .build());

        assertThat(plan.requested()).isTrue();
        assertThat(plan.delegated()).isTrue();
        assertThat(plan.suggestedSubAgents()).isEqualTo(3);
        assertThat(plan.lanes()).containsExactly("research", "implementation", "verification");
        assertThat(plan.source()).isEqualTo("prompt");
        assertThat(plan.reason()).isEqualTo("parallel workstream inferred from prompt");
    }

    @Test
    void keepsDelegationInactiveWhenSubAgentsAreDisabled() {
        HermesDelegationPlanner planner = new HermesDelegationPlanner(HermesAgentModeConfig.builder()
                .subAgentsEnabled(false)
                .build());

        HermesDelegationPlan plan = planner.plan(AgentRequest.builder()
                .prompt("Run this in parallel")
                .parameter("delegate", true)
                .build());

        assertThat(plan.delegationEnabled()).isFalse();
        assertThat(plan.requested()).isTrue();
        assertThat(plan.delegated()).isFalse();
        assertThat(plan.active()).isFalse();
        assertThat(plan.source()).isEqualTo("disabled");
        assertThat(plan.reason()).isEqualTo("sub-agent delegation disabled");
    }

    @Test
    void respectsRequestLevelDelegationOptOut() {
        HermesDelegationPlanner planner = new HermesDelegationPlanner(HermesAgentModeConfig.defaults());

        HermesDelegationPlan plan = planner.plan(AgentRequest.builder()
                .prompt("Run this large workstream in parallel")
                .parameter("delegate", false)
                .build());

        assertThat(plan.delegationEnabled()).isTrue();
        assertThat(plan.requested()).isFalse();
        assertThat(plan.delegated()).isFalse();
        assertThat(plan.source()).isEqualTo("explicit");
        assertThat(plan.reason()).isEqualTo("delegation disabled for request");
    }

    @Test
    void reportsNoDelegationForSimpleRequests() {
        HermesDelegationPlanner planner = new HermesDelegationPlanner(HermesAgentModeConfig.defaults());

        HermesDelegationPlan plan = planner.plan(AgentRequest.builder()
                .prompt("Prepare a release report")
                .build());

        assertThat(plan.delegationEnabled()).isTrue();
        assertThat(plan.requested()).isFalse();
        assertThat(plan.delegated()).isFalse();
        assertThat(plan.suggestedSubAgents()).isZero();
        assertThat(plan.lanes()).isEmpty();
        assertThat(plan.source()).isEqualTo("none");
        assertThat(plan.reason()).isEqualTo("no delegation requested");
    }

    @Test
    void acceptsIterableLaneHints() {
        HermesDelegationPlanner planner = new HermesDelegationPlanner(HermesAgentModeConfig.defaults());

        HermesDelegationPlan plan = planner.plan(AgentRequest.builder()
                .parameters(Map.of("delegation.lanes", List.of("research", "review")))
                .build());

        assertThat(plan.delegated()).isTrue();
        assertThat(plan.suggestedSubAgents()).isEqualTo(2);
        assertThat(plan.lanes()).containsExactly("research", "review");
    }

    @Test
    void rejectsInvalidDelegationHints() {
        HermesDelegationPlanner planner = new HermesDelegationPlanner(HermesAgentModeConfig.defaults());

        assertThatThrownBy(() -> planner.plan(AgentRequest.builder()
                .parameter("parallelism", "many")
                .build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("delegation integer");
        assertThatThrownBy(() -> planner.plan(AgentRequest.builder()
                .parameter("delegate", "maybe")
                .build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("delegation boolean");
    }
}
