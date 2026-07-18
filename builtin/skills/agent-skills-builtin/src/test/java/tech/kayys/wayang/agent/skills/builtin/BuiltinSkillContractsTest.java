package tech.kayys.wayang.agent.skills.builtin;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.spi.InferenceBackend;
import tech.kayys.wayang.agent.spi.InferenceRequest;
import tech.kayys.wayang.agent.spi.InferenceResponse;
import tech.kayys.wayang.agent.spi.InferenceTypes;
import tech.kayys.wayang.agent.spi.skills.rag.RagSkillRetrievedDocument;
import tech.kayys.wayang.agent.spi.skills.rag.RagSkillRetrievalRequest;
import tech.kayys.wayang.agent.spi.skills.rag.RagSkillRetrievalResult;
import tech.kayys.wayang.agent.spi.skills.rag.RagSkillRetriever;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class BuiltinSkillContractsTest {

    @Test
    void codeExecutionUsesActiveMapContractForValidation() {
        CodeExecutionSkill skill = new CodeExecutionSkill();

        assertThat(skill.id()).isEqualTo("code_execution");
        assertThat(skill.category()).isEqualTo("EXECUTION");
        assertThat(skill.canHandle(Map.of("code", "print(1)"))).isTrue();

        Map<String, Object> result = skill.execute(Map.of()).await().indefinitely();

        assertThat(result)
                .containsEntry("success", false)
                .containsEntry("status", "FAILURE");
        assertThat(result.get("observation")).asString().contains("Input 'code' is required");
    }

    @Test
    void httpCallValidatesUrlBeforeClientUsage() {
        HttpCallSkill skill = new HttpCallSkill();
        skill.init();

        assertThat(skill.id()).isEqualTo("http_call");
        assertThat(skill.category()).isEqualTo("COMMUNICATION");
        assertThat(skill.canHandle(Map.of("url", "https://example.test"))).isTrue();

        Map<String, Object> result = skill.execute(Map.of()).await().indefinitely();

        assertThat(result)
                .containsEntry("success", false)
                .containsEntry("status", "FAILURE");
        assertThat(result.get("observation")).asString().contains("Input 'url' is required");
    }

    @Test
    void inferenceSkillDelegatesThroughActiveInferenceBackend() {
        RecordingInferenceBackend backend = new RecordingInferenceBackend("hello from backend");
        InferenceSkill skill = new InferenceSkill();
        skill.inferenceBackend = backend;

        Map<String, Object> result = skill.execute(Map.of(
                "prompt", "Say hello",
                "model", "test-model",
                "maxTokens", 64,
                "temperature", 0.2,
                "tenantId", "tenant-a"))
                .await().indefinitely();

        assertThat(result)
                .containsEntry("success", true)
                .containsEntry("status", "SUCCESS")
                .containsEntry("response", "hello from backend")
                .containsEntry("tokensUsed", 7)
                .containsEntry("model", "test-model");
        assertThat(backend.lastRequest.model()).isEqualTo("test-model");
        assertThat(backend.lastRequest.maxTokens()).isEqualTo(64);
        assertThat(backend.lastRequest.temperature()).isEqualTo(0.2);
        assertThat(backend.lastRequest.metadata()).containsEntry("tenantId", "tenant-a");
        assertThat(backend.lastRequest.messages())
                .extracting(InferenceTypes.ChatMessage::role)
                .containsExactly("system", "user");
    }

    @Test
    void ragSkillUsesSuppliedContextAndBackendWhenVectorStoreIsDisabled() {
        RecordingInferenceBackend backend = new RecordingInferenceBackend("grounded answer");
        RAGSkill skill = new RAGSkill();
        skill.inferenceBackend = backend;

        Map<String, Object> result = skill.execute(Map.of(
                "query", "What is Wayang?",
                "context", "Wayang is an agentic platform.",
                "model", "rag-model",
                "topK", 3))
                .await().indefinitely();

        assertThat(result)
                .containsEntry("success", true)
                .containsEntry("answer", "grounded answer")
                .containsEntry("context", "Wayang is an agentic platform.")
                .containsEntry("topK", 3);
        assertThat(result.get("sources")).isEqualTo(List.of());
        assertThat(backend.lastRequest.model()).isEqualTo("rag-model");
        assertThat(backend.lastRequest.messages().get(1).content())
                .contains("Context:")
                .contains("Question: What is Wayang?");
    }

    @Test
    void ragSkillRetrievesContextThroughRetrieverBoundary() {
        RecordingInferenceBackend backend = new RecordingInferenceBackend("retrieved answer");
        RecordingRagSkillRetriever retriever = new RecordingRagSkillRetriever();
        RAGSkill skill = new RAGSkill();
        skill.inferenceBackend = backend;
        skill.retriever = retriever;
        skill.vectorStoreEnabled = true;

        Map<String, Object> result = skill.execute(Map.of(
                "tenantId", "tenant-a",
                "query", "What is Wayang?",
                "collection", "docs",
                "topK", 2,
                "filters", Map.of("domain", "platform")))
                .await().indefinitely();

        assertThat(result)
                .containsEntry("success", true)
                .containsEntry("answer", "retrieved answer")
                .containsEntry("topK", 2);
        assertThat(result.get("context").toString())
                .contains("Wayang docs:")
                .contains("Wayang is an agentic platform.");
        assertThat((List<?>) result.get("sources")).hasSize(1);
        assertThat(backend.lastRequest.messages().get(1).content())
                .contains("Context:")
                .contains("Wayang is an agentic platform.");
        assertThat(retriever.lastRequest.tenantId()).isEqualTo("tenant-a");
        assertThat(retriever.lastRequest.collection()).isEqualTo("docs");
        assertThat(retriever.lastRequest.topK()).isEqualTo(2);
        assertThat(retriever.lastRequest.filters()).containsEntry("domain", "platform");
        assertThat(retriever.lastRequest.hasQueryEmbedding()).isFalse();
    }

    private static final class RecordingInferenceBackend implements InferenceBackend {
        private final String responseContent;
        private InferenceRequest lastRequest;

        private RecordingInferenceBackend(String responseContent) {
            this.responseContent = responseContent;
        }

        @Override
        public String name() {
            return "recording";
        }

        @Override
        public String version() {
            return "test";
        }

        @Override
        public Uni<InferenceResponse> infer(InferenceRequest request) {
            lastRequest = request;
            return Uni.createFrom().item(InferenceResponse.builder()
                    .responseId("response-1")
                    .requestId(request.requestId())
                    .model(request.model())
                    .content(responseContent)
                    .usage(InferenceTypes.TokenUsage.of(3, 4))
                    .durationMs(12)
                    .build());
        }

        @Override
        public Multi<InferenceTypes.StreamingChunk> stream(InferenceRequest request) {
            return Multi.createFrom().empty();
        }

        @Override
        public List<InferenceTypes.ProviderInfo> listProviders() {
            return List.of();
        }

        @Override
        public boolean isHealthy() {
            return true;
        }
    }

    private static final class RecordingRagSkillRetriever implements RagSkillRetriever {
        private RagSkillRetrievalRequest lastRequest;

        @Override
        public Uni<RagSkillRetrievalResult> retrieve(RagSkillRetrievalRequest request) {
            this.lastRequest = request;
            return Uni.createFrom().item(new RagSkillRetrievalResult(List.of(
                    new RagSkillRetrievedDocument(
                            "doc-1",
                            "Wayang docs",
                            "Wayang is an agentic platform.",
                            "docs://wayang",
                            0.91,
                            Map.of("domain", "platform")))));
        }
    }
}
