package tech.kayys.wayang.agent.hermes;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.spi.AgentRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HermesDelegationDirectiveTest {

    @Test
    void activeDirectiveCarriesSubAgentSpawnPayload() {
        HermesDelegationDirective directive = HermesDelegationDirective.from(
                new HermesDelegationPlan(
                        true,
                        true,
                        true,
                        3,
                        4,
                        List.of("research", "implementation", "verification"),
                        "context-isolated",
                        "explicit",
                        "explicit delegation requested"),
                AgentRequest.builder()
                        .requestId("Req 42")
                        .tenantId("tenant-a")
                        .sessionId("session-a")
                        .userId("user-a")
                        .build());

        assertThat(directive.active()).isTrue();
        assertThat(directive.operation()).isEqualTo("spawn");
        assertThat(directive.groupId()).isEqualTo("hermes-delegation-req-42");
        assertThat(directive.subAgentCount()).isEqualTo(3);
        assertThat(directive.lanes()).containsExactly("research", "implementation", "verification");
        assertThat(directive.toMetadata())
                .containsEntry("parentRequestId", "Req 42")
                .containsEntry("tenantId", "tenant-a")
                .containsEntry("sessionId", "session-a")
                .containsEntry("userId", "user-a")
                .containsEntry("reason", "explicit delegation requested");
    }

    @Test
    void disabledDelegationKeepsIntentButDoesNotSpawn() {
        HermesDelegationDirective directive = HermesDelegationDirective.from(
                new HermesDelegationPlan(
                        false,
                        true,
                        false,
                        0,
                        4,
                        List.of("research", "verification"),
                        "none",
                        "disabled",
                        "sub-agent delegation disabled"),
                AgentRequest.builder()
                        .requestId("req-b")
                        .build());

        assertThat(directive.delegationEnabled()).isFalse();
        assertThat(directive.requested()).isTrue();
        assertThat(directive.delegated()).isFalse();
        assertThat(directive.active()).isFalse();
        assertThat(directive.operation()).isEqualTo("none");
        assertThat(directive.groupId()).isEmpty();
        assertThat(directive.subAgentCount()).isZero();
        assertThat(directive.lanes()).containsExactly("research", "verification");
        assertThat(directive.reason()).isEqualTo("sub-agent delegation disabled");
    }

    @Test
    void defaultPlanDoesNotSpawnWithoutRequest() {
        HermesDelegationDirective directive = HermesDelegationDirective.from(
                new HermesDelegationPlanner(HermesAgentModeConfig.defaults()).defaultPlan(),
                null);

        assertThat(directive.delegationEnabled()).isTrue();
        assertThat(directive.requested()).isFalse();
        assertThat(directive.delegated()).isFalse();
        assertThat(directive.active()).isFalse();
        assertThat(directive.operation()).isEqualTo("none");
        assertThat(directive.maxSubAgents()).isEqualTo(4);
        assertThat(directive.reason()).isEqualTo("default plan only");
    }
}
