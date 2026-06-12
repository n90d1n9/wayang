package tech.kayys.wayang.rag.runtime;

import java.time.Instant;
import java.util.List;

public record RagPluginAdminStatus(
                String tenantId,
                RagPluginTenantStrategyResolution strategy,
                List<RagPluginInspection> plugins,
                List<String> activePluginIds,
                Instant observedAt) {

    public RagPluginAdminStatus {
        tenantId = RagPluginSelectionConfig.normalizeTenant(tenantId);
        plugins = RagRuntimeLists.copy(plugins);
        activePluginIds = RagRuntimeLists.copy(activePluginIds);
        observedAt = observedAt == null ? Instant.now() : observedAt;
    }
}
