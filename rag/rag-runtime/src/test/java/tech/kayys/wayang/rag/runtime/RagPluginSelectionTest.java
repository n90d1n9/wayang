package tech.kayys.wayang.rag.runtime;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.rag.plugin.api.RagPipelinePlugin;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RagPluginSelectionTest {

    @Test
    void filtersEligiblePluginsByEnabledIdsAndTenantSupport() {
        RagPipelinePlugin pluginA = new TestPlugin("Plugin-A", 30, null, false);
        RagPipelinePlugin pluginB = new TestPlugin("plugin-b", 10, "tenant-x", false);
        RagPipelinePlugin pluginC = new TestPlugin("plugin-c", 20, null, false);
        RagPipelinePlugin failing = new TestPlugin("failing", 40, null, true);

        List<RagPipelinePlugin> selected = RagPluginSelection.eligiblePlugins(
                List.of(pluginA, pluginB, pluginC, failing),
                " tenant-x ",
                Set.of("plugin-a", "plugin-b", "failing"));

        assertEquals(List.of("Plugin-A", "plugin-b"), selected.stream().map(RagPipelinePlugin::id).toList());
    }

    @Test
    void appliesConfiguredOrderAndLeavesUnmentionedPluginsInDiscoveryOrder() {
        RagPipelinePlugin pluginA = new TestPlugin("a", 30, null, false);
        RagPipelinePlugin pluginB = new TestPlugin("b", 10, null, false);
        RagPipelinePlugin pluginC = new TestPlugin("c", 20, null, false);

        List<RagPipelinePlugin> ordered = RagPluginSelection.applyOrder(
                List.of(pluginA, pluginB, pluginC),
                " c, a ");

        assertEquals(List.of("c", "a", "b"), ordered.stream().map(RagPipelinePlugin::id).toList());
    }

    @Test
    void exposesInspectionPredicatesWithNormalizedIdsAndSafeTenantSupport() {
        RagPipelinePlugin plugin = new TestPlugin(" Plugin-A ", 10, null, false);
        RagPipelinePlugin failing = new TestPlugin("failing", 20, null, true);

        assertTrue(RagPluginSelection.isEnabled(plugin, Set.of("plugin-a")));
        assertTrue(RagPluginSelection.isEnabled(plugin, Set.of(RagPluginSelectionConfig.WILDCARD_PLUGIN_ID)));
        assertTrue(RagPluginSelection.supportsTenant(plugin, "tenant"));
        assertFalse(RagPluginSelection.supportsTenant(failing, "tenant"));
        assertEquals(Set.of("plugin-a"), RagPluginSelection.pluginIds(List.of(plugin)));
    }

    private record TestPlugin(String id, int order, String tenantId, boolean failTenantCheck)
            implements RagPipelinePlugin {

        @Override
        public boolean supportsTenant(String tenantId) {
            if (failTenantCheck) {
                throw new IllegalStateException("tenant check failed");
            }
            return this.tenantId == null || this.tenantId.equals(tenantId);
        }
    }
}
