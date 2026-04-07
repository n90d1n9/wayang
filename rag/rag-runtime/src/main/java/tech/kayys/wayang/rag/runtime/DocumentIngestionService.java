package tech.kayys.wayang.rag.runtime;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.wayang.rag.core.ChunkingConfig;
import tech.kayys.wayang.rag.core.RagChunk;
import tech.kayys.wayang.rag.core.RagDocument;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

/**
 * Service for ingesting documents into the owned vector store flow.
 * Handles PDF, text, and batch ingestion, performing text extraction,
 * chunking via {@link NativeRagCoreService}, and recording ingestion metrics.
 */
@ApplicationScoped
public class DocumentIngestionService {

    private static final Logger LOG = LoggerFactory.getLogger(DocumentIngestionService.class);

    @Inject
    NativeRagCoreService nativeRagCoreService;

    @Inject
    RagObservabilityMetrics metricsRecorder = new RagObservabilityMetrics();

    /**
     * Ingest PDF documents into vector store
     */
    public Uni<IngestResult> ingestPdfDocuments(
            String tenantId,
            List<Path> pdfPaths,
            Map<String, String> metadata) {

        LOG.info("Ingesting {} PDF documents for tenant: {}", pdfPaths.size(), tenantId);

        return Uni.createFrom().item(() -> {
            long startTime = System.currentTimeMillis();

            // 1. Load documents
            List<RagDocument> documents = new ArrayList<>();

            for (Path path : pdfPaths) {
                Map<String, Object> documentMetadata = new HashMap<>();
                documentMetadata.put("tenantId", tenantId);
                documentMetadata.put("source", path.getFileName().toString());
                documentMetadata.put("collection", metadata.getOrDefault("collection", "default"));
                documentMetadata.putAll(metadata);

                String content = readPdfText(path);
                documents.add(RagDocument.of(content, documentMetadata));
            }

            // 2. Split documents into chunks
            List<RagChunk> chunks = new ArrayList<>();
            for (RagDocument document : documents) {
                chunks.addAll(nativeRagCoreService.ingestText(
                        tenantId,
                        document.metadata().get("source") != null ? document.metadata().get("source").toString() : "",
                        document.content(),
                        document.metadata(),
                        ChunkingConfig.defaults()));
            }

            LOG.info("Split {} documents into {} chunks", documents.size(), chunks.size());

            long duration = System.currentTimeMillis() - startTime;

            LOG.info("Successfully ingested {} chunks in {}ms", chunks.size(), duration);
            metricsRecorder.recordIngestion(tenantId, documents.size(), chunks.size(), duration);

            return new IngestResult(
                    true,
                    documents.size(),
                    chunks.size(),
                    duration,
                    "Successfully ingested documents");
        });
    }

    /**
     * Ingest text documents with custom chunking
     */
    public Uni<IngestResult> ingestTextDocuments(
            String tenantId,
            List<String> texts,
            Map<String, String> metadata,
            ChunkingConfig chunkingConfig) {

        LOG.info("Ingesting {} text documents for tenant: {}", texts.size(), tenantId);

        return Uni.createFrom().item(() -> {
            long startTime = System.currentTimeMillis();

            // 1. Create documents
            List<RagDocument> documents = texts.stream()
                    .map(text -> {
                        Map<String, Object> documentMetadata = new HashMap<>();
                        documentMetadata.put("tenantId", tenantId);
                        documentMetadata.putAll(metadata);
                        return RagDocument.of(text, documentMetadata);
                    })
                    .toList();

            // 2. Split using owned chunker
            List<RagChunk> chunks = new ArrayList<>();
            for (RagDocument document : documents) {
                chunks.addAll(nativeRagCoreService.ingestText(
                        tenantId,
                        document.metadata().get("source") != null ? document.metadata().get("source").toString() : "",
                        document.content(),
                        document.metadata(),
                        chunkingConfig));
            }

            long duration = System.currentTimeMillis() - startTime;
            metricsRecorder.recordIngestion(tenantId, documents.size(), chunks.size(), duration);

            return new IngestResult(
                    true,
                    documents.size(),
                    chunks.size(),
                    duration,
                    "Successfully ingested text documents");
        });
    }

    /**
     * Batch ingest from multiple sources
     */
    public Uni<IngestResult> batchIngest(
            String tenantId,
            List<DocumentSource> sources) {

        LOG.info("Batch ingesting {} sources for tenant: {}", sources.size(), tenantId);

        List<Uni<IngestResult>> unis = sources.stream()
                .map(source -> {
                    return switch (source.type()) {
                        case PDF -> ingestPdfDocuments(
                                tenantId,
                                List.of(Path.of(source.path())),
                                source.metadata());
                        case TEXT -> ingestTextDocuments(
                                tenantId,
                                List.of(source.content()),
                                source.metadata(),
                                ChunkingConfig.defaults());
                        case URL -> ingestFromUrl(tenantId, source.path(), source.metadata());
                        case MARKDOWN, HTML -> ingestTextDocuments(
                                tenantId,
                                List.of(source.content() == null ? "" : source.content()),
                                source.metadata(),
                                ChunkingConfig.defaults());
                    };
                })
                .toList();

        return Uni.combine().all().unis(unis).combinedWith(results -> {
            int totalDocs = 0;
            int totalSegments = 0;
            long totalDuration = 0;

            for (Object result : results) {
                IngestResult r = (IngestResult) result;
                totalDocs += r.documentsIngested();
                totalSegments += r.segmentsCreated();
                totalDuration += r.durationMs();
            }

            metricsRecorder.recordIngestion(tenantId, totalDocs, totalSegments, totalDuration);

            return new IngestResult(
                    true,
                    totalDocs,
                    totalSegments,
                    totalDuration,
                    "Batch ingestion completed");
        });
    }

    private Uni<IngestResult> ingestFromUrl(
            String tenantId,
            String url,
            Map<String, String> metadata) {

        // TODO: Implement URL scraping and ingestion
        return Uni.createFrom().item(new IngestResult(
                true, 0, 0, 0, "URL ingestion not implemented yet"));
    }

    private String readPdfText(Path path) {
        try (PDDocument document = Loader.loadPDF(path.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to parse PDF: " + path, e);
        }
    }

}
