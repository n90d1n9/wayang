package tech.kayys.wayang.rag.embedding;

import tech.kayys.wayang.rag.runtime.RagRuntimeConfig;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.embedding.EmbeddingModelSpec;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Administrative service for managing embedding schema versions and migrations.
 * Handles tenant-specific embedding contracts, maintains migration history with
 * persistence, and provides utilities for history compaction.
 */
@ApplicationScoped
public class EmbeddingSchemaAdminService {

    @Inject
    RagEmbeddingStoreFactory storeFactory;

    @Inject
    RagRuntimeConfig config;

    @Inject
    ObjectMapper objectMapper;

    private final ConcurrentMap<String, List<EmbeddingSchemaMigrationStatus>> history = new ConcurrentHashMap<>();
    private volatile boolean initialized;
    private volatile Path persistencePath;

    public EmbeddingSchemaContract status(String tenantId) {
        initializeIfRequired();
        String tenant = requireTenant(tenantId);
        return storeFactory.contractForTenant(tenant);
    }

    public EmbeddingSchemaMigrationStatus migrate(EmbeddingSchemaMigrationRequest request) {
        initializeIfRequired();
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }
        String tenant = requireTenant(request.tenantId());
        EmbeddingSchemaContract previous = storeFactory.contractForTenant(tenant);
        EmbeddingSchemaContract target = resolveTarget(previous, request);
        boolean changed = !previous.equals(target);
        boolean clearNamespace = request.clearNamespace() == null ? true : request.clearNamespace();
        boolean dryRun = request.dryRun() != null && request.dryRun();
        if (changed && !clearNamespace) {
            throw new IllegalArgumentException("clearNamespace must be true when embedding contract changes");
        }
        if (!dryRun) {
            storeFactory.migrateTenantContract(
                    tenant,
                    target.model(),
                    target.dimension(),
                    target.version(),
                    clearNamespace);
        }
        EmbeddingSchemaMigrationStatus status = new EmbeddingSchemaMigrationStatus(
                tenant,
                previous,
                target,
                changed,
                clearNamespace,
                dryRun,
                Instant.now());
        appendAndPersist(tenant, status);
        return status;
    }

    public List<EmbeddingSchemaMigrationStatus> history(String tenantId, int limit) {
        initializeIfRequired();
        String tenant = requireTenant(tenantId);
        int normalizedLimit = limit <= 0 ? 20 : Math.min(limit, 200);
        List<EmbeddingSchemaMigrationStatus> entries = history.getOrDefault(tenant, List.of());
        int from = Math.max(0, entries.size() - normalizedLimit);
        return List.copyOf(entries.subList(from, entries.size()));
    }

    public Set<String> tenantIdsWithHistory() {
        initializeIfRequired();
        return Set.copyOf(history.keySet());
    }

    public EmbeddingSchemaHistoryCompactionStatus compactHistory(
            String tenantId,
            EmbeddingSchemaHistoryCompactionRequest request) {
        initializeIfRequired();
        String tenant = requireTenant(tenantId);
        EmbeddingSchemaHistoryCompactionRequest safeRequest = request == null
                ? new EmbeddingSchemaHistoryCompactionRequest(null, null, false)
                : request;
        boolean dryRun = safeRequest.dryRun() != null && safeRequest.dryRun();

        List<EmbeddingSchemaMigrationStatus> before = history.getOrDefault(tenant, List.of());
        List<EmbeddingSchemaMigrationStatus> after = compactEntries(
                before,
                safeRequest.maxEvents(),
                safeRequest.maxAgeDays(),
                Instant.now());

        if (!dryRun) {
            if (after.isEmpty()) {
                history.remove(tenant);
            } else {
                history.put(tenant, List.copyOf(after));
            }
            if (persistencePath != null) {
                try {
                    rewritePersistenceFile();
                } catch (IOException e) {
                    throw new IllegalStateException("Failed to rewrite embedding schema migration history", e);
                }
            }
        }

        return new EmbeddingSchemaHistoryCompactionStatus(
                tenant,
                before.size(),
                after.size(),
                Math.max(0, before.size() - after.size()),
                dryRun,
                Instant.now());
    }

    private EmbeddingSchemaContract resolveTarget(
            EmbeddingSchemaContract previous,
            EmbeddingSchemaMigrationRequest request) {
        String model = normalizeModel(request.embeddingModel(), previous.model());
        int dimension = resolveDimension(request.embeddingDimension(), model, previous.dimension());
        String version = normalizeVersion(request.embeddingVersion(), previous.version());
        return new EmbeddingSchemaContract(model, dimension, version);
    }

    private int resolveDimension(Integer requestedDimension, String model, int fallback) {
        if (requestedDimension != null && requestedDimension > 0) {
            return requestedDimension;
        }
        OptionalInt parsed = EmbeddingModelSpec.parseDimension(model);
        if (parsed.isPresent()) {
            return parsed.getAsInt();
        }
        return fallback;
    }

    private String normalizeModel(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    private String normalizeVersion(String value, String fallback) {
        if (value != null && !value.isBlank()) {
            return value.trim();
        }
        if (fallback != null && !fallback.isBlank()) {
            return fallback.trim();
        }
        String configured = config.getEmbeddingVersion();
        if (configured != null && !configured.isBlank()) {
            return configured.trim();
        }
        return "v1";
    }

    private String requireTenant(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("tenantId must not be blank");
        }
        return tenantId.trim();
    }

    private List<EmbeddingSchemaMigrationStatus> appendHistory(
            List<EmbeddingSchemaMigrationStatus> existing,
            EmbeddingSchemaMigrationStatus status) {
        List<EmbeddingSchemaMigrationStatus> updated = existing == null
                ? new ArrayList<>()
                : new ArrayList<>(existing);
        updated.add(status);
        if (updated.size() > 500) {
            updated = new ArrayList<>(updated.subList(updated.size() - 500, updated.size()));
        }
        return List.copyOf(updated);
    }

    private void appendAndPersist(String tenant, EmbeddingSchemaMigrationStatus status) {
        history.compute(tenant, (key, entries) -> appendHistory(entries, status));
        if (persistencePath == null) {
            return;
        }
        try {
            persist(status);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to persist embedding schema migration history", e);
        }
    }

    private void initializeIfRequired() {
        if (initialized) {
            return;
        }
        synchronized (this) {
            if (initialized) {
                return;
            }
            String pathValue = config == null ? null : config.getEmbeddingSchemaHistoryPath();
            if (pathValue != null && !pathValue.isBlank()) {
                persistencePath = Path.of(pathValue.trim());
                loadPersistedHistory();
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
                EmbeddingSchemaMigrationStatus status = mapper().readValue(payload,
                        EmbeddingSchemaMigrationStatus.class);
                history.compute(status.tenantId(), (key, entries) -> appendHistory(entries, status));
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load persisted embedding schema migration history", e);
        }
    }

    private void persist(EmbeddingSchemaMigrationStatus status) throws IOException {
        Path parent = persistencePath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        String line = toJson(status) + System.lineSeparator();
        Files.writeString(
                persistencePath,
                line,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.APPEND);
    }

    private synchronized void rewritePersistenceFile() throws IOException {
        Path parent = persistencePath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        StringBuilder content = new StringBuilder();
        history.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> entry.getValue().forEach(status -> content
                        .append(toJson(status))
                        .append(System.lineSeparator())));
        Files.writeString(
                persistencePath,
                content.toString(),
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING);
    }

    private List<EmbeddingSchemaMigrationStatus> compactEntries(
            List<EmbeddingSchemaMigrationStatus> source,
            Integer maxEvents,
            Integer maxAgeDays,
            Instant now) {
        List<EmbeddingSchemaMigrationStatus> compacted = new ArrayList<>(source);
        if (maxAgeDays != null && maxAgeDays > 0) {
            Instant cutoff = now.minus(maxAgeDays, ChronoUnit.DAYS);
            compacted = compacted.stream()
                    .filter(entry -> entry.migratedAt() != null && !entry.migratedAt().isBefore(cutoff))
                    .toList();
        }
        if (maxEvents != null && maxEvents > 0 && compacted.size() > maxEvents) {
            compacted = compacted.subList(compacted.size() - maxEvents, compacted.size());
        }
        return List.copyOf(compacted);
    }

    private String toJson(EmbeddingSchemaMigrationStatus status) {
        try {
            return mapper().writeValueAsString(status);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize embedding schema migration history event", e);
        }
    }

    private ObjectMapper mapper() {
        if (objectMapper != null) {
            return objectMapper;
        }
        synchronized (this) {
            if (objectMapper == null) {
                objectMapper = new ObjectMapper().findAndRegisterModules();
            }
            return objectMapper;
        }
    }
}
