package tech.kayys.wayang.rag.runtime;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.rag.plugin.api.RagPipelinePlugin;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RagPluginInspectorTest {

    @Test
    void returnsActivePluginIdsInActiveOrderSkippingNulls() {
        assertEquals(List.of("b", "a"), RagPluginInspector.activePluginIds(
                java.util.Arrays.asList(plugin("b"), null, plugin("a"))));
    }

    @Test
    void buildsInspectionRowsFromSelectionState() {
        RagPipelinePlugin active = plugin("a");
        RagPipelinePlugin tenantScoped = new TenantPlugin("b", "tenant-x");
        RagPipelinePlugin disabled = plugin("c");
        RagPluginTenantStrategyResolution strategy = strategy("a,b");

        List<RagPluginInspection> inspections = RagPluginInspector.inspect(
                List.of(active, tenantScoped, disabled),
                List.of(active),
                strategy,
                "tenant-y");

        Map<String, RagPluginInspection> byId = inspections.stream()
                .collect(Collectors.toMap(RagPluginInspection::id, Function.identity()));

        assertTrue(byId.get("a").enabledByConfig());
        assertTrue(byId.get("a").supportsTenant());
        assertTrue(byId.get("a").active());

        assertTrue(byId.get("b").enabledByConfig());
        assertFalse(byId.get("b").supportsTenant());
        assertFalse(byId.get("b").active());

        assertFalse(byId.get("c").enabledByConfig());
        assertTrue(byId.get("c").supportsTenant());
        assertFalse(byId.get("c").active());
    }

    private static RagPipelinePlugin plugin(String id) {
        return new TenantPlugin(id, null);
    }

    private static RagPluginTenantStrategyResolution strategy(String enabledIds) {
        return new RagPluginTenantStrategyResolution(
                "tenant-y",
                enabledIds,
                "",
                "",
                "",
                null,
                null,
                enabledIds,
                "",
                "config");
    }

    private record TenantPlugin(String id, String tenantId) implements RagPipelinePlugin {

        @Override
        public boolean supportsTenant(String tenantId) {
            return this.tenantId == null || this.tenantId.equals(tenantId);
        }
    }
}
