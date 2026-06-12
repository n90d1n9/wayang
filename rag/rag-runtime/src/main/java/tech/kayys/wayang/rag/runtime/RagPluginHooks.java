package tech.kayys.wayang.rag.runtime;

import org.slf4j.Logger;
import tech.kayys.wayang.rag.core.RagResult;
import tech.kayys.wayang.rag.core.RagScoredChunk;
import tech.kayys.wayang.rag.plugin.api.RagPipelinePlugin;
import tech.kayys.wayang.rag.plugin.api.RagPluginExecutionContext;

import java.util.List;

final class RagPluginHooks {

    private RagPluginHooks() {
    }

    static RagPluginExecutionContext beforeQuery(
            List<RagPipelinePlugin> plugins,
            RagPluginExecutionContext context,
            Logger log) {
        RagPluginExecutionContext current = context;
        for (RagPipelinePlugin plugin : pluginsOrEmpty(plugins)) {
            if (plugin == null) {
                continue;
            }
            try {
                RagPluginExecutionContext updated = plugin.beforeQuery(current);
                current = updated == null ? current : updated;
            } catch (RuntimeException ex) {
                warn(log,
                        "RAG plugin {} failed in beforeQuery hook. Continuing without plugin mutation.",
                        plugin.id(),
                        ex);
            }
        }
        return current;
    }

    static List<RagScoredChunk> afterRetrieve(
            List<RagPipelinePlugin> plugins,
            RagPluginExecutionContext context,
            List<RagScoredChunk> chunks,
            Logger log) {
        List<RagScoredChunk> current = RagRuntimeLists.copy(chunks);
        for (RagPipelinePlugin plugin : pluginsOrEmpty(plugins)) {
            if (plugin == null) {
                continue;
            }
            try {
                List<RagScoredChunk> updated = plugin.afterRetrieve(context, current);
                current = updated == null ? current : RagRuntimeLists.copy(updated);
            } catch (RuntimeException ex) {
                warn(log,
                        "RAG plugin {} failed in afterRetrieve hook. Keeping previous chunks.",
                        plugin.id(),
                        ex);
            }
        }
        return current;
    }

    static RagResult afterResult(
            List<RagPipelinePlugin> plugins,
            RagPluginExecutionContext context,
            RagResult result,
            Logger log) {
        RagResult current = result;
        for (RagPipelinePlugin plugin : pluginsOrEmpty(plugins)) {
            if (plugin == null) {
                continue;
            }
            try {
                RagResult updated = plugin.afterResult(context, current);
                current = updated == null ? current : updated;
            } catch (RuntimeException ex) {
                warn(log,
                        "RAG plugin {} failed in afterResult hook. Keeping previous result.",
                        plugin.id(),
                        ex);
            }
        }
        return current;
    }

    private static void warn(Logger log, String message, String pluginId, RuntimeException ex) {
        if (log != null) {
            log.warn(message, pluginId, ex);
        }
    }

    private static List<RagPipelinePlugin> pluginsOrEmpty(List<RagPipelinePlugin> plugins) {
        return RagRuntimeLists.orEmpty(plugins);
    }
}
