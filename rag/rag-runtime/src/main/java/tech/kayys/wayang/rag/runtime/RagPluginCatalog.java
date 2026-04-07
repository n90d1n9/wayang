package tech.kayys.wayang.rag.runtime;

import tech.kayys.wayang.rag.plugin.api.*;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

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
        this.testPlugins = testPlugins == null ? List.of() : List.copyOf(testPlugins);
    }

    public List<RagPipelinePlugin> discover() {
        if (pluginInstances != null) {
            return pluginInstances.stream()
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparingInt(RagPipelinePlugin::order)
                            .thenComparing(plugin -> normalizeId(plugin.id())))
                    .toList();
        }
        return testPlugins.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingInt(RagPipelinePlugin::order)
                        .thenComparing(plugin -> normalizeId(plugin.id())))
                .toList();
    }

    static String normalizeId(String id) {
        if (id == null) {
            return "";
        }
        return id.trim().toLowerCase(Locale.ROOT);
    }
}
