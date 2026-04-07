package tech.kayys.wayang.rag.embedding;

public record EmbeddingSchemaHistoryCompactionRequest(
        Integer maxEvents,
        Integer maxAgeDays,
        Boolean dryRun) {
}
