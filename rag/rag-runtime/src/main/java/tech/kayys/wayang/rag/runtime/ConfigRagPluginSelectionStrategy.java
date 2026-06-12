package tech.kayys.wayang.rag.runtime;

import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.rag.plugin.api.RagPipelinePlugin;

import java.util.List;
import java.util.Map;
import java.util.Set;

@ApplicationScoped
public class ConfigRagPluginSelectionStrategy implements RagPluginSelectionStrategy {

    static final String STRATEGY_ID = "config";

    @Override
    public String id() {
        return STRATEGY_ID;
    }

    @Override
    public RagPluginTenantStrategyResolution resolve(String tenantId, RagRuntimeConfig config) {
        String tenantKey = RagPluginSelectionConfig.normalizeTenant(tenantId);
        String globalEnabled = config.getRagPluginEnabledIds();
        String globalOrder = config.getRagPluginOrder();
        String tenantEnabledRaw = config.getRagPluginTenantEnabledOverrides();
        String tenantOrderRaw = config.getRagPluginTenantOrderOverrides();

        Map<String, String> enabledOverrides = RagPluginSelectionConfig.parseTenantOverrides(tenantEnabledRaw);
        Map<String, String> orderOverrides = RagPluginSelectionConfig.parseTenantOverrides(tenantOrderRaw);

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

        String tenantKey = RagPluginSelectionConfig.normalizeTenant(tenantId);
        RagPluginTenantStrategyResolution effective = resolution == null ? resolve(tenantId, config) : resolution;
        String enabledRaw = effective.effectiveEnabledIds();
        String orderRaw = effective.effectiveOrder();

        Set<String> enabled = RagPluginSelectionConfig.parseEnabledPluginIds(enabledRaw);
        List<RagPipelinePlugin> selected = RagPluginSelection.eligiblePlugins(discovered, tenantKey, enabled);

        if (selected.isEmpty()) {
            return List.of();
        }

        return RagPluginSelection.applyOrder(selected, orderRaw);
    }

    static Map<String, String> parseTenantOverrides(String raw) {
        return RagPluginSelectionConfig.parseTenantOverrides(raw);
    }

    static Set<String> parseEnabledPluginIds(String raw) {
        return RagPluginSelectionConfig.parseEnabledPluginIds(raw);
    }
}
