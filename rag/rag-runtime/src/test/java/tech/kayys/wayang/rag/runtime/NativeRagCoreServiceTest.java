package tech.kayys.wayang.rag.runtime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tech.kayys.wayang.rag.core.ChunkingConfig;
import tech.kayys.wayang.rag.core.GenerationConfig;
import tech.kayys.wayang.rag.core.RagChunk;
import tech.kayys.wayang.rag.core.RagQuery;
import tech.kayys.wayang.rag.core.RagResult;
import tech.kayys.wayang.rag.core.RagScoredChunk;
import tech.kayys.wayang.rag.core.RetrievalConfig;
import tech.kayys.wayang.rag.core.impl.RagPipeline;
import tech.kayys.wayang.rag.core.spi.ChunkingOptions;
import tech.kayys.wayang.rag.plugin.api.RagPluginExecutionContext;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NativeRagCoreServiceTest {

    @Mock
    private NativeGenerationService nativeGenerationService;

    @Mock
    private RagPluginManager pluginManager;

    @Mock
    private NativeRagPipelineFactory pipelineFactory;

    @Mock
    private RagPipeline pipeline;

    @Test
    void ingestTextDefaultsNullChunkingConfigAtCoreBoundary() {
        NativeRagCoreService service = service();
        RagChunk chunk = new RagChunk("chunk-1", "doc-1", 0, "content", Map.of());

        when(pipelineFactory.createRetrievalOnly("tenant")).thenReturn(pipeline);
        when(pipeline.ingest(any(), any(), any(), any(ChunkingOptions.class))).thenReturn(List.of(chunk));

        List<RagChunk> chunks = service.ingestText("tenant", "doc-1", "content", Map.of(), null);

        ArgumentCaptor<ChunkingOptions> options = ArgumentCaptor.forClass(ChunkingOptions.class);
        verify(pipeline).ingest(eq("doc-1"), eq("content"), eq(Map.of()), options.capture());
        assertEquals(List.of(chunk), chunks);
        assertEquals(ChunkingConfig.defaults().chunkSize(), options.getValue().chunkSize());
        assertEquals(ChunkingConfig.defaults().chunkOverlap(), options.getValue().chunkOverlap());
    }

    @Test
    void queryGeneratesAnswerAfterAfterRetrievePlugins() {
        NativeRagCoreService service = service();
        RetrievalConfig retrievalConfig = RetrievalConfig.defaults();
        GenerationConfig generationConfig = GenerationConfig.defaults();
        RagScoredChunk original = scored("original", "Original context");
        RagScoredChunk transformed = scored("transformed", "Transformed context");
        RagQuery ragQuery = new RagQuery("question", retrievalConfig.topK(), retrievalConfig.minSimilarity(), Map.of());

        when(pluginManager.applyBeforeQuery(any(RagPluginExecutionContext.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(pipelineFactory.createRetrievalOnly("tenant")).thenReturn(pipeline);
        when(pipeline.query(any(RagQuery.class)))
                .thenReturn(new RagResult(ragQuery, List.of(original), "", Map.of("retrieved", 1)));
        when(pluginManager.applyAfterRetrieve(any(RagPluginExecutionContext.class), eq(List.of(original))))
                .thenReturn(List.of(transformed));
        when(nativeGenerationService.generate(any(RagQuery.class), eq(List.of(transformed)), same(generationConfig)))
                .thenReturn("answer from transformed context");
        when(pluginManager.applyAfterResult(any(RagPluginExecutionContext.class), any(RagResult.class)))
                .thenAnswer(invocation -> invocation.getArgument(1));

        RagResult result = service.query("tenant", "question", retrievalConfig, generationConfig, Map.of());

        assertEquals("answer from transformed context", result.answer());
        assertEquals(List.of(transformed), result.chunks());
        assertEquals(1, result.metadata().get("retrieved"));
        verify(nativeGenerationService).generate(any(RagQuery.class), eq(List.of(transformed)), same(generationConfig));
    }

    @Test
    void retrieveAppliesBeforeQueryAndAfterRetrievePlugins() {
        NativeRagCoreService service = service();
        RetrievalConfig retrievalConfig = RetrievalConfig.defaults();
        RagScoredChunk original = scored("original", "Original context");
        RagScoredChunk transformed = scored("transformed", "Transformed context");

        when(pluginManager.applyBeforeQuery(any(RagPluginExecutionContext.class)))
                .thenAnswer(invocation -> ((RagPluginExecutionContext) invocation.getArgument(0))
                        .withQuery("rewritten question"));
        when(pipelineFactory.createRetrievalOnly("tenant")).thenReturn(pipeline);
        when(pipeline.query(any(RagQuery.class)))
                .thenReturn(new RagResult(RagQuery.of("rewritten question"), List.of(original), "", Map.of()));
        when(pluginManager.applyAfterRetrieve(any(RagPluginExecutionContext.class), eq(List.of(original))))
                .thenReturn(List.of(transformed));

        List<RagScoredChunk> result = service.retrieve("tenant", "question", retrievalConfig, Map.of());

        ArgumentCaptor<RagQuery> ragQuery = ArgumentCaptor.forClass(RagQuery.class);
        verify(pipeline).query(ragQuery.capture());
        assertEquals("rewritten question", ragQuery.getValue().text());
        assertEquals(List.of(transformed), result);
    }

    @Test
    void retrieveNormalizesNullPipelineChunks() {
        NativeRagCoreService service = service();
        service.pluginManager = null;

        when(pipelineFactory.createRetrievalOnly("tenant")).thenReturn(pipeline);
        when(pipeline.query(any(RagQuery.class)))
                .thenReturn(new RagResult(RagQuery.of("question"), null, "", null));

        List<RagScoredChunk> result = service.retrieve("tenant", "question", RetrievalConfig.defaults(), Map.of());

        assertEquals(List.of(), result);
    }

    @Test
    void retrieveNormalizesMalformedPluginChunks() {
        NativeRagCoreService service = service();
        RetrievalConfig retrievalConfig = RetrievalConfig.defaults();
        RagScoredChunk original = scored("original", "Original context");
        RagScoredChunk transformed = scored("transformed", "Transformed context");

        when(pluginManager.applyBeforeQuery(any(RagPluginExecutionContext.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(pipelineFactory.createRetrievalOnly("tenant")).thenReturn(pipeline);
        when(pipeline.query(any(RagQuery.class)))
                .thenReturn(new RagResult(RagQuery.of("question"), List.of(original), "", Map.of()));
        when(pluginManager.applyAfterRetrieve(any(RagPluginExecutionContext.class), eq(List.of(original))))
                .thenReturn(Arrays.asList(null, new RagScoredChunk(null, 0.2), transformed));

        List<RagScoredChunk> result = service.retrieve("tenant", "question", retrievalConfig, Map.of());

        assertEquals(List.of(transformed), result);
    }

    private NativeRagCoreService service() {
        NativeRagCoreService service = new NativeRagCoreService();
        service.nativeGenerationService = nativeGenerationService;
        service.pluginManager = pluginManager;
        service.pipelineFactory = pipelineFactory;
        return service;
    }

    private static RagScoredChunk scored(String id, String text) {
        return new RagScoredChunk(
                new RagChunk(id, "doc-" + id, 0, text, Map.of()),
                0.9);
    }
}
