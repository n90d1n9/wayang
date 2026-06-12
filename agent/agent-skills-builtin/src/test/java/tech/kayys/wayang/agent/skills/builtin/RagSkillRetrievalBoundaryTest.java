package tech.kayys.wayang.agent.skills.builtin;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.spi.skills.rag.RagSkillRetrievedDocument;
import tech.kayys.wayang.agent.spi.skills.rag.RagSkillRetrievalRequest;
import tech.kayys.wayang.agent.spi.skills.rag.RagSkillRetrievalResult;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RagSkillRetrievalBoundaryTest {

    @Test
    void requestCopiesFiltersAndEmbeddingDefensively() {
        float[] embedding = new float[] { 0.1f, 0.2f };
        Map<String, Object> filters = new HashMap<>();
        filters.put("domain", "docs");
        filters.put("ignored", null);

        RagSkillRetrievalRequest request = new RagSkillRetrievalRequest(
                " tenant-a ",
                " question ",
                " docs ",
                -1,
                embedding,
                filters);
        embedding[0] = 9.0f;
        filters.put("domain", "mutated");

        assertThat(request.tenantId()).isEqualTo("tenant-a");
        assertThat(request.query()).isEqualTo("question");
        assertThat(request.collection()).isEqualTo("docs");
        assertThat(request.topK()).isZero();
        assertThat(request.queryEmbedding()).containsExactly(0.1f, 0.2f);
        assertThat(request.filters()).containsExactlyEntriesOf(Map.of("domain", "docs"));
        assertThatThrownBy(() -> request.filters().put("other", "value"))
                .isInstanceOf(UnsupportedOperationException.class);

        float[] exposed = request.queryEmbedding();
        exposed[0] = 7.0f;
        assertThat(request.queryEmbedding()).containsExactly(0.1f, 0.2f);
    }

    @Test
    void resultFiltersMissingDocumentsAndRendersPromptContext() {
        RagSkillRetrievedDocument document = new RagSkillRetrievedDocument(
                " doc-1 ",
                " Manual ",
                " Wayang supports dynamic skills. ",
                " docs://manual ",
                0.9,
                Map.of("page", 1));

        RagSkillRetrievalResult result = new RagSkillRetrievalResult(Arrays.asList(
                null,
                new RagSkillRetrievedDocument("empty", "Empty", " ", "", 0.1, Map.of()),
                document));

        assertThat(result.documents()).containsExactly(document);
        assertThat(result.context()).contains("Manual:").contains("Wayang supports dynamic skills.");
        assertThat(document.sourceSummary())
                .containsEntry("id", "doc-1")
                .containsEntry("title", "Manual")
                .containsEntry("source", "docs://manual")
                .containsEntry("score", 0.9);
    }
}
