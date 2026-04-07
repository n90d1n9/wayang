package tech.kayys.wayang.rag.runtime.langchain.plugins;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.rag.core.GenerationConfig;
import tech.kayys.wayang.rag.plugin.api.RagPluginExecutionContext;
import tech.kayys.wayang.rag.runtime.RagRuntimePluginTuningConfig;
import tech.kayys.wayang.rag.runtime.RagRuntimeConfig;
import tech.kayys.wayang.rag.LexicalRerankPlugin;
import tech.kayys.wayang.rag.QueryRewritePlugin;
import tech.kayys.wayang.rag.SafetyFilterPlugin;
import tech.kayys.wayang.rag.core.RagChunk;
import tech.kayys.wayang.rag.core.RagQuery;
import tech.kayys.wayang.rag.core.RagResult;
import tech.kayys.wayang.rag.core.RagScoredChunk;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuiltInRagPluginsTest {

    @Test
    void shouldNormalizeQueryWhitespaceAndLowercase() {
        RagRuntimeConfig config = new RagRuntimeConfig();
        config.setRagPluginNormalizeLowercase(true);
        QueryRewritePlugin plugin = new QueryRewritePlugin(new RagRuntimePluginTuningConfig(config));

        RagPluginExecutionContext context = context("  HELLO   Plugin   WORLD  ");
        RagPluginExecutionContext updated = plugin.beforeQuery(context);

        assertEquals("hello plugin world", updated.query());
    }

    @Test
    void shouldRerankChunksUsingLexicalSimilarity() {
        RagRuntimeConfig config = new RagRuntimeConfig();
        config.setRagPluginRerankOriginalWeight(0.2);
        config.setRagPluginRerankLexicalWeight(0.8);

        LexicalRerankPlugin plugin = new LexicalRerankPlugin(new RagRuntimePluginTuningConfig(config));

        RagScoredChunk lowOriginalHighLexical = scoredChunk("a", "payment status approved", 0.20);
        RagScoredChunk highOriginalLowLexical = scoredChunk("b", "shipping address only", 0.90);
        List<RagScoredChunk> ranked = plugin.afterRetrieve(
                context("payment status"),
                List.of(highOriginalLowLexical, lowOriginalHighLexical));

        assertEquals("a", ranked.get(0).chunk().id());
        assertEquals("b", ranked.get(1).chunk().id());
        assertTrue(ranked.get(0).chunk().metadata().containsKey("plugin.lexical_rerank.lexical_score"));
    }

    @Test
    void shouldFilterAndRedactSafetyTerms() {
        RagRuntimeConfig config = new RagRuntimeConfig();
        config.setRagPluginSafetyBlockedTerms("secret,token");
        config.setRagPluginSafetyMask("[MASKED]");

        SafetyFilterPlugin safety = new SafetyFilterPlugin(new RagRuntimePluginTuningConfig(config));

        RagPluginExecutionContext before = safety.beforeQuery(context("what is secret token usage"));
        assertEquals("what is [MASKED] [MASKED] usage", before.query());

        List<RagScoredChunk> filtered = safety.afterRetrieve(before, List.of(
                scoredChunk("safe", "general guidance", 0.8),
                scoredChunk("unsafe", "contains SECRET material", 0.9)));
        assertEquals(1, filtered.size());
        assertEquals("safe", filtered.get(0).chunk().id());

        RagResult result = new RagResult(RagQuery.of("q"), List.of(), "response with token", Map.of());
        RagResult redacted = safety.afterResult(before, result);
        assertEquals("response with [MASKED]", redacted.answer());
        assertTrue(Boolean.TRUE.equals(redacted.metadata().get("plugin.safety_filter.answer_redacted")));
        assertFalse(redacted.answer().contains("token"));
    }

    private static RagPluginExecutionContext context(String query) {
        return new RagPluginExecutionContext(
                "tenant-a",
                query,
                5,
                0.2f,
                Map.of(),
                GenerationConfig.defaults(),
                false);
    }

    private static RagScoredChunk scoredChunk(String id, String text, double score) {
        return new RagScoredChunk(new RagChunk(id, "doc-" + id, 0, text, Map.of()), score);
    }
}
