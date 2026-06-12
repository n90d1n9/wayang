package tech.kayys.wayang.rag.runtime;

import tech.kayys.wayang.rag.plugin.api.RagPipelinePlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

final class RagPluginInspector {

    private RagPluginInspector() {
    }

    static List<String> activePluginIds(List<RagPipelinePlugin> activePlugins) {
        if (activePlugins == null || activePlugins.isEmpty()) {
            return List.of();
        }
        return activePlugins.stream()
                .filter(plugin -> plugin != null)
                .map(RagPipelinePlugin::id)
                .toList();
    }

    static List<RagPluginInspection> inspect(
            List<RagPipelinePlugin> discovered,
            List<RagPipelinePlugin> activePlugins,
            RagPluginTenantStrategyResolution strategy,
            String tenantId) {
        if (discovered == null || discovered.isEmpty()) {
            return List.of();
        }

        Set<String> active = RagPluginSelection.pluginIds(activePlugins);
        Set<String> enabled = RagPluginSelectionConfig.parseEnabledPluginIds(
                strategy == null ? null : strategy.effectiveEnabledIds());

        List<RagPluginInspection> inspections = new ArrayList<>(discovered.size());
        for (RagPipelinePlugin plugin : discovered) {
            if (plugin == null) {
                continue;
            }
            inspections.add(new RagPluginInspection(
                    plugin.id(),
                    plugin.order(),
                    RagPluginSelection.isEnabled(plugin, enabled),
                    RagPluginSelection.supportsTenant(plugin, tenantId),
                    active.contains(RagPluginCatalog.normalizeId(plugin.id()))));
        }
        return RagRuntimeLists.copy(inspections);
    }
}
