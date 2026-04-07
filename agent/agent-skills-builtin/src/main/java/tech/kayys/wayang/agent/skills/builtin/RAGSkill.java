package tech.kayys.wayang.agent.skills.builtin;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import tech.kayys.wayang.agent.spi.*;
import tech.kayys.wayang.agent.spi.EmbeddingRequest;
import tech.kayys.gollek.engine.inference.InferenceService;
import tech.kayys.wayang.agent.spi.InferenceRequest;
import tech.kayys.wayang.agent.spi.Message;

import java.util.List;
import java.util.Map;

/**
 * Built-in skill: Retrieval-Augmented Generation (RAG).
 *
 * <p>
 * Pipeline:
 * <ol>
 * <li>Embed the query using the configured embedding model</li>
 * <li>Retrieve top-K relevant documents from the vector store</li>
 * <li>Augment the prompt with retrieved context</li>
 * <li>Generate answer via LLM inference</li>
 * </ol>
 *
 * <p>
 * Input schema:
 * <ul>
 * <li>{@code query} (string, required) — the question to answer</li>
 * <li>{@code collection} (string, optional) — vector store collection name</li>
 * <li>{@code topK} (integer, optional, default 5) — number of documents to
 * retrieve</li>
 * <li>{@code model} (string, optional) — LLM model override</li>
 * <li>{@code embeddingModel} (string, optional) — embedding model override</li>
 * </ul>
 *
 * <p>
 * Output schema:
 * <ul>
 * <li>{@code answer} (string) — generated answer</li>
 * <li>{@code sources} (list) — retrieved document references</li>
 * <li>{@code context} (string) — the retrieved context used</li>
 * </ul>
 *
 * @author Bhangun
 */
@ApplicationScoped
@SkillDescriptor(id = "rag", name = "RAG — Retrieval Augmented Generation", description = "Search a vector store for relevant documents and generate a grounded answer.", version = "1.0.0", category = SkillCategory.RETRIEVAL, inputs = {
        @SkillDescriptor.Input(name = "query", description = "The question to answer"),
        @SkillDescriptor.Input(name = "collection", required = false, description = "Vector store collection name"),
        @SkillDescriptor.Input(name = "topK", type = "integer", required = false, description = "Number of documents to retrieve"),
        @SkillDescriptor.Input(name = "model", required = false, description = "LLM model override"),
        @SkillDescriptor.Input(name = "embeddingModel", required = false, description = "Embedding model override")
}, outputs = {
        @SkillDescriptor.Output(name = "answer", description = "Generated answer"),
        @SkillDescriptor.Output(name = "sources", type = "array", description = "Retrieved document references"),
        @SkillDescriptor.Output(name = "context", description = "Retrieved context used")
}, triggers = { "search documents", "find in knowledge base", "rag", "lookup",
        "retrieve and answer" }, aliases = { "retrieval", "knowledge-search", "semantic-search" })
public class RAGSkill implements AgentSkill {

    private static final Logger LOG = Logger.getLogger(RAGSkill.class);

    @Inject
    InferenceService inferenceService;

    @ConfigProperty(name = "gollek.agent.skills.rag.default-collection", defaultValue = "default")
    String defaultCollection;

    @ConfigProperty(name = "gollek.agent.skills.rag.default-top-k", defaultValue = "5")
    int defaultTopK;

    @ConfigProperty(name = "gollek.agent.skills.rag.embedding-model", defaultValue = "")
    String defaultEmbeddingModel;

    @ConfigProperty(name = "gollek.agent.skills.rag.vector-store-enabled", defaultValue = "false")
    boolean vectorStoreEnabled;

    @Override
    public String id() {
        return "rag";
    }

    @Override
    public String name() {
        return "RAG — Retrieval Augmented Generation";
    }

    @Override
    public String description() {
        return "Search a vector store for relevant documents and generate a grounded answer.";
    }

    @Override
    public SkillCategory category() {
        return SkillCategory.RETRIEVAL;
    }

    @Override
    public int priority() {
        return 20;
    }

    @Override
    public boolean isHealthy() {
        return vectorStoreEnabled;
    }

    @Override
    public Uni<SkillResult> execute(SkillContext ctx) {
        ctx.requireInput("query");
        long start = System.currentTimeMillis();

        String query = ctx.getStringInput("query");
        String collection = ctx.getStringInput("collection", defaultCollection);
        int topK = ctx.getIntInput("topK", defaultTopK);
        String model = ctx.getStringInput("model");

        LOG.debugf("RAGSkill: query='%s', collection=%s, topK=%d", query, collection, topK);

        if (!vectorStoreEnabled) {
            // Graceful degradation: fall through to direct inference without retrieval
            LOG.warn("RAGSkill: vector store disabled — executing direct inference without retrieval");
            return executeDirectInference(ctx, query, "", start);
        }

        // Step 1: embed the query
        return embedQuery(ctx, query)
                // Step 2: retrieve documents (stub — real impl calls vector store client)
                .chain(embedding -> retrieveDocuments(ctx, embedding, collection, topK))
                // Step 3: augment and generate
                .chain(docs -> {
                    String context = formatDocuments(docs);
                    return executeDirectInference(ctx, query, context, start)
                            .map(result -> result.isSuccess()
                                    ? SkillResult.builder()
                                            .skillId(id())
                                            .invocationId(ctx.invocationId())
                                            .status(SkillResult.Status.SUCCESS)
                                            .observation(java.util.Objects
                                                    .requireNonNullElse(result.getOutput("answer", String.class), ""))
                                            .output("answer",
                                                    java.util.Objects.requireNonNullElse(
                                                            result.getOutput("answer", String.class), ""))
                                            .output("sources", docs.stream().map(d -> d.get("source")).toList())
                                            .output("context", context)
                                            .durationMs(System.currentTimeMillis() - start)
                                            .build()
                                    : result);
                })
                .onFailure().recoverWithItem(err -> {
                    LOG.errorf(err, "RAGSkill failed: query='%s'", query);
                    return SkillResult.builder()
                            .skillId(id())
                            .invocationId(ctx.invocationId())
                            .status(SkillResult.Status.ERROR)
                            .observation(err.getMessage())
                            .error(err)
                            .build();
                });
    }

    // ── Internal steps ─────────────────────────────────────────────────────────

    private Uni<float[]> embedQuery(SkillContext ctx, String query) {
        String embModel = ctx.getStringInput("embeddingModel", defaultEmbeddingModel);
        EmbeddingRequest req = EmbeddingRequest.builder()
                .requestId("rag-embed-" + ctx.invocationId())
                .model(embModel.isBlank() ? "text-embedding-ada-002" : embModel)
                .input(query)
                .build();
        return inferenceService.executeEmbedding(req)
                .map(resp -> resp.embeddings().isEmpty() ? new float[0] : toFloatArray(resp.embeddings().get(0)));
    }

    private Uni<List<Map<String, Object>>> retrieveDocuments(
            SkillContext ctx, float[] embedding, String collection, int topK) {
        // TODO: inject VectorStoreClient and call similarity search
        // For now, return empty list — real implementation queries Qdrant/Weaviate/etc.
        LOG.debugf("RAGSkill: vector search in collection=%s, topK=%d", collection, topK);
        return Uni.createFrom().item(List.of());
    }

    private Uni<SkillResult> executeDirectInference(
            SkillContext ctx, String query, String context, long start) {
        String augmentedPrompt = context.isBlank()
                ? query
                : "Context:\n" + context + "\n\nQuestion: " + query + "\n\nAnswer based on the context above:";

        InferenceRequest.Builder reqBuilder = InferenceRequest.builder()
                .requestId("rag-infer-" + ctx.invocationId())
                .message(Message.system("You are a helpful assistant. Answer based on the provided context."))
                .message(Message.user(augmentedPrompt))
                .parameter("max_tokens", 1024)
                .parameter("temperature", 0.1)
                .metadata("tenantId", ctx.tenantId());

        ctx.getStringInput("model", null);
        return inferenceService.inferAsync(reqBuilder.build())
                .map(resp -> SkillResult.builder()
                        .skillId(id()).invocationId(ctx.invocationId())
                        .status(SkillResult.Status.SUCCESS)
                        .observation(resp.getContent())
                        .output("answer", resp.getContent())
                        .output("tokensUsed", resp.getTokensUsed())
                        .durationMs(System.currentTimeMillis() - start)
                        .build());
    }

    private String formatDocuments(List<Map<String, Object>> docs) {
        if (docs.isEmpty())
            return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < docs.size(); i++) {
            Map<String, Object> doc = docs.get(i);
            sb.append("[").append(i + 1).append("] ").append(doc.getOrDefault("text", "")).append("\n");
        }
        return sb.toString();
    }

    private float[] toFloatArray(float[] arr) {
        return arr;
    }
}
