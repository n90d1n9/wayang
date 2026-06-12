package tech.kayys.wayang.rag.runtime;

import tech.kayys.wayang.rag.plugin.api.RagPipelinePlugin;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import java.util.List;

@ApplicationScoped
public class RagPluginCatalog {

    private final Instance<RagPipelinePlugin> pluginInstances;
    private final List<RagPipelinePlugin> testPlugins;

    @Inject
    public RagPluginCatalog(Instance<RagPipelinePlugin> pluginInstances) {
        this.pluginInstances = pluginInstances;
        this.testPlugins = null;
    }

    RagPluginCatalog(List<RagPipelinePlugin> testPlugins) {
        this.pluginInstances = null;
        this.testPlugins = RagRuntimeLists.copy(testPlugins);
    }

    public List<RagPipelinePlugin> discover() {
        if (pluginInstances != null) {
            return RagPluginDiscovery.ordered(pluginInstances.stream());
        }
        return RagPluginDiscovery.ordered(testPlugins.stream());
    }

    static String normalizeId(String id) {
        return RagPluginDiscovery.normalizeId(id);
    }
}
