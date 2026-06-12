package tech.kayys.wayang.agent.hermes;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.spi.AgentMemoryConfig;
import tech.kayys.wayang.agent.spi.AgentRequest;

import static org.assertj.core.api.Assertions.assertThat;

class HermesMemoryReflectionDirectiveTest {

    @Test
    void activeUserReflectionDirectiveCarriesAdapterPayload() {
        HermesMemoryReflectionPlan plan = new HermesMemoryReflectionPlan(
                true,
                true,
                true,
                "user",
                "post-run",
                "high",
                "prompt",
                "memory reflection inferred from prompt");

        HermesMemoryReflectionDirective directive = HermesMemoryReflectionDirective.from(
                plan,
                AgentRequest.builder()
                        .requestId("req-a")
                        .tenantId("tenant-a")
                        .sessionId("session-a")
                        .userId("user-a")
                        .memoryConfig(new AgentMemoryConfig(true, 12, true, "workspace-a"))
                        .build());

        assertThat(directive.active()).isTrue();
        assertThat(directive.operation()).isEqualTo("consolidate");
        assertThat(directive.subjectType()).isEqualTo("user");
        assertThat(directive.subjectId()).isEqualTo("user-a");
        assertThat(directive.memoryNamespace()).isEqualTo("workspace-a");
        assertThat(directive.toMetadata())
                .containsEntry("requestId", "req-a")
                .containsEntry("tenantId", "tenant-a")
                .containsEntry("sessionId", "session-a")
                .containsEntry("priority", "high");
    }

    @Test
    void inactiveDirectiveKeepsSubjectButDisablesOperation() {
        HermesMemoryReflectionDirective directive = HermesMemoryReflectionDirective.from(
                new HermesMemoryReflectionPlan(true, false, false, "session", "none", "normal", "none", ""),
                AgentRequest.builder()
                        .requestId("req-b")
                        .sessionId("session-b")
                        .build());

        assertThat(directive.active()).isFalse();
        assertThat(directive.operation()).isEqualTo("none");
        assertThat(directive.subjectType()).isEqualTo("session");
        assertThat(directive.subjectId()).isEqualTo("session-b");
        assertThat(directive.reason()).isEqualTo("no memory reflection requested");
    }

    @Test
    void agentScopeUsesAgentIdFromContext() {
        HermesMemoryReflectionDirective directive = HermesMemoryReflectionDirective.from(
                new HermesMemoryReflectionPlan(true, true, true, "agent", "post-run", "normal", "explicit", ""),
                AgentRequest.builder()
                        .tenantId("tenant-a")
                        .agentId("agent-a")
                        .build());

        assertThat(directive.subjectType()).isEqualTo("agent");
        assertThat(directive.subjectId()).isEqualTo("agent-a");
    }
}
