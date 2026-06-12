package tech.kayys.wayang.rag.runtime;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.rag.plugin.api.RagPipelinePlugin;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RagPluginStrategyRegistryTest {

    @Test
    void indexesNormalizedStrategyIdsAndKeepsDefaultFallback() {
        RagPluginSelectionStrategy fallback = new Strategy("config");
        RagPluginSelectionStrategy custom = new Strategy(" Custom ");

        RagPluginStrategyRegistry registry = RagPluginStrategyRegistry.from(
                List.of(custom, new Strategy(" ")),
                fallback);

        assertTrue(registry.contains("custom"));
        assertTrue(registry.contains("CONFIG"));
        assertSame(custom, registry.strategyFor(" custom "));
        assertSame(fallback, registry.strategyFor("missing"));
        assertEquals(List.of("custom", "config"), registry.strategyIds());
        assertThrows(UnsupportedOperationException.class, () -> registry.strategyIds().add("other"));
    }

    private record Strategy(String id) implements RagPluginSelectionStrategy {

        @Override
        public RagPluginTenantStrategyResolution resolve(String tenantId, RagRuntimeConfig config) {
            return null;
        }

        @Override
        public List<RagPipelinePlugin> selectActivePlugins(
                List<RagPipelinePlugin> discovered,
                String tenantId,
                RagRuntimeConfig config,
                RagPluginTenantStrategyResolution resolution) {
            return List.of();
        }
    }
}
