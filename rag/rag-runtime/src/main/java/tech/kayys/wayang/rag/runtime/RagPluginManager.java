package tech.kayys.wayang.rag.runtime;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.kayys.wayang.rag.plugin.api.RagPipelinePlugin;
import tech.kayys.wayang.rag.plugin.api.RagPluginExecutionContext;
import tech.kayys.wayang.rag.core.RagResult;
import tech.kayys.wayang.rag.core.RagScoredChunk;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Manager service for RAG pipeline plugins.
 * Handles plugin discovery via {@link RagPluginCatalog}, tenant-specific
 * strategy resolution
 * via {@link RagPluginTenantStrategyResolver}, and execution of plugin hooks at
 * various
 * stages of the RAG pipeline (before query, after retrieval, after result).
 */
@ApplicationScoped
public class RagPluginManager {

    private static final Logger LOG = LoggerFactory.getLogger(RagPluginManager.class);

    private final RagPluginCatalog pluginCatalog;
    private final RagPluginTenantStrategyResolver strategyResolver;

    @Inject
    public RagPluginManager(RagPluginCatalog pluginCatalog, RagPluginTenantStrategyResolver strategyResolver) {
        this.pluginCatalog = Objects.requireNonNull(pluginCatalog, "pluginCatalog");
        this.strategyResolver = Objects.requireNonNull(strategyResolver, "strategyResolver");
    }

    public RagPluginManager(RagRuntimeConfig config, List<RagPipelinePlugin> testPlugins) {
        Objects.requireNonNull(config, "config");
        this.pluginCatalog = new RagPluginCatalog(testPlugins);
        this.strategyResolver = new RagPluginTenantStrategyResolver(config);
    }

    public RagPluginExecutionContext applyBeforeQuery(RagPluginExecutionContext context) {
        RagPluginExecutionContext current = context;
        for (RagPipelinePlugin plugin : activePlugins(context.tenantId())) {
            try {
                RagPluginExecutionContext updated = plugin.beforeQuery(current);
                current = updated == null ? current : updated;
            } catch (RuntimeException ex) {
                LOG.warn("RAG plugin {} failed in beforeQuery hook. Continuing without plugin mutation.", plugin.id(),
                        ex);
            }
        }
        return current;
    }

    public List<RagScoredChunk> applyAfterRetrieve(
            RagPluginExecutionContext context,
            List<RagScoredChunk> chunks) {
        List<RagScoredChunk> current = chunks == null ? List.of() : List.copyOf(chunks);
        for (RagPipelinePlugin plugin : activePlugins(context.tenantId())) {
            try {
                List<RagScoredChunk> updated = plugin.afterRetrieve(context, current);
                current = updated == null ? current : List.copyOf(updated);
            } catch (RuntimeException ex) {
                LOG.warn("RAG plugin {} failed in afterRetrieve hook. Keeping previous chunks.", plugin.id(), ex);
            }
        }
        return current;
    }

    public RagResult applyAfterResult(RagPluginExecutionContext context, RagResult result) {
        RagResult current = result;
        for (RagPipelinePlugin plugin : activePlugins(context.tenantId())) {
            try {
                RagResult updated = plugin.afterResult(context, current);
                current = updated == null ? current : updated;
            } catch (RuntimeException ex) {
                LOG.warn("RAG plugin {} failed in afterResult hook. Keeping previous result.", plugin.id(), ex);
            }
        }
        return current;
    }

    public List<String> activePluginIds(String tenantId) {
        return activePlugins(tenantId).stream()
                .map(RagPipelinePlugin::id)
                .toList();
    }

    public RagPluginTenantStrategyResolution resolveTenantStrategy(String tenantId) {
        return strategyResolver.resolve(tenantId);
    }

    public List<PluginInspection> inspectPlugins(String tenantId) {
        List<RagPipelinePlugin> discovered = pluginCatalog.discover();
        if (discovered.isEmpty()) {
            return List.of();
        }

        List<RagPipelinePlugin> activePlugins = activePlugins(tenantId);
        java.util.Set<String> active = activePlugins.stream()
                .map(plugin -> RagPluginCatalog.normalizeId(plugin.id()))
                .collect(Collectors.toUnmodifiableSet());
        RagPluginTenantStrategyResolution strategy = resolveTenantStrategy(tenantId);
        java.util.Set<String> enabled = RagPluginTenantStrategyResolver
                .parseEnabledPluginIds(strategy.effectiveEnabledIds());
        boolean allEnabled = enabled.isEmpty() || enabled.contains("*");

        List<PluginInspection> inspections = new ArrayList<>(discovered.size());
        for (RagPipelinePlugin plugin : discovered) {
            boolean enabledByConfig = allEnabled || enabled.contains(RagPluginCatalog.normalizeId(plugin.id()));
            boolean supportsTenant = safeSupportsTenant(plugin, tenantId);
            inspections.add(new PluginInspection(
                    plugin.id(),
                    plugin.order(),
                    enabledByConfig,
                    supportsTenant,
                    active.contains(RagPluginCatalog.normalizeId(plugin.id()))));
        }
        return List.copyOf(inspections);
    }

    List<RagPipelinePlugin> activePlugins(String tenantId) {
        List<RagPipelinePlugin> discovered = pluginCatalog.discover();
        if (discovered.isEmpty()) {
            return List.of();
        }
        return strategyResolver.selectActivePlugins(discovered, tenantId);
    }

    private static boolean safeSupportsTenant(RagPipelinePlugin plugin, String tenantId) {
        try {
            return plugin.supportsTenant(tenantId);
        } catch (RuntimeException ex) {
            return false;
        }
    }

    public record PluginInspection(
            String id,
            int order,
            boolean enabledByConfig,
            boolean supportsTenant,
            boolean active) {
    }
}
