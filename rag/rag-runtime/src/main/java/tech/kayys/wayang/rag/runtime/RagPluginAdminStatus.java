package tech.kayys.wayang.rag.runtime;

import java.time.Instant;
import java.util.List;

public record RagPluginAdminStatus(
                String tenantId,
                RagPluginTenantStrategyResolution strategy,
                List<RagPluginManager.PluginInspection> plugins,
                List<String> activePluginIds,
                Instant observedAt) {
}
