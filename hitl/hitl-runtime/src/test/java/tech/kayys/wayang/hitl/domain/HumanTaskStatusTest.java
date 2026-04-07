package tech.kayys.wayang.hitl.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HumanTaskStatusTest {

    @Test
    void shouldReturnTrueForTerminalStatuses() {
        assertTrue(HumanTaskStatus.COMPLETED.isTerminal());
        assertTrue(HumanTaskStatus.CANCELLED.isTerminal());
        assertTrue(HumanTaskStatus.EXPIRED.isTerminal());
    }

    @Test
    void shouldReturnFalseForNonTerminalStatuses() {
        assertFalse(HumanTaskStatus.CREATED.isTerminal());
        assertFalse(HumanTaskStatus.ASSIGNED.isTerminal());
        assertFalse(HumanTaskStatus.IN_PROGRESS.isTerminal());
        assertFalse(HumanTaskStatus.ESCALATED.isTerminal());
    }
}