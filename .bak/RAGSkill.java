package tech.kayys.gollek.agent.skills.builtin;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import tech.kayys.gollek.agent.memory.AgentMemory;
import tech.kayys.gollek.agent.memory.EmbeddingService;
import tech.kayys.gollek.agent.skill.SkillCategory;
import tech.kayys.gollek.agent.skill.SkillContext;
import tech.kayys.gollek.agent.skill.SkillDescriptor;
import tech.kayys.gollek.agent.skill.SkillResult;
import tech.kayys.gollek.agent.spi.AgentSkill;
import tech.kayys.gollek.agent.toolcalling.ToolSchema;
import tech.kayys.gollek.agent.orchestrator.NativeToolCallingOrchestrator;
import tech.kayys.gollek.engine.inference.InferenceService;
import tech.kayys.gollek.spi.Message;
import tech.kayys.gollek.spi.inference.InferenceRequest;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Retrieval-Augmented Generation (RAG) skill with real vector search via EmbeddingService + AgentMemory.
 *
 * Pipeline: embed query → vector search (pgvector) → keyword fallback → generate answer.
 */
@ApplicationScoped
@SkillDescriptor(
        id       = "rag",
        name     = "Retrieval-Augmented Generation",
        description = "Searches vector memory for relevant context, then generates a grounded answer.",
        category = SkillCategory.RETRIEVAL,
        triggers = {"find in knowledge base","search documents","what do we know about","retrieve","rag"}
)
public class RAGSkill implements AgentSkill, NativeToolCallingOrchestrator.ToolSchemaProvider {

    private static final Logger LOG = Logger.getLogger(RAGSkill.class);

    @Inject EmbeddingService embeddingService;
    @Inject AgentMemory      memory;
    @Inject InferenceService inferenceService;

    @ConfigProperty(name = "gollek.agent.skills.rag.default-top-k",    defaultValue = "5")
    int defaultTopK;
    @ConfigProperty(name = "gollek.agent.skills.rag.embedding-model",  defaultValue = "default")
    String embeddingModel;

    @Override
    public Uni<SkillResult> execute(SkillContext ctx) {
        long t0         = System.currentTimeMillis();
        String query    = ctx.getStringInput("query");
        if (query == null || query.isBlank())
            return Uni.createFrom().item(SkillResult.failure(id(), "Input 'query' is required.", false));

        String tenantId    = ctx.getStringInput("tenant_id", ctx.getTenantId());
        int    topK        = ctx.getIntInput("top_k", defaultTopK);
        String model       = ctx.getStringInput("model", "default");
        String embModel    = ctx.getStringInput("embedding_model", embeddingModel);
        float  minSim      = Float.parseFloat(ctx.getStringInput("min_similarity", "0.3"));
        boolean storeBack  = Boolean.parseBoolean(ctx.getStringInput("store_result", "false"));

        return embeddingService.embed(query, embModel)
                .chain(vec -> retrievePassages(tenantId, vec, query, topK, minSim))
                .chain(passages -> generateAnswer(ctx, query, passages, model, tenantId, storeBack, t0));
    }

    private Uni<List<RetrievedPassage>> retrievePassages(String tenantId, float[] queryVec,
                                                          String query, int topK, float minSim) {
        return memory.searchFacts(tenantId, queryVec, topK * 2)
                .map(facts -> facts.stream()
                        .map(f -> {
                            float score = (queryVec != null && queryVec.length > 0)
                                    ? f.similarity()
                                    : keywordOverlap(query, f.text());
                            return new RetrievedPassage(f.key(), f.text(), score, f.metadata());
                        })
                        .filter(p -> p.similarity() >= minSim)
                        .sorted(Comparator.comparingDouble(RetrievedPassage::similarity).reversed())
                        .limit(topK)
                        .collect(Collectors.toList()));
    }

    private Uni<SkillResult> generateAnswer(SkillContext ctx, String query,
                                             List<RetrievedPassage> passages, String model,
                                             String tenantId, boolean storeBack, long t0) {
        boolean grounded = !passages.isEmpty();
        String context   = grounded ? buildContext(passages) : "";

        String sysPrompt = grounded
                ? "Answer ONLY using the provided context. Cite relevant parts. Say 'I don't know' if not in context."
                : "Answer based on your knowledge.";
        String userMsg   = grounded ? "Context:\n" + context + "\n\nQuestion: " + query : query;

        InferenceRequest ir = InferenceRequest.builder()
                .requestId(ctx.getInvocationId() + "-gen")
                .model(model)
                .message(Message.system(sysPrompt))
                .message(Message.user(userMsg))
                .parameter("max_tokens", 512)
                .parameter("temperature", 0.1)
                .metadata("tenantId", tenantId)
                .build();

        return inferenceService.inferAsync(ir)
                .map(r -> r.getContent())
                .chain(answer -> {
                    Uni<Void> store = Uni.createFrom().voidItem();
                    if (storeBack && grounded && answer != null && !answer.isBlank()) {
                        String factKey = "qa:" + UUID.nameUUIDFromBytes(query.getBytes());
                        store = embeddingService.embed(query + "\n" + answer, embeddingModel)
                                .chain(v -> memory.storeFact(tenantId, factKey,
                                        "Q: " + query + "\nA: " + answer, v,
                                        Map.of("type", "qa")));
                    }
                    return store.map(v -> SkillResult.builder()
                            .skillId(id()).status(SkillResult.Status.SUCCESS)
                            .observation(answer != null ? answer : "")
                            .output("answer", answer)
                            .output("grounded", grounded)
                            .output("passages_count", passages.size())
                            .output("passages", passages.stream()
                                    .map(p -> Map.of("text", p.text(), "score",
                                            String.format("%.3f", p.similarity())))
                                    .collect(Collectors.toList()))
                            .durationMs(System.currentTimeMillis() - t0)
                            .build());
                });
    }

    private String buildContext(List<RetrievedPassage> passages) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < passages.size(); i++)
            sb.append("[").append(i+1).append("](sim=")
              .append(String.format("%.2f",passages.get(i).similarity())).append(")\n")
              .append(passages.get(i).text()).append("\n\n");
        return sb.toString().strip();
    }

    private float keywordOverlap(String query, String text) {
        Set<String> q = tokenize(query), t = tokenize(text);
        if (q.isEmpty() || t.isEmpty()) return 0f;
        long common = q.stream().filter(t::contains).count();
        return (float)(common / Math.sqrt((double) q.size() * t.size()));
    }

    private Set<String> tokenize(String s) {
        return Arrays.stream(s.toLowerCase().split("[^a-z0-9]+"))
                .filter(t -> t.length() > 2).collect(Collectors.toSet());
    }

    @Override
    public ToolSchema toolSchema() {
        return ToolSchema.builder(id()).description(description())
                .requiredParam("query","string","Search query or question.")
                .param("top_k","integer","Number of passages to retrieve (default: "+defaultTopK+").")
                .param("min_similarity","number","Minimum cosine similarity threshold (default: 0.3).")
                .param("store_result","boolean","Store Q&A pair back into memory (default: false).")
                .build();
    }

    @Override public String id()          { return "rag"; }
    @Override public String name()        { return "Retrieval-Augmented Generation"; }
    @Override public String description() { return "Searches vector memory for relevant context and generates a grounded answer."; }
    @Override public String version()     { return "2.0.0"; }
    @Override public SkillCategory category() { return SkillCategory.RETRIEVAL; }
    @Override public boolean canHandle(SkillContext ctx) { return true; }

    private record RetrievedPassage(String key, String text, float similarity, Map<String, Object> metadata) {}
}
