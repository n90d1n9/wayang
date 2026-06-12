package tech.kayys.wayang.rag.runtime;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.rag.plugin.api.RagPipelinePlugin;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RagPluginDiscoveryTest {

    @Test
    void ordersPluginsByOrderThenNormalizedIdAndSkipsNulls() {
        List<RagPipelinePlugin> ordered = RagPluginDiscovery.ordered(Arrays.asList(
                plugin("Beta", 20),
                null,
                plugin(" alpha ", 10),
                plugin("Gamma", 20),
                plugin("delta", 10)).stream());

        assertEquals(List.of(" alpha ", "delta", "Beta", "Gamma"),
                ordered.stream().map(RagPipelinePlugin::id).toList());
        assertThrows(UnsupportedOperationException.class, () -> ordered.add(plugin("other", 30)));
    }

    @Test
    void normalizesIdsForCaseInsensitiveSelection() {
        assertEquals("plugin-a", RagPluginDiscovery.normalizeId(" Plugin-A "));
        assertEquals("", RagPluginDiscovery.normalizeId(null));
    }

    private static RagPipelinePlugin plugin(String id, int order) {
        return new TestPlugin(id, order);
    }

    private record TestPlugin(String id, int order) implements RagPipelinePlugin {
    }
}
