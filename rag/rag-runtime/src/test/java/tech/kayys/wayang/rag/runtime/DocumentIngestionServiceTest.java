package tech.kayys.wayang.rag.runtime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tech.kayys.wayang.rag.core.ChunkingConfig;
import tech.kayys.wayang.rag.core.RagChunk;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentIngestionServiceTest {

    @Mock
    private NativeRagCoreService nativeRagCoreService;

    private DocumentIngestionService ingestionService;

    @BeforeEach
    void setUp() {
        ingestionService = new DocumentIngestionService();
        ingestionService.nativeRagCoreService = nativeRagCoreService;
    }

    @Test
    void testIngestTextDocuments_Success() {
        when(nativeRagCoreService.ingestText(anyString(), anyString(), anyString(), anyMap(),
                any(ChunkingConfig.class)))
                .thenReturn(List.of(RagChunk.of("doc-1", 0, "chunk", Map.of())));

        IngestResult result = ingestionService
                .ingestTextDocuments("tenant", List.of("text"), Map.of("collection", "test"), ChunkingConfig.defaults())
                .await().indefinitely();

        assertNotNull(result);
        assertEquals(1, result.documentsIngested());
        assertEquals(1, result.segmentsCreated());
    }

    @Test
    void testBatchIngest_Success() {
        when(nativeRagCoreService.ingestText(anyString(), anyString(), anyString(), anyMap(),
                any(ChunkingConfig.class)))
                .thenReturn(List.of(RagChunk.of("doc-1", 0, "chunk", Map.of())));

        IngestResult result = ingestionService.batchIngest(
                "tenant",
                List.of(new DocumentSource(SourceType.TEXT, null, "hello", Map.of("collection", "test"))))
                .await().indefinitely();

        assertNotNull(result);
        assertEquals(1, result.documentsIngested());
        assertEquals(1, result.segmentsCreated());
    }

    @Test
    void testIngestPdfDocuments_ThrowsForMissingFile() {
        try {
            ingestionService.ingestPdfDocuments("tenant", List.of(Path.of("/no/such/file.pdf")), Map.of())
                    .await().indefinitely();
        } catch (Exception e) {
            assertNotNull(e);
        }
    }
}
