package tech.kayys.wayang.rag.runtime;

import java.util.List;

import tech.kayys.wayang.rag.plugin.api.RagPipelinePlugin;

public interface RagPluginSelectionStrategy {

    String id();

    RagPluginTenantStrategyResolution resolve(String tenantId, RagRuntimeConfig config);

    List<RagPipelinePlugin> selectActivePlugins(
            List<RagPipelinePlugin> discovered,
            String tenantId,
            RagRuntimeConfig config,
            RagPluginTenantStrategyResolution resolution);
}
