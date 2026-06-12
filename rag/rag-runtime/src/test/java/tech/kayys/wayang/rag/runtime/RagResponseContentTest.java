package tech.kayys.wayang.rag.runtime;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.rag.core.RagQuery;
import tech.kayys.wayang.rag.core.RagResult;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RagResponseContentTest {

    @Test
    void defaultsMissingAnswerToEmptyString() {
        assertEquals("", RagResponseContent.answer(null));
        assertEquals("", RagResponseContent.answer(new RagResult(RagQuery.of("q"), List.of(), null, Map.of())));
    }

    @Test
    void keepsAnswerAndResponseContextAligned() {
        RagResult result = new RagResult(RagQuery.of("q"), List.of(), "answer", Map.of());

        assertEquals("answer", RagResponseContent.answer(result));
        assertEquals("answer", RagResponseContent.context(result));
    }
}
