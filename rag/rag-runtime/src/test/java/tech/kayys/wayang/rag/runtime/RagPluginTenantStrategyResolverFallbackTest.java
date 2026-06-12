package tech.kayys.wayang.rag.runtime;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.rag.plugin.api.RagPipelinePlugin;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RagPluginTenantStrategyResolverFallbackTest {

    @Test
    void fallsBackToDefaultResolutionWhenSelectedStrategyReturnsNullResolution() {
        RagRuntimeConfig config = new RagRuntimeConfig();
        config.setRagPluginSelectionStrategy("custom");
        config.setRagPluginEnabledIds("a");

        RagPluginTenantStrategyResolver resolver = new RagPluginTenantStrategyResolver(
                config,
                List.of(new NullResolutionStrategy()));

        RagPluginTenantStrategyResolution resolution = resolver.resolve("tenant");

        assertEquals("config", resolution.strategyId());
        assertEquals("a", resolution.effectiveEnabledIds());
    }

    @Test
    void fallsBackToDefaultSelectionWhenSelectedStrategyReturnsNullPlugins() {
        RagRuntimeConfig config = new RagRuntimeConfig();
        config.setRagPluginSelectionStrategy("null-list");
        config.setRagPluginEnabledIds("a");

        RagPluginTenantStrategyResolver resolver = new RagPluginTenantStrategyResolver(
                config,
                List.of(new NullPluginListStrategy()));

        List<RagPipelinePlugin> selected = resolver.selectActivePlugins(
                List.of(new NoopPlugin("a", 10), new NoopPlugin("b", 20)),
                "tenant");

        assertEquals(List.of("a"), selected.stream().map(RagPipelinePlugin::id).toList());
    }

    private record NoopPlugin(String id, int order) implements RagPipelinePlugin {
    }

    private static final class NullResolutionStrategy extends NullPluginListStrategy {

        @Override
        public String id() {
            return "custom";
        }

        @Override
        public RagPluginTenantStrategyResolution resolve(String tenantId, RagRuntimeConfig config) {
            return null;
        }
    }

    private static class NullPluginListStrategy implements RagPluginSelectionStrategy {

        @Override
        public String id() {
            return "null-list";
        }

        @Override
        public RagPluginTenantStrategyResolution resolve(String tenantId, RagRuntimeConfig config) {
            return new RagPluginTenantStrategyResolution(
                    tenantId,
                    config.getRagPluginEnabledIds(),
                    config.getRagPluginOrder(),
                    config.getRagPluginTenantEnabledOverrides(),
                    config.getRagPluginTenantOrderOverrides(),
                    null,
                    null,
                    config.getRagPluginEnabledIds(),
                    config.getRagPluginOrder(),
                    id());
        }

        @Override
        public List<RagPipelinePlugin> selectActivePlugins(
                List<RagPipelinePlugin> discovered,
                String tenantId,
                RagRuntimeConfig config,
                RagPluginTenantStrategyResolution resolution) {
            return null;
        }
    }
}
