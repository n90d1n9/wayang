package tech.kayys.wayang.rag.retrieval;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import tech.kayys.wayang.rag.core.RetrievalConfig;
import tech.kayys.wayang.rag.core.RagScoredChunk;
import tech.kayys.wayang.rag.core.RagEvalDataset;
import tech.kayys.wayang.rag.core.RagEvalQueryCase;
import tech.kayys.wayang.rag.runtime.NativeRagCoreService;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@ApplicationScoped
public class RagRetrievalEvalService {

    private static final String DEFAULT_MATCH_FIELD = "documentId";

    private final NativeRagCoreService nativeRagCoreService;
    private final ObjectMapper objectMapper;
    private final RagRetrievalEvalHistoryService historyService;
    private final RagRetrievalEvalMetrics evalMetrics;
    private final Clock clock;

    @Inject
    public RagRetrievalEvalService(
            NativeRagCoreService nativeRagCoreService,
            ObjectMapper objectMapper,
            RagRetrievalEvalHistoryService historyService,
            RagRetrievalEvalMetrics evalMetrics) {
        this(nativeRagCoreService, objectMapper, Clock.systemUTC(), historyService, evalMetrics);
    }

    RagRetrievalEvalService(NativeRagCoreService nativeRagCoreService, ObjectMapper objectMapper, Clock clock) {
        this(nativeRagCoreService, objectMapper, clock, null, null);
    }

    RagRetrievalEvalService(
            NativeRagCoreService nativeRagCoreService,
            ObjectMapper objectMapper,
            Clock clock,
            RagRetrievalEvalHistoryService historyService,
            RagRetrievalEvalMetrics evalMetrics) {
        this.nativeRagCoreService = Objects.requireNonNull(nativeRagCoreService, "nativeRagCoreService");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.historyService = historyService;
        this.evalMetrics = evalMetrics;
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public RagRetrievalEvalResponse evaluate(RagRetrievalEvalRequest request) {
        RagEvalDataset dataset = resolveDataset(request);
        String tenantId = firstNonBlank(
                request == null ? null : request.tenantId(),
                dataset.tenantId());
        if (tenantId == null) {
            throw new BadRequestException("tenantId is required either in request or dataset");
        }

        int topK = resolveTopK(request == null ? null : request.topK(), dataset.topK());
        double minSimilarity = resolveMinSimilarity(request == null ? null : request.minSimilarity(),
                dataset.minSimilarity());
        String matchField = normalizeMatchField(firstNonBlank(
                request == null ? null : request.matchField(),
                dataset.matchField(),
                DEFAULT_MATCH_FIELD));

        List<RagEvalQueryCase> queries = dataset.queries() == null ? List.of() : dataset.queries();
        if (queries.isEmpty()) {
            throw new BadRequestException("dataset.queries must not be empty");
        }

        Map<String, Object> requestFilters = request == null || request.filters() == null ? Map.of()
                : Map.copyOf(request.filters());
        Map<String, Object> datasetFilters = dataset.defaultFilters() == null ? Map.of()
                : Map.copyOf(dataset.defaultFilters());

        RetrievalConfig retrievalConfig = withThresholds(RetrievalConfig.defaults(), topK, (float) minSimilarity);

        List<RagRetrievalEvalCaseResult> results = new ArrayList<>(queries.size());
        List<Long> latencies = new ArrayList<>(queries.size());
        int hits = 0;

        for (int i = 0; i < queries.size(); i++) {
            RagEvalQueryCase queryCase = queries.get(i);
            if (queryCase == null || queryCase.query() == null || queryCase.query().isBlank()) {
                throw new BadRequestException("dataset.queries[" + i + "].query must not be blank");
            }

            Map<String, Object> effectiveFilters = mergeFilters(datasetFilters, requestFilters, queryCase.filters());
            long startedNanos = System.nanoTime();
            List<RagScoredChunk> chunks = nativeRagCoreService.retrieve(
                    tenantId,
                    queryCase.query(),
                    retrievalConfig,
                    effectiveFilters);
            long latencyMs = Math.max(0L, (System.nanoTime() - startedNanos) / 1_000_000L);

            List<String> retrievedIds = extractRetrievedIds(chunks, matchField);
            Set<String> expected = normalizeExpectedIds(queryCase.expectedIds());
            double recall = expected.isEmpty() ? 0.0 : recallAtK(expected, retrievedIds);
            double rr = reciprocalRank(expected, retrievedIds);
            if (rr > 0.0) {
                hits++;
            }

            results.add(new RagRetrievalEvalCaseResult(
                    queryCase.id() == null || queryCase.id().isBlank() ? "case-" + (i + 1) : queryCase.id(),
                    queryCase.query(),
                    List.copyOf(expected),
                    retrievedIds,
                    recall,
                    rr,
                    latencyMs));
            latencies.add(latencyMs);
        }

        double recallAtK = results.stream().mapToDouble(RagRetrievalEvalCaseResult::recall).average().orElse(0.0);
        double mrr = results.stream().mapToDouble(RagRetrievalEvalCaseResult::reciprocalRank).average().orElse(0.0);
        double latencyAvgMs = latencies.stream().mapToLong(Long::longValue).average().orElse(0.0);
        double latencyP95Ms = percentile95(latencies);

        RagRetrievalEvalResponse response = new RagRetrievalEvalResponse(
                dataset.name() == null || dataset.name().isBlank() ? "adhoc" : dataset.name(),
                tenantId,
                topK,
                minSimilarity,
                matchField,
                results.size(),
                hits,
                recallAtK,
                mrr,
                latencyP95Ms,
                latencyAvgMs,
                clock.instant(),
                List.copyOf(results));
        if (historyService != null) {
            historyService.append(response);
        }
        if (evalMetrics != null) {
            evalMetrics.recordRun(response);
        }
        return response;
    }

    private RagEvalDataset resolveDataset(RagRetrievalEvalRequest request) {
        if (request == null) {
            throw new BadRequestException("request body is required");
        }
        if (request.dataset() != null) {
            return request.dataset();
        }
        if (request.fixturePath() != null && !request.fixturePath().isBlank()) {
            return loadFixtureDataset(request.fixturePath().trim());
        }
        throw new BadRequestException("dataset or fixturePath is required");
    }

    private RagEvalDataset loadFixtureDataset(String fixturePath) {
        try {
            if (fixturePath.startsWith("classpath:")) {
                String resource = fixturePath.substring("classpath:".length());
                String normalized = resource.startsWith("/") ? resource.substring(1) : resource;
                InputStream input = Thread.currentThread().getContextClassLoader().getResourceAsStream(normalized);
                if (input == null) {
                    throw new BadRequestException("fixture resource not found: " + fixturePath);
                }
                try (input) {
                    return objectMapper.readValue(input, RagEvalDataset.class);
                }
            }
            String payload = Files.readString(Path.of(fixturePath));
            return objectMapper.readValue(payload, RagEvalDataset.class);
        } catch (IOException ex) {
            throw new BadRequestException("Failed to load fixture dataset: " + fixturePath, ex);
        }
    }

    private static RetrievalConfig withThresholds(RetrievalConfig defaults, int topK, float minSimilarity) {
        return new RetrievalConfig(
                topK,
                minSimilarity,
                defaults.maxChunkSize(),
                defaults.chunkOverlap(),
                defaults.enableReranking(),
                defaults.rerankingModel(),
                defaults.enableHybridSearch(),
                defaults.hybridAlpha(),
                defaults.enableMultiQuery(),
                defaults.numQueryVariations(),
                defaults.enableMmr(),
                defaults.mmrLambda(),
                defaults.metadataFilters(),
                defaults.excludedFields(),
                defaults.enableGrouping(),
                defaults.enableDeduplication());
    }

    private static Map<String, Object> mergeFilters(
            Map<String, Object> datasetFilters,
            Map<String, Object> requestFilters,
            Map<String, Object> caseFilters) {
        java.util.LinkedHashMap<String, Object> merged = new java.util.LinkedHashMap<>();
        if (datasetFilters != null) {
            merged.putAll(datasetFilters);
        }
        if (requestFilters != null) {
            merged.putAll(requestFilters);
        }
        if (caseFilters != null) {
            merged.putAll(caseFilters);
        }
        return Map.copyOf(merged);
    }

    private static Set<String> normalizeExpectedIds(List<String> expectedIds) {
        if (expectedIds == null || expectedIds.isEmpty()) {
            return Set.of();
        }
        LinkedHashSet<String> set = new LinkedHashSet<>();
        for (String id : expectedIds) {
            if (id != null) {
                String trimmed = id.trim();
                if (!trimmed.isEmpty()) {
                    set.add(trimmed);
                }
            }
        }
        return Set.copyOf(set);
    }

    private static List<String> extractRetrievedIds(List<RagScoredChunk> chunks, String matchField) {
        if (chunks == null || chunks.isEmpty()) {
            return List.of();
        }
        List<String> ids = new ArrayList<>(chunks.size());
        for (RagScoredChunk chunk : chunks) {
            if (chunk == null || chunk.chunk() == null) {
                continue;
            }
            String resolved = resolveChunkIdentifier(chunk, matchField);
            if (resolved != null && !resolved.isBlank()) {
                ids.add(resolved);
            }
        }
        return List.copyOf(ids);
    }

    private static String resolveChunkIdentifier(RagScoredChunk scoredChunk, String matchField) {
        if ("chunkid".equals(matchField)) {
            return scoredChunk.chunk().id();
        }
        if ("source".equals(matchField)) {
            Object source = scoredChunk.chunk().metadata().get("source");
            return source == null ? scoredChunk.chunk().documentId() : String.valueOf(source);
        }
        if (matchField.startsWith("metadata:")) {
            String key = matchField.substring("metadata:".length());
            if (key.isBlank()) {
                return scoredChunk.chunk().documentId();
            }
            Object value = scoredChunk.chunk().metadata().get(key);
            return value == null ? null : String.valueOf(value);
        }
        return scoredChunk.chunk().documentId();
    }

    private static double recallAtK(Set<String> expected, List<String> retrievedIds) {
        if (expected.isEmpty()) {
            return 0.0;
        }
        long matched = retrievedIds.stream().filter(expected::contains).count();
        return (double) matched / expected.size();
    }

    private static double reciprocalRank(Set<String> expected, List<String> retrievedIds) {
        for (int i = 0; i < retrievedIds.size(); i++) {
            if (expected.contains(retrievedIds.get(i))) {
                return 1.0 / (i + 1);
            }
        }
        return 0.0;
    }

    private static int resolveTopK(Integer requestTopK, Integer datasetTopK) {
        int value = requestTopK != null ? requestTopK
                : (datasetTopK != null ? datasetTopK : RetrievalConfig.defaults().topK());
        return Math.max(1, value);
    }

    private static double resolveMinSimilarity(Double requestMinSimilarity, Double datasetMinSimilarity) {
        double value = requestMinSimilarity != null
                ? requestMinSimilarity
                : (datasetMinSimilarity != null ? datasetMinSimilarity : RetrievalConfig.defaults().minSimilarity());
        return Math.max(0.0, Math.min(1.0, value));
    }

    private static String normalizeMatchField(String matchField) {
        String normalized = matchField == null ? DEFAULT_MATCH_FIELD : matchField.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            return DEFAULT_MATCH_FIELD;
        }
        if (normalized.startsWith("metadata:")) {
            return normalized;
        }
        return switch (normalized) {
            case "documentid", "document_id", "document" -> "documentid";
            case "chunkid", "chunk_id", "chunk" -> "chunkid";
            case "source" -> "source";
            default -> "documentid";
        };
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static double percentile95(List<Long> latenciesMs) {
        if (latenciesMs == null || latenciesMs.isEmpty()) {
            return 0.0;
        }
        List<Long> sorted = new ArrayList<>(latenciesMs);
        sorted.sort(Long::compareTo);
        int index = (int) Math.ceil(0.95d * sorted.size()) - 1;
        int bounded = Math.max(0, Math.min(index, sorted.size() - 1));
        return sorted.get(bounded);
    }
}
