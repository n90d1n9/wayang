package tech.kayys.wayang.hitl.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TaskOutcomeTest {

    @Test
    void shouldHaveCorrectValues() {
        assertEquals("APPROVED", TaskOutcome.APPROVED.name());
        assertEquals("REJECTED", TaskOutcome.REJECTED.name());
        assertEquals("COMPLETED", TaskOutcome.COMPLETED.name());
        assertEquals("CANCELLED", TaskOutcome.CANCELLED.name());
        assertEquals("EXPIRED", TaskOutcome.EXPIRED.name());
        assertEquals("CUSTOM", TaskOutcome.CUSTOM.name());
    }
}