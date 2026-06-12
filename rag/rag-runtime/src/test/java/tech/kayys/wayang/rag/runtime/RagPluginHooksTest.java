package tech.kayys.wayang.rag.runtime;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.rag.core.GenerationConfig;
import tech.kayys.wayang.rag.core.RagChunk;
import tech.kayys.wayang.rag.core.RagQuery;
import tech.kayys.wayang.rag.core.RagResult;
import tech.kayys.wayang.rag.core.RagScoredChunk;
import tech.kayys.wayang.rag.plugin.api.RagPipelinePlugin;
import tech.kayys.wayang.rag.plugin.api.RagPluginExecutionContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RagPluginHooksTest {

    @Test
    void beforeQueryKeepsPreviousContextOnNullOrException() {
        RagPluginExecutionContext context = context("q");

        RagPluginExecutionContext updated = RagPluginHooks.beforeQuery(
                List.of(
                        new QueryPlugin("a", "[A]", false, false),
                        new QueryPlugin("null", "[NULL]", true, false),
                        new QueryPlugin("failing", "[FAIL]", false, true),
                        new QueryPlugin("b", "[B]", false, false)),
                context,
                null);

        assertEquals("q[A][B]", updated.query());
    }

    @Test
    void afterRetrieveKeepsPreviousChunksOnNullOrException() {
        List<RagScoredChunk> updated = RagPluginHooks.afterRetrieve(
                List.of(
                        new RetrievePlugin("a", false, false),
                        new RetrievePlugin("null", true, false),
                        new RetrievePlugin("failing", false, true),
                        new RetrievePlugin("b", false, false)),
                context("q"),
                null,
                null);

        assertEquals(List.of("chunk-a", "chunk-b"), updated.stream()
                .map(chunk -> chunk.chunk().id())
                .toList());
    }

    @Test
    void afterResultKeepsPreviousResultOnNullOrException() {
        RagResult base = new RagResult(RagQuery.of("q"), List.of(), "answer", Map.of());

        RagResult updated = RagPluginHooks.afterResult(
                List.of(
                        new ResultPlugin("a", "[A]", false, false),
                        new ResultPlugin("null", "[NULL]", true, false),
                        new ResultPlugin("failing", "[FAIL]", false, true),
                        new ResultPlugin("b", "[B]", false, false)),
                context("q"),
                base,
                null);

        assertEquals("answer[A][B]", updated.answer());
    }

    private static RagPluginExecutionContext context(String query) {
        return new RagPluginExecutionContext(
                "tenant",
                query,
                5,
                0.4f,
                Map.of(),
                GenerationConfig.defaults(),
                false);
    }

    private record QueryPlugin(String id, String marker, boolean returnsNull, boolean fails)
            implements RagPipelinePlugin {

        @Override
        public RagPluginExecutionContext beforeQuery(RagPluginExecutionContext context) {
            if (fails) {
                throw new IllegalStateException("query failed");
            }
            if (returnsNull) {
                return null;
            }
            return context.withQuery(context.query() + marker);
        }
    }

    private record RetrievePlugin(String id, boolean returnsNull, boolean fails)
            implements RagPipelinePlugin {

        @Override
        public List<RagScoredChunk> afterRetrieve(
                RagPluginExecutionContext context,
                List<RagScoredChunk> chunks) {
            if (fails) {
                throw new IllegalStateException("retrieve failed");
            }
            if (returnsNull) {
                return null;
            }
            List<RagScoredChunk> updated = new ArrayList<>(chunks);
            RagChunk chunk = new RagChunk("chunk-" + id, "doc-" + id, 0, "text", Map.of());
            updated.add(new RagScoredChunk(chunk, 0.8));
            return updated;
        }
    }

    private record ResultPlugin(String id, String marker, boolean returnsNull, boolean fails)
            implements RagPipelinePlugin {

        @Override
        public RagResult afterResult(RagPluginExecutionContext context, RagResult result) {
            if (fails) {
                throw new IllegalStateException("result failed");
            }
            if (returnsNull) {
                return null;
            }
            return new RagResult(result.query(), result.chunks(), result.answer() + marker, result.metadata());
        }
    }
}
