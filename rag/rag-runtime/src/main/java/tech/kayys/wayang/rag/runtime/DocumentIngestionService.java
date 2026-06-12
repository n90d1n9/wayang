package tech.kayys.wayang.rag.runtime;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.wayang.rag.core.ChunkingConfig;
import tech.kayys.wayang.rag.core.RagChunk;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

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

        LOG.info("Ingesting {} PDF documents for tenant: {}", sizeOf(pdfPaths), tenantId);

        return ingestContext(() -> RagIngestionContext.pdf(tenantId, pdfPaths, metadata, this::readPdfText), true);
    }

    /**
     * Ingest text documents with custom chunking
     */
    public Uni<IngestResult> ingestTextDocuments(
            String tenantId,
            List<String> texts,
            Map<String, String> metadata,
            ChunkingConfig chunkingConfig) {

        LOG.info("Ingesting {} text documents for tenant: {}", sizeOf(texts), tenantId);

        return ingestContext(() -> RagIngestionContext.text(tenantId, texts, metadata, chunkingConfig), true);
    }

    /**
     * Batch ingest from multiple sources
     */
    public Uni<IngestResult> batchIngest(
            String tenantId,
            List<DocumentSource> sources) {

        LOG.info("Batch ingesting {} sources for tenant: {}", sizeOf(sources), tenantId);

        return Uni.createFrom().item(() -> {
            int totalDocs = 0;
            int totalSegments = 0;
            long totalDuration = 0;

            for (DocumentSource source : safeSources(sources)) {
                IngestResult r = ingestSource(tenantId, source);
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

    private IngestResult ingestFromUrl(
            String tenantId,
            String url,
            Map<String, String> metadata) {

        // TODO: Implement URL scraping and ingestion
        return new IngestResult(true, 0, 0, 0, "URL ingestion not implemented yet");
    }

    private Uni<IngestResult> ingestContext(Supplier<RagIngestionContext> contextSupplier, boolean recordMetrics) {
        return Uni.createFrom().item(() -> ingestContext(contextSupplier.get(), recordMetrics));
    }

    private IngestResult ingestContext(RagIngestionContext context, boolean recordMetrics) {
        long startTime = System.currentTimeMillis();
        List<RagChunk> chunks = context.ingestWith(nativeRagCoreService);
        long duration = System.currentTimeMillis() - startTime;

        LOG.info("Split {} documents into {} chunks", context.documents().size(), chunks.size());
        LOG.info("Successfully ingested {} chunks in {}ms", chunks.size(), duration);

        if (recordMetrics) {
            metricsRecorder.recordIngestion(context.tenantId(), context.documents().size(), chunks.size(), duration);
        }
        return context.toResult(duration, chunks.size());
    }

    private IngestResult ingestSource(String tenantId, DocumentSource source) {
        if (source.type() == null) {
            return RagIngestionContext.empty(tenantId, "Skipped source without type").toResult(0, 0);
        }
        return switch (source.type()) {
            case PDF -> ingestContext(
                    source.path() == null || source.path().isBlank()
                            ? RagIngestionContext.empty(tenantId, "Skipped PDF source without path")
                            : RagIngestionContext.pdf(
                                    tenantId,
                                    List.of(Path.of(source.path())),
                                    source.metadata(),
                                    this::readPdfText),
                    false);
            case TEXT, MARKDOWN, HTML -> ingestContext(
                    RagIngestionContext.text(
                            tenantId,
                            List.of(source.content() == null ? "" : source.content()),
                            source.metadata(),
                            ChunkingConfig.defaults()),
                    false);
            case URL -> ingestFromUrl(tenantId, source.path(), source.metadata());
        };
    }

    private String readPdfText(Path path) {
        try (PDDocument document = Loader.loadPDF(path.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to parse PDF: " + path, e);
        }
    }

    private static int sizeOf(List<?> values) {
        return values == null ? 0 : values.size();
    }

    private static List<DocumentSource> safeSources(List<DocumentSource> sources) {
        if (sources == null || sources.isEmpty()) {
            return List.of();
        }
        return sources.stream()
                .filter(Objects::nonNull)
                .toList();
    }

}
