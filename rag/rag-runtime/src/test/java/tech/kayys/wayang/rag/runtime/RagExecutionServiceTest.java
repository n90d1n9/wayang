package tech.kayys.wayang.rag.runtime;

import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tech.kayys.wayang.rag.core.CitationStyle;
import tech.kayys.wayang.rag.core.GenerationConfig;
import tech.kayys.wayang.rag.core.RerankingModel;
import tech.kayys.wayang.rag.core.RagResponse;
import tech.kayys.wayang.rag.core.RagWorkflowInput;
import tech.kayys.wayang.rag.core.RetrievalConfig;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RagExecutionServiceTest {

    @Mock
    private RagQueryService ragQueryService;

    private RagExecutionService ragExecutionService;

    @BeforeEach
    void setUp() {
        ragExecutionService = new RagExecutionService();
        ragExecutionService.ragQueryService = ragQueryService;
    }

    @Test
    void testExecuteRagWorkflow_Success() {
        when(ragQueryService.advancedQuery(any(RagQueryRequest.class)))
                .thenReturn(Uni.createFrom().item(new RagResponse(
                        "q", "a", List.of(), List.of(), null, null, Instant.now(), Map.of(), List.of(),
                        Optional.empty())));
        RetrievalConfig retrievalConfig = new RetrievalConfig(
                8,
                0.7f,
                1024,
                128,
                true,
                RerankingModel.COHERE_RERANK,
                true,
                0.4f,
                false,
                0,
                false,
                0,
                Map.of("domain", "docs"),
                List.of(),
                false,
                true);
        GenerationConfig generationConfig = new GenerationConfig(
                "anthropic",
                "claude",
                0.3f,
                512,
                1.0f,
                0.0f,
                0.0f,
                List.of(),
                "system",
                Map.of(NativeGenerationMode.PARAM_NATIVE_GENERATION_MODE, "extractive"),
                true,
                false,
                CitationStyle.INLINE_NUMBERED,
                false,
                false,
                Map.of());

        RagResponse response = ragExecutionService.executeRagWorkflow(
                new RagWorkflowInput("tenant", "q", retrievalConfig, generationConfig))
                .await().indefinitely();

        ArgumentCaptor<RagQueryRequest> request = ArgumentCaptor.forClass(RagQueryRequest.class);
        verify(ragQueryService).advancedQuery(request.capture());

        assertEquals("a", response.answer());
        assertEquals("tenant", request.getValue().tenantId());
        assertEquals("q", request.getValue().query());
        assertEquals(retrievalConfig, request.getValue().retrievalConfig());
        assertEquals(generationConfig, request.getValue().generationConfig());
        assertEquals(List.of(RagWorkflowRequestMapper.DEFAULT_COLLECTION), request.getValue().collections());
        assertEquals(Map.of("domain", "docs"), request.getValue().filters());
    }
}
