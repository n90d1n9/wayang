package tech.kayys.wayang.rag.runtime;

import tech.kayys.wayang.rag.plugin.api.RagPipelinePlugin;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

final class RagPluginSelection {

    private RagPluginSelection() {
    }

    static List<RagPipelinePlugin> eligiblePlugins(
            List<RagPipelinePlugin> discovered,
            String tenantId,
            Set<String> enabledPluginIds) {
        if (discovered == null || discovered.isEmpty()) {
            return List.of();
        }
        String tenantKey = RagPluginSelectionConfig.normalizeTenant(tenantId);
        return discovered.stream()
                .filter(plugin -> isEnabled(plugin, enabledPluginIds))
                .filter(plugin -> supportsTenant(plugin, tenantKey))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    static List<RagPipelinePlugin> applyOrder(List<RagPipelinePlugin> selected, String orderRaw) {
        if (selected == null || selected.isEmpty()) {
            return List.of();
        }
        List<String> order = RagPluginSelectionConfig.parsePluginOrder(orderRaw);
        if (order.isEmpty()) {
            return RagRuntimeLists.copy(selected);
        }

        Map<String, RagPipelinePlugin> pluginById = new LinkedHashMap<>();
        for (RagPipelinePlugin plugin : selected) {
            if (plugin == null) {
                continue;
            }
            pluginById.put(RagPluginCatalog.normalizeId(plugin.id()), plugin);
        }

        List<RagPipelinePlugin> reordered = new ArrayList<>(selected.size());
        for (String id : order) {
            RagPipelinePlugin plugin = pluginById.remove(id);
            if (plugin != null) {
                reordered.add(plugin);
            }
        }
        reordered.addAll(pluginById.values());
        return RagRuntimeLists.copy(reordered);
    }

    static Set<String> pluginIds(List<RagPipelinePlugin> plugins) {
        if (plugins == null || plugins.isEmpty()) {
            return Set.of();
        }
        return plugins.stream()
                .filter(plugin -> plugin != null)
                .map(plugin -> RagPluginCatalog.normalizeId(plugin.id()))
                .collect(Collectors.toUnmodifiableSet());
    }

    static boolean isEnabled(RagPipelinePlugin plugin, Set<String> enabledPluginIds) {
        if (plugin == null) {
            return false;
        }
        return RagPluginSelectionConfig.allPluginsEnabled(enabledPluginIds)
                || enabledPluginIds.contains(RagPluginCatalog.normalizeId(plugin.id()));
    }

    static boolean supportsTenant(RagPipelinePlugin plugin, String tenantId) {
        if (plugin == null) {
            return false;
        }
        try {
            return plugin.supportsTenant(RagPluginSelectionConfig.normalizeTenant(tenantId));
        } catch (RuntimeException ex) {
            return false;
        }
    }
}
