package tech.kayys.wayang.rag.runtime;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.rag.core.ConversationTurn;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RagConversationQueryTest {

    @Test
    void addsConversationMetadataAndEnhancesQuery() {
        ConversationTurn turn = new ConversationTurn("user", "hello", Instant.EPOCH);

        RagConversationQuery query = RagConversationQuery.from(
                "what next?",
                "session-1",
                List.of(turn));

        assertEquals(
                "Previous conversation:\nuser: hello\n\nCurrent question: what next?",
                query.query());
        assertEquals("session-1", query.metadata().get(RagConversationQuery.SESSION_ID));
        assertEquals(List.of(turn), query.metadata().get(RagConversationQuery.CONVERSATION_HISTORY));
    }

    @Test
    void filtersNullTurnsBeforeMetadataAndQueryRendering() {
        ConversationTurn turn = new ConversationTurn("assistant", "answer", Instant.EPOCH);

        RagConversationQuery query = RagConversationQuery.from(
                "follow up",
                null,
                Arrays.asList(null, turn));

        assertEquals(
                "Previous conversation:\nassistant: answer\n\nCurrent question: follow up",
                query.query());
        assertFalse(query.metadata().containsKey(RagConversationQuery.SESSION_ID));
        assertEquals(List.of(turn), query.metadata().get(RagConversationQuery.CONVERSATION_HISTORY));
    }

    @Test
    void leavesQueryUnchangedWhenHistoryIsEmpty() {
        RagConversationQuery query = RagConversationQuery.from("plain question", " ", null);

        assertEquals("plain question", query.query());
        assertEquals(0, query.metadata().size());
    }

    @Test
    void normalizesQueryAndSessionInputs() {
        ConversationTurn turn = new ConversationTurn("user", "hello", Instant.EPOCH);

        RagConversationQuery query = RagConversationQuery.from(
                "  ",
                " session-1 ",
                List.of(turn));

        assertEquals(
                "Previous conversation:\nuser: hello\n\nCurrent question:",
                query.query());
        assertEquals("session-1", query.metadata().get(RagConversationQuery.SESSION_ID));
    }

    @Test
    void snapshotsMetadataAndHistoryDefensively() {
        ConversationTurn turn = new ConversationTurn("user", "hello", Instant.EPOCH);
        List<ConversationTurn> history = new ArrayList<>(List.of(turn));
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put(RagConversationQuery.SESSION_ID, " session-1 ");
        metadata.put(RagConversationQuery.CONVERSATION_HISTORY, history);

        RagConversationQuery query = new RagConversationQuery(" question ", metadata);
        history.clear();
        metadata.put(RagConversationQuery.SESSION_ID, "mutated");

        assertEquals("question", query.query());
        assertEquals("session-1", query.metadata().get(RagConversationQuery.SESSION_ID));
        assertEquals(List.of(turn), query.metadata().get(RagConversationQuery.CONVERSATION_HISTORY));
        assertThrows(UnsupportedOperationException.class, () -> query.metadata().put("other", "value"));
        @SuppressWarnings("unchecked")
        List<ConversationTurn> copiedHistory =
                (List<ConversationTurn>) query.metadata().get(RagConversationQuery.CONVERSATION_HISTORY);
        assertThrows(UnsupportedOperationException.class, () -> copiedHistory.add(turn));
    }
}
