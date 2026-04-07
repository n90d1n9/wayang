package tech.kayys.wayang.rag.runtime;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Instant;
import java.util.List;

@ApplicationScoped
public class RagPluginAdminService {

    @Inject
    RagPluginManager pluginManager;

    public RagPluginAdminStatus status(String tenantId) {
        String effectiveTenantId = tenantId == null ? "" : tenantId.trim();
        RagPluginTenantStrategyResolution strategy = pluginManager.resolveTenantStrategy(effectiveTenantId);
        List<RagPluginManager.PluginInspection> plugins = pluginManager.inspectPlugins(effectiveTenantId);
        List<String> activePluginIds = pluginManager.activePluginIds(effectiveTenantId);
        return new RagPluginAdminStatus(effectiveTenantId, strategy, plugins, activePluginIds, Instant.now());
    }
}
