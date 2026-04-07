package tech.kayys.wayang.rag.runtime.langchain;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.rag.core.GenerationConfig;
import tech.kayys.wayang.rag.plugin.api.RagPipelinePlugin;
import tech.kayys.wayang.rag.plugin.api.RagPluginExecutionContext;
import tech.kayys.wayang.rag.runtime.RagPluginManager;
import tech.kayys.wayang.rag.runtime.RagPluginTenantStrategyResolution;
import tech.kayys.wayang.rag.runtime.RagRuntimeConfig;
import tech.kayys.wayang.rag.core.RagChunk;
import tech.kayys.wayang.rag.core.RagQuery;
import tech.kayys.wayang.rag.core.RagResult;
import tech.kayys.wayang.rag.core.RagScoredChunk;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RagPluginManagerTest {

    @Test
    void shouldApplyHooksInConfiguredOrderAndFilterByEnabledIds() {
        RagRuntimeConfig config = new RagRuntimeConfig();
        config.setRagPluginEnabledIds("b,a");
        config.setRagPluginOrder("a,b");

        RagPipelinePlugin pluginA = new TestPlugin("a", 200, "[A]");
        RagPipelinePlugin pluginB = new TestPlugin("b", 100, "[B]");
        RagPipelinePlugin pluginC = new TestPlugin("c", 50, "[C]");

        RagPluginManager manager = new RagPluginManager(config, List.of(pluginA, pluginB, pluginC));
        RagPluginExecutionContext context = new RagPluginExecutionContext(
                "tenant-1",
                "q",
                5,
                0.4f,
                Map.of(),
                GenerationConfig.defaults(),
                false);

        RagPluginExecutionContext before = manager.applyBeforeQuery(context);
        assertEquals("q[A][B]", before.query());

        List<RagScoredChunk> chunks = manager.applyAfterRetrieve(context, List.of());
        assertEquals(2, chunks.size());
        assertEquals("chunk-a", chunks.get(0).chunk().id());
        assertEquals("chunk-b", chunks.get(1).chunk().id());

        RagResult base = new RagResult(RagQuery.of("q"), List.of(), "answer", Map.of());
        RagResult after = manager.applyAfterResult(context, base);
        assertEquals("answer[A][B]", after.answer());
    }

    @Test
    void shouldFilterByTenantSupport() {
        RagRuntimeConfig config = new RagRuntimeConfig();
        config.setRagPluginEnabledIds("*");

        RagPipelinePlugin scoped = new RagPipelinePlugin() {
            @Override
            public String id() {
                return "tenant-only";
            }

            @Override
            public boolean supportsTenant(String tenantId) {
                return "tenant-x".equals(tenantId);
            }

            @Override
            public RagPluginExecutionContext beforeQuery(RagPluginExecutionContext context) {
                return context.withQuery(context.query() + "[TENANT]");
            }
        };

        RagPluginManager manager = new RagPluginManager(config, List.of(scoped));
        RagPluginExecutionContext contextX = new RagPluginExecutionContext(
                "tenant-x",
                "q",
                5,
                0.4f,
                Map.of(),
                GenerationConfig.defaults(),
                false);
        RagPluginExecutionContext contextY = new RagPluginExecutionContext(
                "tenant-y",
                "q",
                5,
                0.4f,
                Map.of(),
                GenerationConfig.defaults(),
                false);

        assertEquals("q[TENANT]", manager.applyBeforeQuery(contextX).query());
        assertEquals("q", manager.applyBeforeQuery(contextY).query());
    }

    @Test
    void shouldExposePluginInspectionAndActiveIds() {
        RagRuntimeConfig config = new RagRuntimeConfig();
        config.setRagPluginEnabledIds("b,a");
        config.setRagPluginOrder("a,b");

        RagPipelinePlugin tenantScoped = new RagPipelinePlugin() {
            @Override
            public String id() {
                return "b";
            }

            @Override
            public int order() {
                return 20;
            }

            @Override
            public boolean supportsTenant(String tenantId) {
                return "tenant-x".equals(tenantId);
            }
        };
        RagPipelinePlugin pluginA = new TestPlugin("a", 10, "[A]");
        RagPipelinePlugin pluginC = new TestPlugin("c", 30, "[C]");

        RagPluginManager manager = new RagPluginManager(config, List.of(pluginA, tenantScoped, pluginC));

        List<RagPluginManager.PluginInspection> inspection = manager.inspectPlugins("tenant-y");
        Map<String, RagPluginManager.PluginInspection> byId = inspection.stream()
                .collect(Collectors.toMap(RagPluginManager.PluginInspection::id, Function.identity()));

        assertTrue(byId.get("a").enabledByConfig());
        assertTrue(byId.get("a").supportsTenant());
        assertTrue(byId.get("a").active());

        assertTrue(byId.get("b").enabledByConfig());
        assertFalse(byId.get("b").supportsTenant());
        assertFalse(byId.get("b").active());

        assertFalse(byId.get("c").enabledByConfig());
        assertTrue(byId.get("c").supportsTenant());
        assertFalse(byId.get("c").active());

        assertEquals(List.of("a"), manager.activePluginIds("tenant-y"));
    }

    @Test
    void shouldApplyTenantStrategyOverrides() {
        RagRuntimeConfig config = new RagRuntimeConfig();
        config.setRagPluginEnabledIds("a,b,c");
        config.setRagPluginOrder("a,b,c");
        config.setRagPluginTenantEnabledOverrides("tenant-x=b,c");
        config.setRagPluginTenantOrderOverrides("tenant-x=c,b");

        RagPipelinePlugin pluginA = new TestPlugin("a", 100, "[A]");
        RagPipelinePlugin pluginB = new TestPlugin("b", 200, "[B]");
        RagPipelinePlugin pluginC = new TestPlugin("c", 300, "[C]");

        RagPluginManager manager = new RagPluginManager(config, List.of(pluginA, pluginB, pluginC));
        assertEquals(List.of("a", "b", "c"), manager.activePluginIds("tenant-y"));
        assertEquals(List.of("c", "b"), manager.activePluginIds("tenant-x"));

        RagPluginTenantStrategyResolution strategy = manager.resolveTenantStrategy("tenant-x");
        assertEquals("a,b,c", strategy.globalEnabledIds());
        assertEquals("a,b,c", strategy.globalOrder());
        assertEquals("b,c", strategy.matchedTenantEnabledOverride());
        assertEquals("c,b", strategy.matchedTenantOrderOverride());
        assertEquals("b,c", strategy.effectiveEnabledIds());
        assertEquals("c,b", strategy.effectiveOrder());
        assertEquals("config", strategy.strategyId());
    }

    private static final class TestPlugin implements RagPipelinePlugin {
        private final String id;
        private final int order;
        private final String marker;

        private TestPlugin(String id, int order, String marker) {
            this.id = id;
            this.order = order;
            this.marker = marker;
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public int order() {
            return order;
        }

        @Override
        public RagPluginExecutionContext beforeQuery(RagPluginExecutionContext context) {
            return context.withQuery(context.query() + marker);
        }

        @Override
        public List<RagScoredChunk> afterRetrieve(RagPluginExecutionContext context, List<RagScoredChunk> chunks) {
            RagChunk chunk = new RagChunk("chunk-" + id, "doc-" + id, 0, "text", Map.of());
            java.util.ArrayList<RagScoredChunk> updated = new java.util.ArrayList<>(chunks);
            updated.add(new RagScoredChunk(chunk, 0.8));
            return updated;
        }

        @Override
        public RagResult afterResult(RagPluginExecutionContext context, RagResult result) {
            return new RagResult(result.query(), result.chunks(), result.answer() + marker, result.metadata());
        }
    }
}
