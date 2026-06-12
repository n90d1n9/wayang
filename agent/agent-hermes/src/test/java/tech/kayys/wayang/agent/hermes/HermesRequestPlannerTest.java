package tech.kayys.wayang.agent.hermes;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.spi.AgentRequest;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HermesRequestPlannerTest {

    @Test
    void aggregatesAllHermesSidecarPlans() {
        HermesRequestPlanner planner = new HermesRequestPlanner(HermesAgentModeConfig.builder()
                .preferredProvider("ollama")
                .trajectoryExportEnabled(true)
                .maxSubAgents(3)
                .build());

        HermesRequestPlan plan = planner.plan(AgentRequest.builder()
                .prompt("""
                        Generate the API backup report every day in parallel,
                        remember this preference, export trace, and show the learned skill catalog.
                        """)
                .sessionId("session-a")
                .userId("user-a")
                .context(Map.of("platform", "slack", "channelId", "ops"))
                .parameter(HermesMetadataKeys.PARAM_EXECUTION_BACKEND, "docker")
                .parameter("lanes", "research,verification")
                .parameter("includePrompts", true)
                .build());

        assertThat(plan.executionPlan().backend()).isEqualTo("docker");
        assertThat(plan.executionDirective().active()).isTrue();
        assertThat(plan.executionDirective().operation()).isEqualTo("dispatch");
        assertThat(plan.executionDirective().backend()).isEqualTo("docker");
        assertThat(plan.executionDirective().adapterType()).isEqualTo("container");
        assertThat(plan.gatewayContext().platform()).isEqualTo("slack");
        assertThat(plan.gatewayDirective().active()).isTrue();
        assertThat(plan.gatewayDirective().operation()).isEqualTo("deliver");
        assertThat(plan.gatewayDirective().destinationType()).isEqualTo("conversation");
        assertThat(plan.gatewayDirective().destinationId()).isEqualTo("session-a");
        assertThat(plan.automationIntent().scheduled()).isTrue();
        assertThat(plan.automationDirective().active()).isTrue();
        assertThat(plan.automationDirective().operation()).isEqualTo("register");
        assertThat(plan.automationDirective().taskId()).startsWith("hermes-automation-");
        assertThat(plan.delegationPlan().delegated()).isTrue();
        assertThat(plan.delegationDirective().active()).isTrue();
        assertThat(plan.delegationDirective().operation()).isEqualTo("spawn");
        assertThat(plan.delegationDirective().subAgentCount()).isEqualTo(2);
        assertThat(plan.delegationDirective().lanes()).containsExactly("research", "verification");
        assertThat(plan.providerRoutingPlan().selectedProvider()).isEqualTo("ollama");
        assertThat(plan.providerRoutingDirective().active()).isTrue();
        assertThat(plan.providerRoutingDirective().operation()).isEqualTo("route");
        assertThat(plan.providerRoutingDirective().selectedProvider()).isEqualTo("ollama");
        assertThat(plan.providerRoutingDirective().routingMode()).isEqualTo("local");
        assertThat(plan.memoryReflectionPlan().reflect()).isTrue();
        assertThat(plan.memoryReflectionDirective().active()).isTrue();
        assertThat(plan.memoryReflectionDirective().operation()).isEqualTo("consolidate");
        assertThat(plan.memoryReflectionDirective().subjectId()).isEqualTo("session-a");
        assertThat(plan.trajectoryExportPlan().export()).isTrue();
        assertThat(plan.trajectoryExportDirective().active()).isTrue();
        assertThat(plan.trajectoryExportDirective().operation()).isEqualTo("export");
        assertThat(plan.trajectoryExportDirective().destination()).isEqualTo("local");
        assertThat(plan.skillLineagePlan().active()).isTrue();
        assertThat(plan.skillLineagePlan().operation()).isEqualTo("catalog");
        assertThat(plan.skillLineageDirective().active()).isTrue();
        assertThat(plan.skillLineageDirective().operation()).isEqualTo("catalog");
        assertThat(plan.skillLineageDirective().target()).isEqualTo("learned-skills");
        assertThat(plan.contextMetadata())
                .containsKeys(HermesMetadataKeys.CONTEXT_PLAN_KEYS.toArray(String[]::new))
                .containsKeys(HermesMetadataKeys.CONTEXT_DIRECTIVE_KEYS.toArray(String[]::new));
        assertThat(plan.parameterMetadata())
                .containsKeys(HermesMetadataKeys.PARAMETER_PLAN_KEYS.toArray(String[]::new))
                .containsKeys(HermesMetadataKeys.PARAMETER_DIRECTIVE_KEYS.toArray(String[]::new));
    }

    @Test
    void defaultPlanExposesStableNoRequestMetadata() {
        HermesRequestPlan plan = new HermesRequestPlanner(HermesAgentModeConfig.defaults()).defaultPlan();

        assertThat(plan.executionPlan().backend()).isEqualTo("local");
        assertThat(plan.executionDirective().active()).isFalse();
        assertThat(plan.executionDirective().operation()).isEqualTo("none");
        assertThat(plan.gatewayContext().platform()).isEqualTo("cli");
        assertThat(plan.gatewayDirective().active()).isFalse();
        assertThat(plan.gatewayDirective().operation()).isEqualTo("none");
        assertThat(plan.automationIntent().scheduled()).isFalse();
        assertThat(plan.automationDirective().active()).isFalse();
        assertThat(plan.delegationPlan().delegated()).isFalse();
        assertThat(plan.delegationDirective().active()).isFalse();
        assertThat(plan.delegationDirective().operation()).isEqualTo("none");
        assertThat(plan.providerRoutingPlan().selectedProvider()).isEqualTo("auto");
        assertThat(plan.providerRoutingDirective().active()).isFalse();
        assertThat(plan.providerRoutingDirective().operation()).isEqualTo("none");
        assertThat(plan.memoryReflectionPlan().reflect()).isFalse();
        assertThat(plan.memoryReflectionDirective().active()).isFalse();
        assertThat(plan.trajectoryExportPlan().exportEnabled()).isFalse();
        assertThat(plan.trajectoryExportDirective().active()).isFalse();
        assertThat(plan.trajectoryExportDirective().operation()).isEqualTo("none");
        assertThat(plan.skillLineagePlan().active()).isFalse();
        assertThat(plan.skillLineageDirective().active()).isFalse();
        assertThat(plan.skillLineageDirective().operation()).isEqualTo("none");
        assertThat(plan.contextMetadata())
                .hasSize(HermesMetadataKeys.CONTEXT_PLAN_KEYS.size()
                        + HermesMetadataKeys.CONTEXT_DIRECTIVE_KEYS.size());
        assertThat(plan.parameterMetadata())
                .hasSize(HermesMetadataKeys.PARAMETER_PLAN_KEYS.size()
                        + HermesMetadataKeys.PARAMETER_DIRECTIVE_KEYS.size());
    }
}
