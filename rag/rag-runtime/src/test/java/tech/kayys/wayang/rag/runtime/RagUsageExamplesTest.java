package tech.kayys.wayang.rag.runtime;

import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tech.kayys.wayang.rag.core.RagMetrics;
import tech.kayys.wayang.rag.core.RagResponse;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RagUsageExamplesTest {

        @Mock
        private DocumentIngestionService ingestionService;

        @Mock
        private RagQueryService queryService;

        @Test
        void testUsageExamples_ExecuteWithoutError() {
                when(ingestionService.ingestPdfDocuments(anyString(), anyList(), anyMap()))
                                .thenReturn(Uni.createFrom().item(new IngestResult(true, 1, 1, 1L, "ok")));
                when(ingestionService.batchIngest(anyString(), anyList()))
                                .thenReturn(Uni.createFrom().item(new IngestResult(true, 1, 1, 1L, "ok")));
                when(queryService.query(anyString(), anyString(), anyString()))
                                .thenReturn(Uni.createFrom().item(new RagResponse(
                                                "q", "a", List.of(), List.of(), new RagMetrics(1, 1, 1, 1f, 1, 0, true),
                                                null, Instant.now(), Map.of(), List.of(), Optional.empty())));
                when(queryService.advancedQuery(any()))
                                .thenReturn(Uni.createFrom().item(new RagResponse(
                                                "q", "a", List.of(), List.of(), new RagMetrics(1, 1, 1, 1f, 1, 0, true),
                                                null, Instant.now(), Map.of(), List.of(), Optional.empty())));
                when(queryService.conversationalQuery(anyString(), anyString(), anyString(), anyList()))
                                .thenReturn(Uni.createFrom().item(new RagResponse(
                                                "q", "a", List.of(), List.of(), new RagMetrics(1, 1, 1, 1f, 1, 0, true),
                                                null, Instant.now(), Map.of(), List.of(), Optional.empty())));

                RagUsageExamples.example1_SimpleRag(ingestionService, queryService);
                RagUsageExamples.example2_AdvancedRag(queryService);
                RagUsageExamples.example3_ConversationalRag(queryService);
                RagUsageExamples.example4_BatchIngestion(ingestionService);

                verify(ingestionService, atLeastOnce()).ingestPdfDocuments(anyString(), anyList(), anyMap());
                verify(queryService, atLeastOnce()).query(anyString(), anyString(), anyString());
        }
}
