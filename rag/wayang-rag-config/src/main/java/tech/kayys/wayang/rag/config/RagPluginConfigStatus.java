package tech.kayys.wayang.rag.config;

import java.time.Instant;

public record RagPluginConfigStatus(
        RagPluginConfigSnapshot config,
        Instant updatedAt) {
}
