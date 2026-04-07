package tech.kayys.wayang.hitl.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EscalationReasonTest {

    @Test
    void shouldHaveCorrectValues() {
        assertEquals("TIMEOUT", EscalationReason.TIMEOUT.name());
        assertEquals("MANUAL", EscalationReason.MANUAL.name());
        assertEquals("SLA_BREACH", EscalationReason.SLA_BREACH.name());
        assertEquals("PRIORITY_CHANGE", EscalationReason.PRIORITY_CHANGE.name());
    }
}