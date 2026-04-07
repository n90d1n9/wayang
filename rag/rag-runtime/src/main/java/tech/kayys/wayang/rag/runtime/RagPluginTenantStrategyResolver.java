package tech.kayys.wayang.rag.runtime;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import tech.kayys.wayang.rag.plugin.api.RagPipelinePlugin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@ApplicationScoped
public class RagPluginTenantStrategyResolver {

    private static final Logger LOG = LoggerFactory.getLogger(RagPluginTenantStrategyResolver.class);

    private final RagRuntimeConfig config;
    private final Map<String, RagPluginSelectionStrategy> strategiesById;
    private final RagPluginSelectionStrategy defaultStrategy;

    @Inject
    public RagPluginTenantStrategyResolver(
            RagRuntimeConfig config,
            Instance<RagPluginSelectionStrategy> strategyInstances) {
        this.config = config;
        this.defaultStrategy = new ConfigRagPluginSelectionStrategy();
        this.strategiesById = indexStrategies(strategyInstances, defaultStrategy);
    }

    public RagPluginTenantStrategyResolver(RagRuntimeConfig config) {
        this.config = config;
        this.defaultStrategy = new ConfigRagPluginSelectionStrategy();
        this.strategiesById = indexStrategies(null, defaultStrategy);
    }

    public RagPluginTenantStrategyResolution resolve(String tenantId) {
        String tenantKey = normalizeTenant(tenantId);
        RagPluginSelectionStrategy selected = strategyForConfig();
        RagPluginTenantStrategyResolution resolved = selected.resolve(tenantKey, config);
        if (resolved == null) {
            LOG.warn("RAG plugin selection strategy {} returned null resolution. Falling back to {}.",
                    selected.id(), defaultStrategy.id());
            resolved = defaultStrategy.resolve(tenantKey, config);
        }
        return withStrategyId(resolved, selected.id());
    }

    public List<RagPipelinePlugin> selectActivePlugins(List<RagPipelinePlugin> discovered, String tenantId) {
        if (discovered == null || discovered.isEmpty()) {
            return List.of();
        }

        String tenantKey = normalizeTenant(tenantId);
        RagPluginSelectionStrategy selected = strategyForConfig();
        RagPluginTenantStrategyResolution resolution = withStrategyId(resolve(tenantKey), selected.id());
        List<RagPipelinePlugin> active = selected.selectActivePlugins(discovered, tenantKey, config, resolution);
        if (active == null) {
            LOG.warn("RAG plugin selection strategy {} returned null plugin list. Falling back to {}.",
                    selected.id(), defaultStrategy.id());
            return defaultStrategy.selectActivePlugins(discovered, tenantKey, config, resolution);
        }
        return List.copyOf(active);
    }

    public static Map<String, String> parseTenantOverrides(String raw) {
        return ConfigRagPluginSelectionStrategy.parseTenantOverrides(raw);
    }

    public static java.util.Set<String> parseEnabledPluginIds(String raw) {
        return ConfigRagPluginSelectionStrategy.parseEnabledPluginIds(raw);
    }

    public List<String> availableStrategyIds() {
        return strategiesById.keySet().stream().toList();
    }

    public boolean isKnownStrategy(String strategyId) {
        String normalized = normalizeStrategyId(strategyId);
        return strategiesById.containsKey(normalized);
    }

    private RagPluginSelectionStrategy strategyForConfig() {
        String requested = normalizeStrategyId(config.getRagPluginSelectionStrategy());
        RagPluginSelectionStrategy selected = strategiesById.get(requested);
        if (selected != null) {
            return selected;
        }
        if (!requested.isBlank()) {
            LOG.warn("Unknown RAG plugin selection strategy '{}'. Falling back to '{}'.",
                    requested, defaultStrategy.id());
        }
        return defaultStrategy;
    }

    private static Map<String, RagPluginSelectionStrategy> indexStrategies(
            Instance<RagPluginSelectionStrategy> strategyInstances,
            RagPluginSelectionStrategy fallback) {
        Map<String, RagPluginSelectionStrategy> indexed = new LinkedHashMap<>();
        if (strategyInstances != null) {
            for (RagPluginSelectionStrategy strategy : strategyInstances) {
                if (strategy == null || strategy.id() == null || strategy.id().isBlank()) {
                    continue;
                }
                indexed.put(normalizeStrategyId(strategy.id()), strategy);
            }
        }
        indexed.putIfAbsent(normalizeStrategyId(fallback.id()), fallback);
        return Map.copyOf(indexed);
    }

    private static RagPluginTenantStrategyResolution withStrategyId(
            RagPluginTenantStrategyResolution input,
            String strategyId) {
        String normalized = normalizeStrategyId(strategyId);
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

    private static String normalizeTenant(String tenantId) {
        if (tenantId == null) {
            return "";
        }
        return tenantId.trim();
    }

    private static String normalizeStrategyId(String strategyId) {
        if (strategyId == null) {
            return "";
        }
        return strategyId.trim().toLowerCase(Locale.ROOT);
    }
}
