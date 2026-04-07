package tech.kayys.wayang.rag.runtime;

import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.rag.plugin.api.RagPipelinePlugin;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class ConfigRagPluginSelectionStrategy implements RagPluginSelectionStrategy {

    static final String STRATEGY_ID = "config";

    @Override
    public String id() {
        return STRATEGY_ID;
    }

    @Override
    public RagPluginTenantStrategyResolution resolve(String tenantId, RagRuntimeConfig config) {
        String tenantKey = normalizeTenant(tenantId);
        String globalEnabled = config.getRagPluginEnabledIds();
        String globalOrder = config.getRagPluginOrder();
        String tenantEnabledRaw = config.getRagPluginTenantEnabledOverrides();
        String tenantOrderRaw = config.getRagPluginTenantOrderOverrides();

        Map<String, String> enabledOverrides = parseTenantOverrides(tenantEnabledRaw);
        Map<String, String> orderOverrides = parseTenantOverrides(tenantOrderRaw);

        String matchedEnabled = tenantKey.isEmpty() ? null : enabledOverrides.get(tenantKey);
        String matchedOrder = tenantKey.isEmpty() ? null : orderOverrides.get(tenantKey);

        String effectiveEnabled = matchedEnabled == null ? globalEnabled : matchedEnabled;
        String effectiveOrder = matchedOrder == null ? globalOrder : matchedOrder;

        return new RagPluginTenantStrategyResolution(
                tenantKey,
                globalEnabled,
                globalOrder,
                tenantEnabledRaw,
                tenantOrderRaw,
                matchedEnabled,
                matchedOrder,
                effectiveEnabled,
                effectiveOrder,
                STRATEGY_ID);
    }

    @Override
    public List<RagPipelinePlugin> selectActivePlugins(
            List<RagPipelinePlugin> discovered,
            String tenantId,
            RagRuntimeConfig config,
            RagPluginTenantStrategyResolution resolution) {
        if (discovered == null || discovered.isEmpty()) {
            return List.of();
        }

        String tenantKey = normalizeTenant(tenantId);
        RagPluginTenantStrategyResolution effective = resolution == null ? resolve(tenantId, config) : resolution;
        String enabledRaw = effective.effectiveEnabledIds();
        String orderRaw = effective.effectiveOrder();

        Set<String> enabled = parseEnabledPluginIds(enabledRaw);
        boolean allEnabled = enabled.isEmpty() || enabled.contains("*");

        List<RagPipelinePlugin> selected = discovered.stream()
                .filter(plugin -> allEnabled || enabled.contains(RagPluginCatalog.normalizeId(plugin.id())))
                .filter(plugin -> safeSupportsTenant(plugin, tenantKey))
                .collect(Collectors.toCollection(ArrayList::new));

        if (selected.isEmpty()) {
            return List.of();
        }

        return applyOrderOverrides(selected, orderRaw);
    }

    static Map<String, String> parseTenantOverrides(String raw) {
        if (raw == null || raw.isBlank()) {
            return Map.of();
        }
        Map<String, String> parsed = new LinkedHashMap<>();
        for (String token : raw.split(";")) {
            String entry = token == null ? "" : token.trim();
            if (entry.isEmpty()) {
                continue;
            }
            int delimiter = entry.indexOf('=');
            if (delimiter <= 0 || delimiter == entry.length() - 1) {
                continue;
            }
            String tenant = normalizeTenant(entry.substring(0, delimiter));
            String value = entry.substring(delimiter + 1).trim();
            if (!tenant.isEmpty() && !value.isEmpty()) {
                parsed.put(tenant, value);
            }
        }
        return Map.copyOf(parsed);
    }

    static Set<String> parseEnabledPluginIds(String raw) {
        if (raw == null || raw.isBlank()) {
            return Set.of("*");
        }
        return raw.lines()
                .flatMap(line -> java.util.Arrays.stream(line.split(",")))
                .map(String::trim)
                .filter(token -> !token.isEmpty())
                .map(RagPluginCatalog::normalizeId)
                .collect(Collectors.toUnmodifiableSet());
    }

    private static List<RagPipelinePlugin> applyOrderOverrides(List<RagPipelinePlugin> selected, String orderRaw) {
        if (orderRaw == null || orderRaw.isBlank()) {
            return List.copyOf(selected);
        }
        List<String> order = java.util.Arrays.stream(orderRaw.split(","))
                .map(String::trim)
                .filter(token -> !token.isEmpty())
                .map(RagPluginCatalog::normalizeId)
                .toList();
        if (order.isEmpty()) {
            return List.copyOf(selected);
        }

        Map<String, RagPipelinePlugin> pluginById = new LinkedHashMap<>();
        for (RagPipelinePlugin plugin : selected) {
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
        return List.copyOf(reordered);
    }

    private static boolean safeSupportsTenant(RagPipelinePlugin plugin, String tenantId) {
        try {
            return plugin.supportsTenant(tenantId);
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private static String normalizeTenant(String tenantId) {
        if (tenantId == null) {
            return "";
        }
        return tenantId.trim();
    }
}
