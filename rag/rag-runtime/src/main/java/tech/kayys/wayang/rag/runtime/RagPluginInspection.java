package tech.kayys.wayang.rag.runtime;

public record RagPluginInspection(
        String id,
        int order,
        boolean enabledByConfig,
        boolean supportsTenant,
        boolean active) {
}
