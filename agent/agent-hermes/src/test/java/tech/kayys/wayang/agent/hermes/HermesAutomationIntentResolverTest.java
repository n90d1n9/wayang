package tech.kayys.wayang.agent.hermes;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.spi.AgentRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HermesAutomationIntentResolverTest {

    @Test
    void resolvesExplicitCronSchedule() {
        HermesAutomationIntentResolver resolver = new HermesAutomationIntentResolver(HermesAgentModeConfig.defaults());

        HermesAutomationIntent intent = resolver.resolve(AgentRequest.builder()
                .prompt("Generate the release report")
                .parameter("cron", "0 7 * * *")
                .parameter("timezone", "Asia/Jakarta")
                .build());

        assertThat(intent.schedulerEnabled()).isTrue();
        assertThat(intent.scheduled()).isTrue();
        assertThat(intent.active()).isTrue();
        assertThat(intent.schedule()).isEqualTo("0 7 * * *");
        assertThat(intent.scheduleType()).isEqualTo("cron");
        assertThat(intent.task()).isEqualTo("Generate the release report");
        assertThat(intent.timezone()).isEqualTo("Asia/Jakarta");
        assertThat(intent.source()).isEqualTo("explicit");
        assertThat(intent.recurring()).isTrue();
    }

    @Test
    void resolvesExplicitRruleSchedule() {
        HermesAutomationIntentResolver resolver = new HermesAutomationIntentResolver(HermesAgentModeConfig.defaults());

        HermesAutomationIntent intent = resolver.resolve(AgentRequest.builder()
                .prompt("Generate the API backup report")
                .parameter("schedule", "FREQ=DAILY;INTERVAL=1")
                .build());

        assertThat(intent.scheduled()).isTrue();
        assertThat(intent.schedule()).isEqualTo("FREQ=DAILY;INTERVAL=1");
        assertThat(intent.scheduleType()).isEqualTo("rrule");
        assertThat(intent.source()).isEqualTo("explicit");
        assertThat(intent.recurring()).isTrue();
    }

    @Test
    void resolvesExplicitIsoIntervalSchedule() {
        HermesAutomationIntentResolver resolver = new HermesAutomationIntentResolver(HermesAgentModeConfig.defaults());

        HermesAutomationIntent intent = resolver.resolve(AgentRequest.builder()
                .prompt("Check queue health")
                .parameter("interval", "PT30M")
                .build());

        assertThat(intent.scheduled()).isTrue();
        assertThat(intent.schedule()).isEqualTo("PT30M");
        assertThat(intent.scheduleType()).isEqualTo("interval");
        assertThat(intent.recurring()).isTrue();
    }

    @Test
    void infersRecurringScheduleFromPrompt() {
        HermesAutomationIntentResolver resolver = new HermesAutomationIntentResolver(HermesAgentModeConfig.defaults());

        HermesAutomationIntent intent = resolver.resolve(AgentRequest.builder()
                .prompt("Generate the API backup report every day")
                .build());

        assertThat(intent.scheduled()).isTrue();
        assertThat(intent.schedule()).isEqualTo("daily");
        assertThat(intent.scheduleType()).isEqualTo("natural-language");
        assertThat(intent.source()).isEqualTo("prompt");
        assertThat(intent.recurring()).isTrue();
        assertThat(intent.reason()).isEqualTo("recurring schedule inferred from prompt");
    }

    @Test
    void keepsIntentInactiveWhenCronIsDisabled() {
        HermesAutomationIntentResolver resolver = new HermesAutomationIntentResolver(HermesAgentModeConfig.builder()
                .cronEnabled(false)
                .build());

        HermesAutomationIntent intent = resolver.resolve(AgentRequest.builder()
                .prompt("Run this nightly")
                .build());

        assertThat(intent.schedulerEnabled()).isFalse();
        assertThat(intent.scheduled()).isFalse();
        assertThat(intent.active()).isFalse();
        assertThat(intent.schedule()).isEqualTo("nightly");
        assertThat(intent.source()).isEqualTo("disabled");
        assertThat(intent.reason()).isEqualTo("cron automation disabled");
    }

    @Test
    void reportsNoScheduleWhenRequestIsOnDemand() {
        HermesAutomationIntentResolver resolver = new HermesAutomationIntentResolver(HermesAgentModeConfig.defaults());

        HermesAutomationIntent intent = resolver.resolve(AgentRequest.builder()
                .prompt("Prepare a release report")
                .build());

        assertThat(intent.schedulerEnabled()).isTrue();
        assertThat(intent.scheduled()).isFalse();
        assertThat(intent.active()).isFalse();
        assertThat(intent.schedule()).isEmpty();
        assertThat(intent.scheduleType()).isEqualTo("none");
        assertThat(intent.source()).isEqualTo("none");
        assertThat(intent.reason()).isEqualTo("no automation schedule detected");
    }

    @Test
    void rejectsInvalidAutomationBooleanHints() {
        HermesAutomationIntentResolver resolver = new HermesAutomationIntentResolver(HermesAgentModeConfig.defaults());

        assertThatThrownBy(() -> resolver.resolve(AgentRequest.builder()
                .prompt("Run report")
                .parameter("scheduled", "maybe")
                .build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("automation boolean");
    }
}
