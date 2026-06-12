package tech.kayys.wayang.rag.runtime;

import tech.kayys.wayang.rag.plugin.api.RagPipelinePlugin;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

final class RagPluginDiscovery {

    private RagPluginDiscovery() {
    }

    static List<RagPipelinePlugin> ordered(Stream<RagPipelinePlugin> plugins) {
        if (plugins == null) {
            return List.of();
        }
        return plugins
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingInt(RagPipelinePlugin::order)
                        .thenComparing(plugin -> normalizeId(plugin.id())))
                .toList();
    }

    static String normalizeId(String id) {
        return RagRuntimeText.trimToLowerEmpty(id);
    }
}
