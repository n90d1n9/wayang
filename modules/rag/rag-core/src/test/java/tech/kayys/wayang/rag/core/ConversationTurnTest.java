package tech.kayys.wayang.rag.core;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConversationTurnTest {

    @Test
    void normalizesRoleAndDefaultsMissingValues() {
        ConversationTurn turn = new ConversationTurn(" user ", null, null);

        assertEquals("user", turn.role());
        assertEquals("", turn.content());
        assertEquals(Instant.EPOCH, turn.timestamp());
        assertFalse(turn.hasContent());
    }

    @Test
    void reportsRenderableHistoryTurns() {
        assertTrue(new ConversationTurn("assistant", "answer", Instant.EPOCH).hasContent());
        assertFalse(new ConversationTurn("", "answer", Instant.EPOCH).hasContent());
        assertFalse(new ConversationTurn("assistant", " ", Instant.EPOCH).hasContent());
    }
}
