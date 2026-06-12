package tech.kayys.wayang.rag.runtime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tech.kayys.wayang.rag.core.ChunkingConfig;
import tech.kayys.wayang.rag.core.RagChunk;
import tech.kayys.wayang.rag.core.RagMetadataKeys;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentIngestionServiceTest {

    @Mock
    private NativeRagCoreService nativeRagCoreService;

    @Mock
    private RagObservabilityMetrics metricsRecorder;

    private DocumentIngestionService ingestionService;

    @BeforeEach
    void setUp() {
        ingestionService = new DocumentIngestionService();
        ingestionService.nativeRagCoreService = nativeRagCoreService;
        ingestionService.metricsRecorder = metricsRecorder;
    }

    @Test
    void testIngestTextDocuments_Success() {
        when(nativeRagCoreService.ingestText(anyString(), anyString(), anyString(), anyMap(),
                any(ChunkingConfig.class)))
                .thenReturn(List.of(RagChunk.of("doc-1", 0, "chunk", Map.of())));

        IngestResult result = ingestionService
                .ingestTextDocuments(
                        "tenant",
                        List.of("text"),
                        Map.of(RagMetadataKeys.COLLECTION, "test"),
                        ChunkingConfig.defaults())
                .await().indefinitely();

        assertNotNull(result);
        assertEquals(1, result.documentsIngested());
        assertEquals(1, result.segmentsCreated());
    }

    @Test
    void ingestTextDocuments_DefaultsNullMetadataAndChunking() {
        when(nativeRagCoreService.ingestText(anyString(), anyString(), anyString(), anyMap(),
                any(ChunkingConfig.class)))
                .thenReturn(List.of(RagChunk.of("doc-1", 0, "chunk", Map.of())));

        IngestResult result = ingestionService
                .ingestTextDocuments("tenant", List.of("text"), null, null)
                .await().indefinitely();

        ArgumentCaptor<Map<String, Object>> metadata = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<ChunkingConfig> chunkingConfig = ArgumentCaptor.forClass(ChunkingConfig.class);
        verify(nativeRagCoreService).ingestText(
                eq("tenant"),
                eq(""),
                eq("text"),
                metadata.capture(),
                chunkingConfig.capture());

        assertEquals(1, result.documentsIngested());
        assertEquals(1, result.segmentsCreated());
        assertEquals("tenant", metadata.getValue().get(RagMetadataKeys.TENANT_ID));
        assertEquals(ChunkingConfig.defaults(), chunkingConfig.getValue());
    }

    @Test
    void testBatchIngest_Success() {
        when(nativeRagCoreService.ingestText(anyString(), anyString(), anyString(), anyMap(),
                any(ChunkingConfig.class)))
                .thenReturn(List.of(RagChunk.of("doc-1", 0, "chunk", Map.of())));

        IngestResult result = ingestionService.batchIngest(
                "tenant",
                List.of(new DocumentSource(
                        SourceType.TEXT,
                        null,
                        "hello",
                        Map.of(RagMetadataKeys.COLLECTION, "test"))))
                .await().indefinitely();

        assertNotNull(result);
        assertEquals(1, result.documentsIngested());
        assertEquals(1, result.segmentsCreated());
    }

    @Test
    void batchIngest_RecordsOneAggregateMetric() {
        when(nativeRagCoreService.ingestText(anyString(), anyString(), anyString(), anyMap(),
                any(ChunkingConfig.class)))
                .thenReturn(List.of(RagChunk.of("doc-1", 0, "chunk", Map.of())));

        IngestResult result = ingestionService.batchIngest(
                "tenant",
                List.of(
                        new DocumentSource(
                                SourceType.TEXT,
                                null,
                                "first",
                                Map.of(RagMetadataKeys.COLLECTION, "alpha")),
                        new DocumentSource(
                                SourceType.MARKDOWN,
                                null,
                                "second",
                                Map.of(RagMetadataKeys.COLLECTION, "beta"))))
                .await().indefinitely();

        assertEquals(2, result.documentsIngested());
        assertEquals(2, result.segmentsCreated());
        verify(metricsRecorder, times(1)).recordIngestion(eq("tenant"), eq(2), eq(2), anyLong());
    }

    @Test
    void testIngestPdfDocuments_ThrowsForMissingFile() {
        assertThrows(Exception.class, () -> ingestionService
                .ingestPdfDocuments("tenant", List.of(Path.of("/no/such/file.pdf")), Map.of())
                .await().indefinitely());
    }
}
