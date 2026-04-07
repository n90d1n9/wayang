package tech.kayys.wayang.rag.embedding;

public record EmbeddingSchemaMigrationRequest(
        String tenantId,
        String embeddingModel,
        Integer embeddingDimension,
        String embeddingVersion,
        Boolean clearNamespace,
        Boolean dryRun) {
}
