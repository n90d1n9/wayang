package tech.kayys.wayang.rag.runtime;

import tech.kayys.wayang.rag.core.ChunkingConfig;
import tech.kayys.wayang.rag.core.RagChunk;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

record RagIngestionContext(
        String tenantId,
        List<RagIngestionDocument> documents,
        ChunkingConfig chunkingConfig,
        String successMessage) {

    static RagIngestionContext empty(String tenantId, String message) {
        return create(tenantId, List.of(), ChunkingConfig.defaults(), message);
    }

    static RagIngestionContext pdf(
            String tenantId,
            List<Path> pdfPaths,
            Map<String, String> metadata,
            Function<Path, String> textReader) {

        List<RagIngestionDocument> documents = safeList(pdfPaths).stream()
                .filter(Objects::nonNull)
                .map(path -> RagIngestionDocument.pdf(
                        tenantId,
                        path,
                        textReader.apply(path),
                        metadata))
                .toList();
        return create(
                tenantId,
                documents,
                ChunkingConfig.defaults(),
                "Successfully ingested documents");
    }

    static RagIngestionContext text(
            String tenantId,
            List<String> texts,
            Map<String, String> metadata,
            ChunkingConfig chunkingConfig) {

        List<RagIngestionDocument> documents = safeList(texts).stream()
                .map(text -> RagIngestionDocument.text(tenantId, text, metadata))
                .toList();
        return create(
                tenantId,
                documents,
                chunkingConfig,
                "Successfully ingested text documents");
    }

    List<RagChunk> ingestWith(NativeRagCoreService nativeRagCoreService) {
        List<RagChunk> chunks = new ArrayList<>();
        for (RagIngestionDocument document : documents) {
            List<RagChunk> ingested = nativeRagCoreService.ingestText(
                    tenantId,
                    document.source(),
                    document.content(),
                    document.metadata(),
                    chunkingConfig);
            if (ingested != null) {
                chunks.addAll(ingested);
            }
        }
        return RagRuntimeLists.copy(chunks);
    }

    IngestResult toResult(long durationMs, int chunksCreated) {
        return new IngestResult(
                true,
                documents.size(),
                chunksCreated,
                durationMs,
                successMessage);
    }

    private static RagIngestionContext create(
            String tenantId,
            List<RagIngestionDocument> documents,
            ChunkingConfig chunkingConfig,
            String successMessage) {

        return new RagIngestionContext(
                tenantId,
                RagRuntimeLists.copy(documents),
                chunkingConfig == null ? ChunkingConfig.defaults() : chunkingConfig,
                successMessage == null ? "Ingestion completed" : successMessage);
    }

    private static <T> List<T> safeList(List<T> values) {
        return RagRuntimeLists.orEmpty(values);
    }
}

record RagIngestionDocument(
        String source,
        String content,
        Map<String, Object> metadata) {

    static RagIngestionDocument pdf(
            String tenantId,
            Path path,
            String content,
            Map<String, String> metadata) {

        return create(content, RagIngestionMetadata.pdf(tenantId, path, metadata));
    }

    static RagIngestionDocument text(
            String tenantId,
            String content,
            Map<String, String> metadata) {

        return create(content, RagIngestionMetadata.text(tenantId, metadata));
    }

    private static RagIngestionDocument create(String content, Map<String, Object> metadata) {
        Map<String, Object> copiedMetadata = RagIngestionMetadata.copy(metadata);
        return new RagIngestionDocument(
                RagIngestionMetadata.source(copiedMetadata),
                content == null ? "" : content,
                copiedMetadata);
    }
}
