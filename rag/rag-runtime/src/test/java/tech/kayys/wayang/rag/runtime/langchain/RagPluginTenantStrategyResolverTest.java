package tech.kayys.wayang.rag.runtime.langchain;

import org.junit.jupiter.api.Test;

import tech.kayys.wayang.rag.plugin.api.RagPipelinePlugin;
import tech.kayys.wayang.rag.runtime.RagPluginTenantStrategyResolution;
import tech.kayys.wayang.rag.runtime.RagPluginTenantStrategyResolver;
import tech.kayys.wayang.rag.runtime.RagRuntimeConfig;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RagPluginTenantStrategyResolverTest {

    @Test
    void shouldResolveTenantOverridesAndSelectOrderedPlugins() {
        RagRuntimeConfig config = new RagRuntimeConfig();
        config.setRagPluginEnabledIds("a,b,c");
        config.setRagPluginOrder("a,b,c");
        config.setRagPluginTenantEnabledOverrides("tenant-x=b,c");
        config.setRagPluginTenantOrderOverrides("tenant-x=c,b");

        RagPluginTenantStrategyResolver resolver = new RagPluginTenantStrategyResolver(config);

        RagPluginTenantStrategyResolution strategy = resolver.resolve("tenant-x");
        assertEquals("config", strategy.strategyId());
        assertEquals("b,c", strategy.effectiveEnabledIds());
        assertEquals("c,b", strategy.effectiveOrder());

        List<RagPipelinePlugin> selected = resolver.selectActivePlugins(
                List.of(new NoopPlugin("a", 10), new NoopPlugin("b", 20), new NoopPlugin("c", 30)),
                "tenant-x");

        assertEquals(List.of("c", "b"), selected.stream().map(RagPipelinePlugin::id).toList());
    }

    @Test
    void shouldParseTenantOverridesIgnoringMalformedEntries() {
        Map<String, String> parsed = RagPluginTenantStrategyResolver.parseTenantOverrides(
                "tenant-a=a,b; ;bad;tenant-b=c;tenant-c=");

        assertEquals(2, parsed.size());
        assertEquals("a,b", parsed.get("tenant-a"));
        assertEquals("c", parsed.get("tenant-b"));
    }

    @Test
    void shouldDefaultEnabledToWildcardWhenMissing() {
        assertTrue(RagPluginTenantStrategyResolver.parseEnabledPluginIds(null).contains("*"));
    }

    @Test
    void shouldExposeKnownStrategies() {
        RagRuntimeConfig config = new RagRuntimeConfig();
        RagPluginTenantStrategyResolver resolver = new RagPluginTenantStrategyResolver(config);
        assertTrue(resolver.isKnownStrategy("config"));
        assertFalse(resolver.isKnownStrategy("missing"));
        assertTrue(resolver.availableStrategyIds().contains("config"));
    }

    private static final class NoopPlugin implements RagPipelinePlugin {
        private final String id;
        private final int order;

        private NoopPlugin(String id, int order) {
            this.id = id;
            this.order = order;
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public int order() {
            return order;
        }
    }
}
