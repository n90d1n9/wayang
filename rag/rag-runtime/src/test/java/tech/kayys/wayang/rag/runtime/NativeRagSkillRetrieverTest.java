package tech.kayys.wayang.rag.runtime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tech.kayys.wayang.agent.spi.skills.rag.RagSkillRetrievalRequest;
import tech.kayys.wayang.agent.spi.skills.rag.RagSkillRetrievalResult;
import tech.kayys.wayang.rag.core.RagChunk;
import tech.kayys.wayang.rag.core.RagMetadataKeys;
import tech.kayys.wayang.rag.core.RagScoredChunk;
import tech.kayys.wayang.rag.core.RetrievalConfig;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NativeRagSkillRetrieverTest {

    @Mock
    private NativeRagCoreService nativeRagCoreService;

    @Test
    void delegatesSkillRetrievalToNativeRuntimeAndProjectsSources() {
        NativeRagSkillRetriever retriever = retriever();
        RagScoredChunk chunk = new RagScoredChunk(
                RagChunk.of(
                        "doc-1",
                        0,
                        "Wayang supports dynamic skills.",
                        Map.of(RagMetadataKeys.SOURCE, "manual", "page", 2)),
                0.91);
        when(nativeRagCoreService.retrieve(eq("tenant-a"), eq("question"), any(RetrievalConfig.class), anyMap()))
                .thenReturn(List.of(chunk));

        RagSkillRetrievalResult result = retriever.retrieve(new RagSkillRetrievalRequest(
                " tenant-a ",
                " question ",
                " docs ",
                2,
                null,
                Map.of("domain", "platform")))
                .await().indefinitely();

        ArgumentCaptor<RetrievalConfig> config = ArgumentCaptor.forClass(RetrievalConfig.class);
        ArgumentCaptor<Map<String, Object>> filters = ArgumentCaptor.forClass(Map.class);
        verify(nativeRagCoreService).retrieve(eq("tenant-a"), eq("question"), config.capture(), filters.capture());
        assertEquals(2, config.getValue().topK());
        assertEquals("platform", filters.getValue().get("domain"));
        assertEquals("docs", filters.getValue().get(RagMetadataKeys.COLLECTION));
        assertEquals(1, result.documents().size());
        assertEquals("manual", result.documents().getFirst().title());
        assertEquals("Wayang supports dynamic skills.", result.documents().getFirst().content());
        assertEquals("2", result.documents().getFirst().metadata().get("page"));
    }

    @Test
    void returnsEmptyResultForMissingQuery() {
        NativeRagSkillRetriever retriever = retriever();

        RagSkillRetrievalResult result = retriever.retrieve(new RagSkillRetrievalRequest(
                "tenant-a",
                " ",
                "docs",
                2,
                null,
                Map.of()))
                .await().indefinitely();

        assertEquals(List.of(), result.documents());
        verifyNoInteractions(nativeRagCoreService);
    }

    private NativeRagSkillRetriever retriever() {
        NativeRagSkillRetriever retriever = new NativeRagSkillRetriever();
        retriever.nativeRagCoreService = nativeRagCoreService;
        return retriever;
    }
}
