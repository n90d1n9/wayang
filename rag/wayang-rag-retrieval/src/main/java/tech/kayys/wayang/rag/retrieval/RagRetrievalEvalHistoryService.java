package tech.kayys.wayang.rag.retrieval;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.rag.runtime.RagRuntimeConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@ApplicationScoped
public class RagRetrievalEvalHistoryService {

    private final RagRuntimeConfig config;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final List<RagRetrievalEvalRun> runs = new ArrayList<>();

    private volatile boolean initialized;
    private volatile Path persistencePath;

    @Inject
    public RagRetrievalEvalHistoryService(RagRuntimeConfig config, ObjectMapper objectMapper) {
        this(config, objectMapper, Clock.systemUTC());
    }

    RagRetrievalEvalHistoryService(RagRuntimeConfig config, ObjectMapper objectMapper, Clock clock) {
        this.config = Objects.requireNonNull(config, "config");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public synchronized RagRetrievalEvalRun append(RagRetrievalEvalResponse response) {
        initializeIfRequired();
        RagRetrievalEvalRun run = new RagRetrievalEvalRun(
                UUID.randomUUID().toString(),
                response.datasetName(),
                response.tenantId(),
                response.topK(),
                response.minSimilarity(),
                response.matchField(),
                response.queryCount(),
                response.hitCount(),
                response.recallAtK(),
                response.mrr(),
                response.latencyP95Ms(),
                response.latencyAvgMs(),
                response.evaluatedAt() == null ? clock.instant() : response.evaluatedAt());

        runs.add(run);
        enforceRetention();
        if (persistencePath != null) {
            try {
                rewritePersistenceFile();
            } catch (IOException e) {
                throw new IllegalStateException("Failed to persist retrieval eval history", e);
            }
        }
        return run;
    }

    public synchronized List<RagRetrievalEvalRun> history(String tenantId, String datasetName, int limit) {
        initializeIfRequired();
        int boundedLimit = limit <= 0 ? 20 : Math.min(limit, 500);
        List<RagRetrievalEvalRun> filtered = filteredRuns(tenantId, datasetName);
        int from = Math.max(0, filtered.size() - boundedLimit);
        return List.copyOf(filtered.subList(from, filtered.size()));
    }

    public synchronized RagRetrievalEvalTrendResponse trend(String tenantId, String datasetName, int windowSize) {
        initializeIfRequired();
        int boundedWindow = windowSize <= 0 ? 20 : Math.min(windowSize, 200);
        List<RagRetrievalEvalRun> filtered = filteredRuns(tenantId, datasetName);
        int from = Math.max(0, filtered.size() - boundedWindow);
        List<RagRetrievalEvalRun> window = List.copyOf(filtered.subList(from, filtered.size()));

        RagRetrievalEvalRun latest = window.isEmpty() ? null : window.get(window.size() - 1);
        RagRetrievalEvalRun previous = window.size() < 2 ? null : window.get(window.size() - 2);

        return new RagRetrievalEvalTrendResponse(
                emptyToNull(tenantId),
                emptyToNull(datasetName),
                boundedWindow,
                window.size(),
                latest,
                previous,
                delta(previous == null ? null : previous.recallAtK(), latest == null ? null : latest.recallAtK()),
                delta(previous == null ? null : previous.mrr(), latest == null ? null : latest.mrr()),
                delta(previous == null ? null : previous.latencyP95Ms(), latest == null ? null : latest.latencyP95Ms()),
                delta(previous == null ? null : previous.latencyAvgMs(), latest == null ? null : latest.latencyAvgMs()),
                window,
                clock.instant());
    }

    private List<RagRetrievalEvalRun> filteredRuns(String tenantId, String datasetName) {
        return runs.stream()
                .filter(run -> matches(run.tenantId(), tenantId))
                .filter(run -> matches(run.datasetName(), datasetName))
                .sorted(Comparator.comparing(RagRetrievalEvalRun::evaluatedAt))
                .toList();
    }

    private boolean matches(String value, String filter) {
        if (filter == null || filter.isBlank()) {
            return true;
        }
        return Objects.equals(value, filter.trim());
    }

    private void enforceRetention() {
        int limit = Math.max(1, config.getRetrievalEvalHistoryMaxEvents());
        if (runs.size() <= limit) {
            return;
        }
        int removeCount = runs.size() - limit;
        runs.subList(0, removeCount).clear();
    }

    private void initializeIfRequired() {
        if (initialized) {
            return;
        }
        synchronized (this) {
            if (initialized) {
                return;
            }
            String configuredPath = config.getRetrievalEvalHistoryPath();
            if (configuredPath != null && !configuredPath.isBlank()) {
                persistencePath = Path.of(configuredPath.trim());
                loadPersistedHistory();
                enforceRetention();
            }
            initialized = true;
        }
    }

    private void loadPersistedHistory() {
        if (persistencePath == null || !Files.exists(persistencePath)) {
            return;
        }
        try {
            for (String line : Files.readAllLines(persistencePath)) {
                String payload = line == null ? "" : line.trim();
                if (payload.isEmpty()) {
                    continue;
                }
                RagRetrievalEvalRun run = objectMapper.readValue(payload, RagRetrievalEvalRun.class);
                runs.add(run);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load retrieval eval history", e);
        }
    }

    private void rewritePersistenceFile() throws IOException {
        Path parent = persistencePath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        StringBuilder content = new StringBuilder();
        for (RagRetrievalEvalRun run : runs) {
            content.append(toJson(run)).append(System.lineSeparator());
        }
        Files.writeString(
                persistencePath,
                content.toString(),
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING);
    }

    private String toJson(RagRetrievalEvalRun run) {
        try {
            return objectMapper.writeValueAsString(run);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize retrieval eval run", e);
        }
    }

    private static String emptyToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static Double delta(Double previous, Double latest) {
        if (previous == null || latest == null) {
            return null;
        }
        return latest - previous;
    }
}
