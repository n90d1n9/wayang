package tech.kayys.wayang.agent.hermes;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.spi.AgentRequest;

import static org.assertj.core.api.Assertions.assertThat;

class HermesAutomationDirectiveTest {

    @Test
    void activeScheduleDirectiveCarriesRegistrationPayload() {
        HermesAutomationDirective directive = HermesAutomationDirective.from(
                new HermesAutomationIntent(
                        true,
                        true,
                        "0 7 * * *",
                        "cron",
                        "Generate daily report",
                        "Asia/Jakarta",
                        "explicit",
                        true,
                        "explicit schedule provided"),
                AgentRequest.builder()
                        .requestId("Req 42")
                        .tenantId("tenant-a")
                        .sessionId("session-a")
                        .userId("user-a")
                        .build());

        assertThat(directive.active()).isTrue();
        assertThat(directive.operation()).isEqualTo("register");
        assertThat(directive.taskId()).isEqualTo("hermes-automation-req-42");
        assertThat(directive.schedule()).isEqualTo("0 7 * * *");
        assertThat(directive.scheduleType()).isEqualTo("cron");
        assertThat(directive.timezone()).isEqualTo("Asia/Jakarta");
        assertThat(directive.toMetadata())
                .containsEntry("tenantId", "tenant-a")
                .containsEntry("sessionId", "session-a")
                .containsEntry("userId", "user-a")
                .containsEntry("recurring", true);
    }

    @Test
    void inactiveIntentDisablesRegistrationOperation() {
        HermesAutomationDirective directive = HermesAutomationDirective.from(
                new HermesAutomationIntent(
                        true,
                        false,
                        "",
                        "none",
                        "Prepare report",
                        "",
                        "none",
                        false,
                        "no automation schedule detected"),
                AgentRequest.builder()
                        .requestId("req-b")
                        .build());

        assertThat(directive.active()).isFalse();
        assertThat(directive.operation()).isEqualTo("none");
        assertThat(directive.taskId()).isEmpty();
        assertThat(directive.reason()).isEqualTo("no automation schedule detected");
    }

    @Test
    void disabledSchedulerKeepsScheduleButNoRegistration() {
        HermesAutomationDirective directive = HermesAutomationDirective.from(
                new HermesAutomationIntent(
                        false,
                        false,
                        "daily",
                        "natural-language",
                        "Generate report",
                        "",
                        "disabled",
                        true,
                        "cron automation disabled"),
                AgentRequest.builder()
                        .requestId("req-c")
                        .build());

        assertThat(directive.schedulerEnabled()).isFalse();
        assertThat(directive.scheduled()).isFalse();
        assertThat(directive.active()).isFalse();
        assertThat(directive.operation()).isEqualTo("none");
        assertThat(directive.schedule()).isEqualTo("daily");
        assertThat(directive.recurring()).isTrue();
    }
}
