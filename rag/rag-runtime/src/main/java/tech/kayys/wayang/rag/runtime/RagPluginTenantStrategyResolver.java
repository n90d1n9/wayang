package tech.kayys.wayang.rag.runtime;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import tech.kayys.wayang.rag.plugin.api.RagPipelinePlugin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@ApplicationScoped
public class RagPluginTenantStrategyResolver {

    private static final Logger LOG = LoggerFactory.getLogger(RagPluginTenantStrategyResolver.class);

    private final RagRuntimeConfig config;
    private final RagPluginStrategyRegistry strategyRegistry;

    @Inject
    public RagPluginTenantStrategyResolver(
            RagRuntimeConfig config,
            Instance<RagPluginSelectionStrategy> strategyInstances) {
        this(config, (Iterable<RagPluginSelectionStrategy>) strategyInstances);
    }

    public RagPluginTenantStrategyResolver(RagRuntimeConfig config) {
        this(config, List.of());
    }

    RagPluginTenantStrategyResolver(
            RagRuntimeConfig config,
            Iterable<RagPluginSelectionStrategy> strategyInstances) {
        this.config = Objects.requireNonNull(config, "config");
        this.strategyRegistry = RagPluginStrategyRegistry.from(
                strategyInstances,
                new ConfigRagPluginSelectionStrategy());
    }

    public RagPluginTenantStrategyResolution resolve(String tenantId) {
        return resolveSelection(tenantId).resolution();
    }

    public List<RagPipelinePlugin> selectActivePlugins(List<RagPipelinePlugin> discovered, String tenantId) {
        if (discovered == null || discovered.isEmpty()) {
            return List.of();
        }

        String tenantKey = RagPluginSelectionConfig.normalizeTenant(tenantId);
        ResolvedPluginStrategy selected = resolveSelection(tenantKey);
        List<RagPipelinePlugin> active = selected.strategy()
                .selectActivePlugins(discovered, tenantKey, config, selected.resolution());
        if (active == null) {
            LOG.warn("RAG plugin selection strategy {} returned null plugin list. Falling back to {}.",
                    selected.strategy().id(), strategyRegistry.defaultStrategy().id());
            ResolvedPluginStrategy fallback = resolveDefaultSelection(tenantKey);
            return fallback.strategy().selectActivePlugins(
                    discovered,
                    tenantKey,
                    config,
                    fallback.resolution());
        }
        return RagRuntimeLists.copy(active);
    }

    public static Map<String, String> parseTenantOverrides(String raw) {
        return RagPluginSelectionConfig.parseTenantOverrides(raw);
    }

    public static java.util.Set<String> parseEnabledPluginIds(String raw) {
        return RagPluginSelectionConfig.parseEnabledPluginIds(raw);
    }

    public List<String> availableStrategyIds() {
        return strategyRegistry.strategyIds();
    }

    public boolean isKnownStrategy(String strategyId) {
        return strategyRegistry.contains(strategyId);
    }

    private RagPluginSelectionStrategy strategyForConfig() {
        String requested = RagPluginSelectionConfig.normalizeStrategyId(config.getRagPluginSelectionStrategy());
        if (!requested.isBlank() && !strategyRegistry.contains(requested)) {
            LOG.warn("Unknown RAG plugin selection strategy '{}'. Falling back to '{}'.",
                    requested, strategyRegistry.defaultStrategy().id());
        }
        return strategyRegistry.strategyFor(requested);
    }

    private ResolvedPluginStrategy resolveSelection(String tenantId) {
        String tenantKey = RagPluginSelectionConfig.normalizeTenant(tenantId);
        RagPluginSelectionStrategy selected = strategyForConfig();
        RagPluginTenantStrategyResolution resolved = selected.resolve(tenantKey, config);
        if (resolved != null) {
            return new ResolvedPluginStrategy(selected, withStrategyId(resolved, selected.id()));
        }

        LOG.warn("RAG plugin selection strategy {} returned null resolution. Falling back to {}.",
                selected.id(), strategyRegistry.defaultStrategy().id());
        return resolveDefaultSelection(tenantKey);
    }

    private ResolvedPluginStrategy resolveDefaultSelection(String tenantId) {
        String tenantKey = RagPluginSelectionConfig.normalizeTenant(tenantId);
        RagPluginSelectionStrategy fallback = strategyRegistry.defaultStrategy();
        RagPluginTenantStrategyResolution resolved = fallback.resolve(tenantKey, config);
        return new ResolvedPluginStrategy(fallback, withStrategyId(resolved, fallback.id()));
    }

    private static RagPluginTenantStrategyResolution withStrategyId(
            RagPluginTenantStrategyResolution input,
            String strategyId) {
        String normalized = RagPluginSelectionConfig.normalizeStrategyId(strategyId);
        if (Objects.equals(input.strategyId(), normalized)) {
            return input;
        }
        return new RagPluginTenantStrategyResolution(
                input.tenantId(),
                input.globalEnabledIds(),
                input.globalOrder(),
                input.tenantEnabledOverridesRaw(),
                input.tenantOrderOverridesRaw(),
                input.matchedTenantEnabledOverride(),
                input.matchedTenantOrderOverride(),
                input.effectiveEnabledIds(),
                input.effectiveOrder(),
                normalized);
    }

    private record ResolvedPluginStrategy(
            RagPluginSelectionStrategy strategy,
            RagPluginTenantStrategyResolution resolution) {
    }
}
