package tech.kayys.wayang.rag.runtime;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.rag.core.ConversationTurn;
import tech.kayys.wayang.rag.core.RagMetadataKeys;
import tech.kayys.wayang.rag.core.RagMode;
import tech.kayys.wayang.rag.core.RetrievalConfig;
import tech.kayys.wayang.rag.core.SearchStrategy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RagQueryWorkflowContextTest {

    @Test
    void constructorNormalizesDefaultsAndCopiesInputsDefensively() {
        List<String> collections = new ArrayList<>(List.of(" docs ", " ", "faq"));
        Map<String, Object> filters = new HashMap<>();
        filters.put("domain", "manual");
        filters.put("nullable", null);

        RagQueryWorkflowContext context = new RagQueryWorkflowContext(
                " tenant ",
                " question ",
                null,
                null,
                null,
                null,
                collections,
                filters);
        collections.set(0, "mutated");
        filters.put("domain", "mutated");

        assertEquals("tenant", context.tenantId());
        assertEquals("question", context.query());
        assertEquals(RagMode.STANDARD, context.mode());
        assertEquals(SearchStrategy.HYBRID, context.strategy());
        assertEquals(RetrievalConfig.defaults().topK(), context.retrievalConfig().topK());
        assertEquals(List.of("docs", "faq"), context.collections());
        assertEquals("manual", context.filters().get("domain"));
        assertTrue(context.filters().containsKey("nullable"));
        assertThrows(UnsupportedOperationException.class, () -> context.collections().add("other"));
        assertThrows(UnsupportedOperationException.class, () -> context.filters().put("other", "value"));
    }

    @Test
    void simpleContextNormalizesBlankTenantQueryAndCollection() {
        RagQueryWorkflowContext context = RagQueryWorkflowContext.simple(null, null, " docs ");

        assertEquals("", context.tenantId());
        assertEquals("", context.query());
        assertEquals(List.of("docs"), context.collections());
        assertEquals("docs", context.nativeFilters().get(RagMetadataKeys.COLLECTION));
    }

    @Test
    void conversationalContextSnapshotsConversationMetadata() {
        ConversationTurn turn = new ConversationTurn("user", "hello", Instant.EPOCH);
        List<ConversationTurn> history = new ArrayList<>(List.of(turn));

        RagQueryWorkflowContext context = RagQueryWorkflowContext.conversational(
                " tenant ",
                " question ",
                " session-1 ",
                history);
        history.clear();

        assertEquals("tenant", context.tenantId());
        assertEquals("Previous conversation:\nuser: hello\n\nCurrent question: question", context.query());
        assertEquals("session-1", context.filters().get(RagConversationQuery.SESSION_ID));
        Object historyMetadata = context.filters().get(RagConversationQuery.CONVERSATION_HISTORY);
        @SuppressWarnings("unchecked")
        List<ConversationTurn> copiedHistory = (List<ConversationTurn>) assertInstanceOf(List.class, historyMetadata);
        assertEquals(List.of(turn), copiedHistory);
        assertThrows(UnsupportedOperationException.class, () -> copiedHistory.add(turn));
        assertThrows(UnsupportedOperationException.class, () -> context.filters().put("other", "value"));
    }
}
