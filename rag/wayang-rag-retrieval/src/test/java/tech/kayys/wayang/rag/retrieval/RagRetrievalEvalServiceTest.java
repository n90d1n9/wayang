package tech.kayys.wayang.rag.retrieval;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import tech.kayys.wayang.rag.core.RagChunk;
import tech.kayys.wayang.rag.core.RagEvalDataset;
import tech.kayys.wayang.rag.core.RagEvalQueryCase;
import tech.kayys.wayang.rag.core.RagScoredChunk;
import tech.kayys.wayang.rag.runtime.NativeRagCoreService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RagRetrievalEvalServiceTest {

        @Test
        void shouldComputeRecallMrrAndLatency() {
                NativeRagCoreService nativeService = mock(NativeRagCoreService.class);
                when(nativeService.retrieve(eq("tenant-a"), eq("q1"), any(), anyMap()))
                                .thenReturn(List.of(
                                                scored("chunk-2", "doc-2", Map.of("source", "s2"), 0.88),
                                                scored("chunk-1", "doc-1", Map.of("source", "s1"), 0.83)));
                when(nativeService.retrieve(eq("tenant-a"), eq("q2"), any(), anyMap()))
                                .thenReturn(List.of(scored("chunk-3", "doc-3", Map.of("source", "s3"), 0.77)));

                RagRetrievalEvalService service = new RagRetrievalEvalService(
                                nativeService,
                                new ObjectMapper(),
                                Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneId.of("UTC")));

                RagEvalDataset dataset = new RagEvalDataset(
                                "baseline-fixture",
                                "tenant-a",
                                5,
                                0.2,
                                "documentId",
                                Map.of("collection", "docs"),
                                List.of(
                                                new RagEvalQueryCase("c1", "q1", List.of("doc-1"), Map.of()),
                                                new RagEvalQueryCase("c2", "q2", List.of("doc-9"), Map.of())));

                RagRetrievalEvalResponse response = service.evaluate(new RagRetrievalEvalRequest(
                                null,
                                null,
                                null,
                                null,
                                Map.of(),
                                null,
                                dataset));

                assertEquals("baseline-fixture", response.datasetName());
                assertEquals("tenant-a", response.tenantId());
                assertEquals(2, response.queryCount());
                assertEquals(1, response.hitCount());
                assertEquals(0.5, response.recallAtK(), 1e-9);
                assertEquals(0.25, response.mrr(), 1e-9);
                assertTrue(response.latencyP95Ms() >= 0.0);
                assertEquals(Instant.parse("2026-01-01T00:00:00Z"), response.evaluatedAt());
        }

        @Test
        void shouldLoadDatasetFromClasspathFixture() {
                NativeRagCoreService nativeService = mock(NativeRagCoreService.class);
                when(nativeService.retrieve(eq("tenant-b"), eq("who"), any(), anyMap()))
                                .thenReturn(List.of(scored("chunk-7", "doc-7", Map.of("source", "guide.md"), 0.91)));

                RagRetrievalEvalService service = new RagRetrievalEvalService(
                                nativeService,
                                new ObjectMapper(),
                                Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneId.of("UTC")));

                RagRetrievalEvalResponse response = service.evaluate(new RagRetrievalEvalRequest(
                                null,
                                null,
                                null,
                                null,
                                Map.of(),
                                "classpath:rag-eval/fixture.json",
                                null));

                assertEquals("classpath-fixture", response.datasetName());
                assertEquals("tenant-b", response.tenantId());
                assertEquals("source", response.matchField());
                assertEquals(1.0, response.recallAtK(), 1e-9);
                assertEquals(1.0, response.mrr(), 1e-9);
        }

        private static RagScoredChunk scored(String chunkId, String documentId, Map<String, Object> metadata,
                        double score) {
                return new RagScoredChunk(new RagChunk(chunkId, documentId, 0, "text", metadata), score);
        }
}
