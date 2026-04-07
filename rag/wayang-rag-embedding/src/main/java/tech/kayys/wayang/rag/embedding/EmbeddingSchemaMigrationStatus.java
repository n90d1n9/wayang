package tech.kayys.wayang.rag.embedding;

import java.time.Instant;

public record EmbeddingSchemaMigrationStatus(
        String tenantId,
        EmbeddingSchemaContract previous,
        EmbeddingSchemaContract current,
        boolean changed,
        boolean clearedNamespace,
        boolean dryRun,
        Instant migratedAt) {
}
