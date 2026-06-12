package tech.kayys.wayang.agent.api;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HermesOperationalMessagesTest {

    @Test
    void pinsUserFacingPortMessages() {
        assertThat(HermesOperationalMessages.MISSING_DIAGNOSTICS_PORT)
                .isEqualTo("Hermes runtime diagnostics port is not configured");
        assertThat(HermesOperationalMessages.MISSING_JOURNAL_PORT)
                .isEqualTo("Hermes runtime journal port is not configured");
        assertThat(HermesOperationalMessages.MISSING_LEARNING_AUDIT_PORT)
                .isEqualTo("Hermes learning audit port is not configured");
    }
}
