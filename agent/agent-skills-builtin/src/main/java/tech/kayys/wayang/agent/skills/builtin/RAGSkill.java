package tech.kayys.wayang.agent.skills.builtin;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import tech.kayys.wayang.agent.spi.AgentSkill;
import tech.kayys.wayang.agent.spi.InferenceBackend;
import tech.kayys.wayang.agent.spi.skills.SkillCategory;
import tech.kayys.wayang.agent.spi.skills.SkillDescriptor;
import tech.kayys.wayang.agent.spi.skills.rag.RagSkillRetrievedDocument;
import tech.kayys.wayang.agent.spi.skills.rag.RagSkillRetrievalRequest;
import tech.kayys.wayang.agent.spi.skills.rag.RagSkillRetrievalResult;
import tech.kayys.wayang.agent.spi.skills.rag.RagSkillRetriever;
import tech.kayys.wayang.embedding.EmbeddingRequest;
import tech.kayys.wayang.embedding.EmbeddingResponse;
import tech.kayys.wayang.embedding.EmbeddingService;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
@SkillDescriptor(id = "rag", name = "RAG - Retrieval Augmented Generation", description = "Generates a grounded answer using optional retrieved context and the configured inference backend.", version = "1.0.0", category = SkillCategory.RETRIEVAL, inputs = {
        @SkillDescriptor.Input(name = "query", description = "The question to answer"),
        @SkillDescriptor.Input(name = "context", required = false, description = "Pre-retrieved context to ground the answer"),
        @SkillDescriptor.Input(name = "collection", required = false, description = "Retriever collection or namespace"),
        @SkillDescriptor.Input(name = "filters", type = "object", required = false, description = "Retriever metadata filters"),
        @SkillDescriptor.Input(name = "model", required = false, description = "Inference model override"),
        @SkillDescriptor.Input(name = "embeddingModel", required = false, description = "Embedding model override")
}, outputs = {
        @SkillDescriptor.Output(name = "answer", description = "Generated answer"),
        @SkillDescriptor.Output(name = "context", description = "Context used"),
        @SkillDescriptor.Output(name = "sources", type = "array", description = "Retrieved document references")
}, triggers = { "search documents", "find in knowledge base", "rag", "lookup", "retrieve and answer" }, aliases = { "retrieval", "knowledge-search", "semantic-search" }, priority = 20)
public class RAGSkill implements AgentSkill {

    private static final Logger LOG = Logger.getLogger(RAGSkill.class);

    @Inject
    InferenceBackend inferenceBackend;

    @Inject
    EmbeddingService embeddingService;

    @Inject
    Instance<RagSkillRetriever> retrieverInstances;

    RagSkillRetriever retriever;

    @ConfigProperty(name = "gollek.agent.skills.rag.vector-store-enabled", defaultValue = "false")
    boolean vectorStoreEnabled = false;

    @ConfigProperty(name = "gollek.agent.skills.rag.default-collection", defaultValue = "default")
    String defaultCollection = "default";

    @ConfigProperty(name = "gollek.agent.skills.rag.default-top-k", defaultValue = "5")
    int defaultTopK = 5;

    @Override
    public String id() {
        return "rag";
    }

    @Override
    public String name() {
        return "RAG - Retrieval Augmented Generation";
    }

    @Override
    public String description() {
        return "Generates grounded answers with optional retrieved context.";
    }

    @Override
    public String category() {
        return SkillCategory.RETRIEVAL.name();
    }

    @Override
    public int priority() {
        return 20;
    }

    @Override
    public boolean canHandle(Map<String, Object> inputs) {
        return inputs != null && inputs.containsKey("query");
    }

    @Override
    public Uni<Map<String, Object>> execute(Map<String, Object> context) {
        Map<String, Object> inputs = context == null ? Map.of() : context;
        String query = BuiltinSkillSupport.stringInput(inputs, "query");
        if (query == null || query.isBlank()) {
            return Uni.createFrom().item(BuiltinSkillSupport.failure("Input 'query' is required"));
        }
        String normalizedQuery = query.trim();
        if (inferenceBackend == null) {
            return Uni.createFrom().item(BuiltinSkillSupport.failure("Inference backend is not configured"));
        }

        Uni<EmbeddingResponse> queryEmbedding = shouldEmbedForRetrieval()
                ? embeddingService.embed(new EmbeddingRequest(
                        List.of(normalizedQuery),
                        BuiltinSkillSupport.stringInput(inputs, "embeddingModel"),
                        null,
                        true))
                : Uni.createFrom().nullItem();

        return queryEmbedding
                .onFailure().recoverWithItem(error -> {
                    LOG.warnf(error, "RAG query embedding failed; continuing without retrieval vector");
                    return null;
                })
                .flatMap(embedding -> retrieve(inputs, normalizedQuery, embedding)
                        .flatMap(retrieval -> inferAnswer(inputs, normalizedQuery, embedding, retrieval)));
    }

    private Uni<Map<String, Object>> inferAnswer(
            Map<String, Object> inputs,
            String query,
            EmbeddingResponse embedding,
            RagSkillRetrievalResult retrieval) {
        String suppliedContext = BuiltinSkillSupport.stringInput(inputs, "context", "");
        String context = mergeContext(suppliedContext, retrieval);
        String prompt = context.isBlank()
                ? query
                : "Context:\n" + context + "\n\nQuestion: " + query + "\n\nAnswer based on the context above:";
        String model = BuiltinSkillSupport.stringInput(inputs, "model");
        int maxTokens = BuiltinSkillSupport.intInput(inputs, "maxTokens", 1024);
        long start = System.currentTimeMillis();

        return inferenceBackend.infer(BuiltinSkillSupport.textRequest(
                        "skill-rag-" + UUID.randomUUID(),
                        model,
                        "You are a helpful assistant. Answer based on the provided context when context is present.",
                        prompt,
                        maxTokens,
                        0.1,
                        BuiltinSkillSupport.stringInput(inputs, "tenantId")))
                .map(response -> {
                    String answer = BuiltinSkillSupport.responseContent(response);
                    Map<String, Object> outputs = new LinkedHashMap<>();
                    outputs.put("answer", answer);
                    outputs.put("context", context);
                    outputs.put("sources", sourceSummaries(retrieval));
                    outputs.put("tokensUsed", BuiltinSkillSupport.totalTokens(response));
                    outputs.put("durationMs", System.currentTimeMillis() - start);
                    outputs.put("model", BuiltinSkillSupport.responseModel(response, model));
                    outputs.put("topK", topK(inputs));
                    if (embedding != null) {
                        outputs.put("query_embedding_dimension", embedding.dimension());
                    }
                    return BuiltinSkillSupport.success(answer, outputs);
                })
                .onFailure().recoverWithItem(BuiltinSkillSupport::error);
    }

    private Uni<RagSkillRetrievalResult> retrieve(
            Map<String, Object> inputs,
            String query,
            EmbeddingResponse embedding) {
        RagSkillRetriever activeRetriever = activeRetriever();
        if (!vectorStoreEnabled || activeRetriever == null) {
            return Uni.createFrom().item(RagSkillRetrievalResult.empty());
        }
        RagSkillRetrievalRequest request = new RagSkillRetrievalRequest(
                BuiltinSkillSupport.stringInput(inputs, "tenantId"),
                query,
                collection(inputs),
                topK(inputs),
                firstEmbedding(embedding),
                BuiltinSkillSupport.objectMapInput(inputs, "filters"));
        return activeRetriever.retrieve(request)
                .onItem().ifNull().continueWith(RagSkillRetrievalResult.empty())
                .onFailure().recoverWithItem(error -> {
                    LOG.warnf(error, "RAG retrieval failed; continuing with supplied context only");
                    return RagSkillRetrievalResult.empty();
                });
    }

    private boolean shouldEmbedForRetrieval() {
        return vectorStoreEnabled && embeddingService != null && activeRetriever() != null;
    }

    private float[] firstEmbedding(EmbeddingResponse embedding) {
        if (embedding == null) {
            return null;
        }
        try {
            return embedding.first();
        } catch (RuntimeException error) {
            LOG.warnf(error, "RAG query embedding response was empty; retrieving without vector");
            return null;
        }
    }

    private RagSkillRetriever activeRetriever() {
        if (retriever != null) {
            return retriever;
        }
        if (retrieverInstances == null || retrieverInstances.isUnsatisfied()) {
            return null;
        }
        for (RagSkillRetriever candidate : retrieverInstances) {
            return candidate;
        }
        return null;
    }

    private int topK(Map<String, Object> inputs) {
        return Math.max(1, BuiltinSkillSupport.intInput(inputs, "topK", defaultTopK));
    }

    private String collection(Map<String, Object> inputs) {
        String collection = BuiltinSkillSupport.stringInput(inputs, "collection", defaultCollection);
        return collection == null || collection.isBlank() ? defaultCollection : collection.trim();
    }

    private String mergeContext(String suppliedContext, RagSkillRetrievalResult retrieval) {
        String safeSuppliedContext = suppliedContext == null ? "" : suppliedContext.trim();
        String retrievedContext = retrieval == null ? "" : retrieval.context();
        if (safeSuppliedContext.isBlank()) {
            return retrievedContext;
        }
        if (retrievedContext.isBlank()) {
            return safeSuppliedContext;
        }
        return safeSuppliedContext + "\n\nRetrieved context:\n" + retrievedContext;
    }

    private List<Map<String, Object>> sourceSummaries(RagSkillRetrievalResult retrieval) {
        if (retrieval == null || retrieval.isEmpty()) {
            return List.of();
        }
        return retrieval.documents().stream()
                .map(RagSkillRetrievedDocument::sourceSummary)
                .toList();
    }
}
