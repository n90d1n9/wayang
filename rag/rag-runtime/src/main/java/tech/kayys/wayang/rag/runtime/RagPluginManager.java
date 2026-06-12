package tech.kayys.wayang.rag.runtime;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.kayys.wayang.rag.plugin.api.RagPipelinePlugin;
import tech.kayys.wayang.rag.plugin.api.RagPluginExecutionContext;
import tech.kayys.wayang.rag.core.RagResult;
import tech.kayys.wayang.rag.core.RagScoredChunk;

import java.util.List;
import java.util.Objects;

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
        return RagPluginHooks.beforeQuery(activePlugins(context.tenantId()), context, LOG);
    }

    public List<RagScoredChunk> applyAfterRetrieve(
            RagPluginExecutionContext context,
            List<RagScoredChunk> chunks) {
        return RagPluginHooks.afterRetrieve(activePlugins(context.tenantId()), context, chunks, LOG);
    }

    public RagResult applyAfterResult(RagPluginExecutionContext context, RagResult result) {
        return RagPluginHooks.afterResult(activePlugins(context.tenantId()), context, result, LOG);
    }

    public List<String> activePluginIds(String tenantId) {
        return RagPluginInspector.activePluginIds(activePlugins(tenantId));
    }

    public RagPluginTenantStrategyResolution resolveTenantStrategy(String tenantId) {
        return strategyResolver.resolve(tenantId);
    }

    public List<RagPluginInspection> inspectPlugins(String tenantId) {
        List<RagPipelinePlugin> discovered = pluginCatalog.discover();
        if (discovered.isEmpty()) {
            return List.of();
        }

        List<RagPipelinePlugin> activePlugins = activePlugins(tenantId);
        RagPluginTenantStrategyResolution strategy = resolveTenantStrategy(tenantId);
        return RagPluginInspector.inspect(discovered, activePlugins, strategy, tenantId);
    }

    List<RagPipelinePlugin> activePlugins(String tenantId) {
        List<RagPipelinePlugin> discovered = pluginCatalog.discover();
        if (discovered.isEmpty()) {
            return List.of();
        }
        return strategyResolver.selectActivePlugins(discovered, tenantId);
    }

}
