package tech.kayys.wayang.agent.hermes;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.spi.AgentRequest;

import static org.assertj.core.api.Assertions.assertThat;

class HermesTrajectoryExportDirectiveTest {

    @Test
    void activeDirectiveCarriesExportPayload() {
        HermesTrajectoryExportDirective directive = HermesTrajectoryExportDirective.from(
                new HermesTrajectoryExportPlan(
                        true,
                        true,
                        true,
                        "markdown",
                        "object-storage",
                        true,
                        false,
                        false,
                        "explicit",
                        "explicit trajectory export requested"),
                AgentRequest.builder()
                        .requestId("Req 77")
                        .tenantId("tenant-a")
                        .sessionId("session-a")
                        .userId("user-a")
                        .build());

        assertThat(directive.active()).isTrue();
        assertThat(directive.operation()).isEqualTo("export");
        assertThat(directive.exportId()).isEqualTo("hermes-trajectory-req-77");
        assertThat(directive.format()).isEqualTo("markdown");
        assertThat(directive.destination()).isEqualTo("object-storage");
        assertThat(directive.includePrompts()).isTrue();
        assertThat(directive.includeToolCalls()).isFalse();
        assertThat(directive.redactSensitive()).isFalse();
        assertThat(directive.toMetadata())
                .containsEntry("requestId", "Req 77")
                .containsEntry("tenantId", "tenant-a")
                .containsEntry("sessionId", "session-a")
                .containsEntry("userId", "user-a")
                .containsEntry("reason", "explicit trajectory export requested");
    }

    @Test
    void disabledExportKeepsRequestButDoesNotExport() {
        HermesTrajectoryExportDirective directive = HermesTrajectoryExportDirective.from(
                new HermesTrajectoryExportPlan(
                        false,
                        true,
                        false,
                        "jsonl",
                        "none",
                        false,
                        true,
                        true,
                        "disabled",
                        "trajectory export disabled"),
                AgentRequest.builder()
                        .requestId("req-b")
                        .build());

        assertThat(directive.exportEnabled()).isFalse();
        assertThat(directive.requested()).isTrue();
        assertThat(directive.active()).isFalse();
        assertThat(directive.operation()).isEqualTo("none");
        assertThat(directive.exportId()).isEmpty();
        assertThat(directive.destination()).isEqualTo("none");
        assertThat(directive.reason()).isEqualTo("trajectory export disabled");
    }

    @Test
    void defaultPlanDoesNotExportWithoutRequest() {
        HermesTrajectoryExportDirective directive = HermesTrajectoryExportDirective.from(
                new HermesTrajectoryExportResolver(HermesAgentModeConfig.defaults()).defaultPlan(),
                null);

        assertThat(directive.exportEnabled()).isFalse();
        assertThat(directive.requested()).isFalse();
        assertThat(directive.active()).isFalse();
        assertThat(directive.operation()).isEqualTo("none");
        assertThat(directive.format()).isEqualTo("jsonl");
        assertThat(directive.destination()).isEqualTo("none");
        assertThat(directive.reason()).isEqualTo("default plan only");
    }
}
