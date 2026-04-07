package tech.kayys.wayang.rag.core;

import tech.kayys.wayang.rag.core.store.VectorStore;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.gamelan.engine.error.ErrorInfo;
import tech.kayys.gamelan.engine.node.NodeExecutionResult;
import tech.kayys.gamelan.engine.node.NodeExecutionTask;
import tech.kayys.gamelan.sdk.executor.core.SimpleNodeExecutionResult;
import tech.kayys.gamelan.sdk.executor.core.AbstractWorkflowExecutor;
import tech.kayys.gamelan.engine.protocol.CommunicationType;
import tech.kayys.gamelan.sdk.executor.core.Executor;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * RAG RETRIEVAL & RERANKING EXECUTOR - INTERNAL IMPLEMENTATION
 */
@Executor(executorType = "rag-retrieval", communicationType = CommunicationType.GRPC, maxConcurrentTasks = 20, supportedNodeTypes = {
        "TASK", "RAG_RETRIEVAL" }, version = "1.0.1")
@ApplicationScoped
public class RetrievalExecutor extends AbstractWorkflowExecutor {

    private static final Logger LOG = LoggerFactory.getLogger(RetrievalExecutor.class);

    public record RetrievalContext(
            String query,
            int topK,
            int finalK,
            double minScore,
            String strategy,
            boolean enableReranking,
            boolean enableDiversity,
            boolean enableQueryExpansion,
            Map<String, Object> filters,
            String storeType,
            String tenantId,
            RetrievalConfig config) {
    }

    @Inject
    VectorStoreRegistry storeRegistry;

    @Inject
    RetrievalStrategyFactory strategyFactory;

    @Inject
    RerankingPipeline rerankingPipeline;

    @Inject
    QueryExpansionService queryExpansion;

    @Inject
    RetrievalMetricsCollector metricsCollector;

    @ConfigProperty(name = "gamelan.rag.retrieval.top-k", defaultValue = "20")
    int defaultTopK;

    @ConfigProperty(name = "gamelan.rag.retrieval.final-k", defaultValue = "5")
    int defaultFinalK;

    @ConfigProperty(name = "gamelan.rag.retrieval.min-score", defaultValue = "0.7")
    double defaultMinScore;

    @ConfigProperty(name = "gamelan.rag.retrieval.strategy", defaultValue = "hybrid")
    String defaultStrategy;

    @ConfigProperty(name = "gamelan.rag.retrieval.rerank", defaultValue = "true")
    boolean defaultEnableReranking;

    @ConfigProperty(name = "gamelan.rag.retrieval.diversity", defaultValue = "true")
    boolean defaultEnableDiversity;

    @ConfigProperty(name = "gamelan.rag.retrieval.query-expansion", defaultValue = "false")
    boolean defaultQueryExpansion;

    @Override
    public Uni<NodeExecutionResult> execute(NodeExecutionTask task) {
        LOG.info("Starting retrieval for run: {}, node: {}",
                task.runId().value(), task.nodeId().value());

        Instant startTime = Instant.now();
        Map<String, Object> context = task.context();

        RetrievalContext retCtx = extractConfiguration(context);

        return validateConfiguration(retCtx)
                .flatMap(valid -> {
                    if (!valid) {
                        return Uni.createFrom().item(SimpleNodeExecutionResult.failure(
                                task.runId(),
                                task.nodeId(),
                                task.attempt(),
                                new ErrorInfo(
                                        "CONFIG_INVALID",
                                        "Invalid retrieval configuration",
                                        "",
                                        Map.of("retryable", false)),
                                task.token()));
                    }

                    return performRetrieval(retCtx, task.runId().value())
                            .map(result -> {
                                long durationMs = Duration.between(startTime, Instant.now()).toMillis();

                                metricsCollector.recordRetrieval(
                                        task.runId().value(),
                                        result.resultsRetrieved(),
                                        result.finalResults(),
                                        durationMs,
                                        result.avgScore());

                                return SimpleNodeExecutionResult.success(
                                        task.runId(),
                                        task.nodeId(),
                                        task.attempt(),
                                        Map.of(
                                                "query", retCtx.query(),
                                                "resultsRetrieved", result.resultsRetrieved(),
                                                "finalResults", result.finalResults(),
                                                "avgScore", result.avgScore(),
                                                "maxScore", result.maxScore(),
                                                "minScore", result.minScore(),
                                                "durationMs", durationMs,
                                                "contexts", result.contexts(),
                                                "metadata", result.metadata(),
                                                "reranked", result.reranked()),
                                        task.token(),
                                        Duration.ofMillis(durationMs));
                            })
                            .onFailure().recoverWithItem(error -> {
                                LOG.error("Retrieval failed", error);
                                return SimpleNodeExecutionResult.failure(
                                        task.runId(),
                                        task.nodeId(),
                                        task.attempt(),
                                        new ErrorInfo(
                                                "RAG_RETRIEVAL_FAILED",
                                                error.getMessage(),
                                                "",
                                                Map.of("retryable", true)),
                                        task.token());
                            });
                });
    }

    private Uni<RetrievalResult> performRetrieval(RetrievalContext retCtx, String workflowRunId) {
        LOG.debug("Performing retrieval for query: '{}' (strategy: {})",
                retCtx.query(), retCtx.strategy());

        return Uni.createFrom().item(() -> {
            // Query expansion
            List<String> queries = new ArrayList<>();
            queries.add(retCtx.query());

            if (retCtx.enableQueryExpansion()) {
                queries.addAll(queryExpansion.expand(retCtx.query(), 2));
                LOG.debug("Expanded to {} query variations", queries.size());
            }

            // Initial retrieval
            RetrievalStrategy strategy = strategyFactory.getStrategy(retCtx.strategy());
            List<ScoredDocument> initialResults = new ArrayList<>();

            for (String query : queries) {
                VectorStore<RagChunk> store = storeRegistry.getStore(
                        retCtx.tenantId(),
                        retCtx.storeType());

                List<ScoredDocument> queryResults = strategy.retrieve(query, store, retCtx.config());
                initialResults.addAll(queryResults);
            }

            // Deduplicate by content
            Map<String, ScoredDocument> deduplicated = new LinkedHashMap<>();
            for (ScoredDocument doc : initialResults) {
                String key = doc.segment().text();
                if (!deduplicated.containsKey(key) ||
                        deduplicated.get(key).score() < doc.score()) {
                    deduplicated.put(key, doc);
                }
            }

            List<ScoredDocument> uniqueResults = new ArrayList<>(deduplicated.values());
            uniqueResults.sort(Comparator.comparingDouble(ScoredDocument::score).reversed());
            uniqueResults = uniqueResults.stream().limit(retCtx.topK()).collect(Collectors.toList());

            LOG.debug("Retrieved {} unique results after deduplication", uniqueResults.size());

            // Apply filters
            List<ScoredDocument> filteredResults = applyFilters(uniqueResults, retCtx);
            LOG.debug("Filtered to {} results", filteredResults.size());

            // Reranking
            List<ScoredDocument> rerankedResults = filteredResults;
            boolean wasReranked = false;

            if (retCtx.enableReranking() && filteredResults.size() > retCtx.finalK()) {
                rerankedResults = rerankingPipeline.rerank(
                        retCtx.query(),
                        filteredResults,
                        retCtx.finalK());
                wasReranked = true;
                LOG.debug("Reranked to {} results", rerankedResults.size());
            }

            // Diversity filtering using MMR
            List<ScoredDocument> finalResults = rerankedResults;

            if (retCtx.enableDiversity() && rerankedResults.size() > retCtx.finalK()) {
                finalResults = applyMMR(rerankedResults, retCtx.finalK(), 0.5);
                LOG.debug("Applied MMR, selected {} diverse results", finalResults.size());
            }

            // Limit to final k
            finalResults = finalResults.stream()
                    .limit(retCtx.finalK())
                    .collect(Collectors.toList());

            // Extract contexts and metadata
            List<String> contexts = finalResults.stream()
                    .map(doc -> doc.segment().text())
                    .collect(Collectors.toList());

            List<Map<String, Object>> metadata = finalResults.stream()
                    .map(doc -> new HashMap<>(doc.segment().metadata()))
                    .collect(Collectors.toList());

            // Calculate statistics
            OptionalDouble avgScore = finalResults.stream()
                    .mapToDouble(ScoredDocument::score)
                    .average();

            OptionalDouble maxScore = finalResults.stream()
                    .mapToDouble(ScoredDocument::score)
                    .max();

            OptionalDouble minScore = finalResults.stream()
                    .mapToDouble(ScoredDocument::score)
                    .min();

            return new RetrievalResult(
                    uniqueResults.size(),
                    finalResults.size(),
                    avgScore.orElse(0.0),
                    maxScore.orElse(0.0),
                    minScore.orElse(0.0),
                    contexts,
                    metadata,
                    wasReranked);
        });
    }

    private List<ScoredDocument> applyFilters(List<ScoredDocument> results, RetrievalContext retCtx) {
        if (retCtx.filters() == null || retCtx.filters().isEmpty()) {
            return results;
        }

        return results.stream()
                .filter(doc -> {
                    Map<String, Object> metadata = doc.segment().metadata();
                    for (Map.Entry<String, Object> filter : retCtx.filters().entrySet()) {
                        Object value = metadata.get(filter.getKey());
                        if (value == null || !value.equals(filter.getValue())) {
                            return false;
                        }
                    }
                    return true;
                })
                .collect(Collectors.toList());
    }

    private List<ScoredDocument> applyMMR(List<ScoredDocument> docs, int k, double lambda) {
        if (docs.isEmpty())
            return docs;

        List<ScoredDocument> selected = new ArrayList<>();
        List<ScoredDocument> remaining = new ArrayList<>(docs);

        // Select first document
        selected.add(remaining.remove(0));

        // Iteratively select documents maximizing MMR
        while (selected.size() < k && !remaining.isEmpty()) {
            ScoredDocument best = null;
            double bestMMR = Double.NEGATIVE_INFINITY;
            int bestIndex = -1;

            for (int i = 0; i < remaining.size(); i++) {
                ScoredDocument candidate = remaining.get(i);

                double relevance = candidate.score();
                double maxSimilarity = selected.stream()
                        .mapToDouble(sel -> cosineSimilarity(candidate.segment().text(), sel.segment().text()))
                        .max()
                        .orElse(0.0);

                double mmr = lambda * relevance - (1 - lambda) * maxSimilarity;

                if (mmr > bestMMR) {
                    bestMMR = mmr;
                    best = candidate;
                    bestIndex = i;
                }
            }

            if (best != null) {
                selected.add(best);
                remaining.remove(bestIndex);
            } else {
                break;
            }
        }

        return selected;
    }

    private double cosineSimilarity(String text1, String text2) {
        Set<String> words1 = new HashSet<>(Arrays.asList(text1.toLowerCase().split("\\s+")));
        Set<String> words2 = new HashSet<>(Arrays.asList(text2.toLowerCase().split("\\s+")));

        Set<String> intersection = new HashSet<>(words1);
        intersection.retainAll(words2);

        if (words1.isEmpty() || words2.isEmpty())
            return 0.0;

        return intersection.size() / Math.sqrt(words1.size() * words2.size());
    }

    @SuppressWarnings("unchecked")
    private RetrievalContext extractConfiguration(Map<String, Object> context) {
        String query = (String) context.get("query");
        int topK = context.containsKey("topK") ? ((Number) context.get("topK")).intValue() : defaultTopK;
        int finalK = context.containsKey("finalK") ? ((Number) context.get("finalK")).intValue() : defaultFinalK;
        double minScore = context.containsKey("minScore") ? ((Number) context.get("minScore")).doubleValue()
                : defaultMinScore;
        String strategy = (String) context.getOrDefault("strategy", defaultStrategy);
        boolean enableReranking = context.containsKey("enableReranking") ? (Boolean) context.get("enableReranking")
                : defaultEnableReranking;
        boolean enableDiversity = context.containsKey("enableDiversity") ? (Boolean) context.get("enableDiversity")
                : defaultEnableDiversity;
        boolean enableQueryExpansion = context.containsKey("enableQueryExpansion")
                ? (Boolean) context.get("enableQueryExpansion")
                : defaultQueryExpansion;

        Map<String, Object> filters = (Map<String, Object>) context.getOrDefault("filters", Map.of());
        String storeType = (String) context.getOrDefault("storeType", "in-memory");
        String tenantId = (String) context.getOrDefault("tenantId", "default");

        RetrievalConfig config = new RetrievalConfig(topK, (float) minScore, 512, 50, enableReranking,
                RerankingModel.COHERE_RERANK, false, 0.7f, false, 3, false, 0, filters, List.of(), false, false);

        return new RetrievalContext(
                query, topK, finalK, minScore, strategy,
                enableReranking, enableDiversity, enableQueryExpansion,
                filters, storeType, tenantId, config);
    }

    private Uni<Boolean> validateConfiguration(RetrievalContext retCtx) {
        return Uni.createFrom().item(() -> {
            if (retCtx.query() == null || retCtx.query().isBlank()) {
                LOG.error("No query provided");
                return false;
            }
            if (retCtx.topK() <= 0 || retCtx.topK() > 1000) {
                LOG.error("Invalid topK: {}", retCtx.topK());
                return false;
            }
            if (retCtx.finalK() <= 0 || retCtx.finalK() > retCtx.topK()) {
                LOG.error("Invalid finalK: {}", retCtx.finalK());
                return false;
            }
            if (retCtx.minScore() < 0.0 || retCtx.minScore() > 1.0) {
                LOG.error("Invalid minScore: {}", retCtx.minScore());
                return false;
            }
            return true;
        });
    }

    @Override
    public boolean canHandle(NodeExecutionTask task) {
        return task.context().containsKey("query");
    }
}
