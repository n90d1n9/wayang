package tech.kayys.wayang.rag.embedding;

import java.time.Instant;

public record EmbeddingSchemaHistoryCompactionStatus(
        String tenantId,
        int beforeCount,
        int afterCount,
        int removedCount,
        boolean dryRun,
        Instant compactedAt) {
}
