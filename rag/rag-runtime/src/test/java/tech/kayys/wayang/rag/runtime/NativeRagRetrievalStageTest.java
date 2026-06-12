package tech.kayys.wayang.rag.runtime;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.rag.core.GenerationConfig;
import tech.kayys.wayang.rag.core.RagQuery;
import tech.kayys.wayang.rag.core.RagResult;
import tech.kayys.wayang.rag.core.RetrievalConfig;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NativeRagRetrievalStageTest {

    @Test
    void copiesRetrievalMetadataWhenBuildingAnswerResult() {
        NativeRagQueryContext context = NativeRagQueryContext.create(
                "tenant",
                "question",
                RetrievalConfig.defaults(),
                GenerationConfig.defaults(),
                Map.of(),
                false);
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("retrieved", 1);
        NativeRagRetrievalStage stage = new NativeRagRetrievalStage(
                context,
                new RagResult(RagQuery.of("question"), List.of(), "", metadata),
                List.of());

        RagResult result = stage.withAnswer("answer");
        metadata.put("retrieved", 2);

        assertEquals("answer", result.answer());
        assertEquals(1, result.metadata().get("retrieved"));
        assertThrows(UnsupportedOperationException.class, () -> result.metadata().put("other", "value"));
    }
}
