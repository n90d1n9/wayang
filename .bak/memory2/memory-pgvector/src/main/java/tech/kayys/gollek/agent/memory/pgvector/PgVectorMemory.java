package tech.kayys.gollek.agent.memory.pgvector;

import io.quarkus.arc.properties.IfBuildProperty;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.pgclient.PgPool;
import io.vertx.mutiny.sqlclient.Row;
import io.vertx.mutiny.sqlclient.RowSet;
import io.vertx.mutiny.sqlclient.Tuple;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import tech.kayys.gollek.agent.memory.AgentMemory;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * PostgreSQL + pgvector backed {@link AgentMemory} for production deployments.
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Working memory stored in Redis (delegate to RedisAgentMemory) or PG</li>
 *   <li>Conversation history in {@code conversation_history} table</li>
 *   <li>Long-term facts in {@code agent_facts} table with pgvector embeddings</li>
 *   <li>Cosine similarity search via {@code <=>} operator</li>
 *   <li>HNSW index for fast ANN search at scale</li>
 * </ul>
 *
 * <h2>Activation</h2>
 * <pre>
 * gollek.agent.memory.backend=pgvector
 * quarkus.datasource.reactive.url=postgresql://localhost:5432/gollek
 * gollek.agent.memory.vector-dimensions=1536
 * </pre>
 *
 * <h2>Required schema</h2>
 * The init.sql already creates the necessary tables. Ensure pgvector is installed:
 * <pre>
 * CREATE EXTENSION IF NOT EXISTS vector;
 * ALTER TABLE agent_facts ADD COLUMN IF NOT EXISTS embedding vector(1536);
 * CREATE INDEX IF NOT EXISTS agent_facts_embedding_hnsw
 *   ON agent_facts USING hnsw (embedding vector_cosine_ops)
 *   WITH (m = 16, ef_construction = 64);
 * </pre>
 */
@Alternative
@ApplicationScoped
@IfBuildProperty(name = "gollek.agent.memory.backend", stringValue = "pgvector")
public class PgVectorMemory implements AgentMemory {

    private static final Logger LOG = Logger.getLogger(PgVectorMemory.class);

    @Inject PgPool pgPool;

    @ConfigProperty(name = "gollek.agent.memory.vector-dimensions", defaultValue = "1536")
    int vectorDimensions;

    @ConfigProperty(name = "gollek.agent.memory.conversation.max-history", defaultValue = "50")
    int maxHistory;

    // ── Working memory (PG-backed, ephemeral; expire old rows via scheduled job) ────

    @Override
    public Uni<Void> put(String runId, String key, Object value) {
        String sql = """
                INSERT INTO agent_working_memory (run_id, key, value, created_at)
                VALUES ($1, $2, $3::jsonb, NOW())
                ON CONFLICT (run_id, key) DO UPDATE SET value = EXCLUDED.value
                """;
        String json = toJson(value);
        return pgPool.preparedQuery(sql)
                .execute(Tuple.of(runId, key, json))
                .replaceWithVoid()
                .onFailure().invoke(e -> LOG.warnf("Working memory put failed: %s", e.getMessage()));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Uni<Optional<T>> get(String runId, String key, Class<T> type) {
        String sql = "SELECT value FROM agent_working_memory WHERE run_id = $1 AND key = $2";
        return pgPool.preparedQuery(sql)
                .execute(Tuple.of(runId, key))
                .map(rows -> {
                    if (rows.rowCount() == 0) return Optional.<T>empty();
                    String json = rows.iterator().next().getString("value");
                    T val = fromJson(json, type);
                    return Optional.ofNullable(val);
                })
                .onFailure().recoverWithItem(Optional.empty());
    }

    @Override
    public Uni<Void> delete(String runId, String key) {
        return pgPool.preparedQuery("DELETE FROM agent_working_memory WHERE run_id=$1 AND key=$2")
                .execute(Tuple.of(runId, key))
                .replaceWithVoid()
                .onFailure().recoverWithItem((Void) null);
    }

    @Override
    public Uni<Map<String, Object>> snapshot(String runId) {
        return pgPool.preparedQuery("SELECT key, value FROM agent_working_memory WHERE run_id=$1")
                .execute(Tuple.of(runId))
                .map(rows -> {
                    Map<String, Object> result = new LinkedHashMap<>();
                    rows.forEach(row -> result.put(row.getString("key"),
                            fromJson(row.getString("value"), Object.class)));
                    return result;
                })
                .onFailure().recoverWithItem(Map.of());
    }

    @Override
    public Uni<Void> clearRun(String runId) {
        return pgPool.preparedQuery("DELETE FROM agent_working_memory WHERE run_id=$1")
                .execute(Tuple.of(runId))
                .replaceWithVoid()
                .onFailure().recoverWithItem((Void) null);
    }

    // ── Conversation history ───────────────────────────────────────────────────

    @Override
    public Uni<Void> appendMessage(String sessionId, String role, String content) {
        String sql = """
                INSERT INTO conversation_history (session_id, tenant_id, role, content, created_at)
                VALUES ($1, 'community', $2, $3, NOW())
                """;
        return pgPool.preparedQuery(sql)
                .execute(Tuple.of(sessionId, role, content))
                .chain(() -> trimHistory(sessionId))
                .onFailure().invoke(e -> LOG.warnf("appendMessage failed: %s", e.getMessage()));
    }

    @Override
    public Uni<List<ConversationMessage>> getHistory(String sessionId, int limit) {
        int lim = limit > 0 ? limit : maxHistory;
        String sql = """
                SELECT role, content, created_at
                FROM conversation_history
                WHERE session_id = $1
                ORDER BY created_at ASC
                LIMIT $2
                """;
        return pgPool.preparedQuery(sql)
                .execute(Tuple.of(sessionId, lim))
                .map(rows -> {
                    List<ConversationMessage> msgs = new ArrayList<>();
                    rows.forEach(row -> msgs.add(new ConversationMessage(
                            row.getString("role"),
                            row.getString("content"),
                            row.getLocalDateTime("created_at") != null
                                    ? row.getLocalDateTime("created_at").toInstant(java.time.ZoneOffset.UTC)
                                    : Instant.now())));
                    return msgs;
                })
                .onFailure().recoverWithItem(List.of());
    }

    @Override
    public Uni<Void> clearHistory(String sessionId) {
        return pgPool.preparedQuery("DELETE FROM conversation_history WHERE session_id=$1")
                .execute(Tuple.of(sessionId))
                .replaceWithVoid()
                .onFailure().recoverWithItem((Void) null);
    }

    private Uni<Void> trimHistory(String sessionId) {
        // Keep only the most recent maxHistory messages
        String sql = """
                DELETE FROM conversation_history
                WHERE session_id = $1
                  AND msg_id NOT IN (
                      SELECT msg_id FROM conversation_history
                      WHERE session_id = $1
                      ORDER BY created_at DESC
                      LIMIT $2
                  )
                """;
        return pgPool.preparedQuery(sql)
                .execute(Tuple.of(sessionId, maxHistory))
                .replaceWithVoid()
                .onFailure().recoverWithItem((Void) null);
    }

    // ── Long-term / semantic memory (pgvector) ─────────────────────────────────

    @Override
    public Uni<Void> storeFact(String tenantId, String key, String text, float[] vector, Map<String, Object> metadata) {
        String metaJson = metadata != null ? toJson(metadata) : "{}";
        String vectorStr = vectorToString(vector);

        if (vectorStr != null) {
            // With embedding vector — full semantic storage
            String sql = """
                    INSERT INTO agent_facts
                        (fact_id, tenant_id, content, embedding, metadata, created_at)
                    VALUES ($1::uuid, $2, $3, $4::vector, $5::jsonb, NOW())
                    ON CONFLICT (fact_id) DO UPDATE
                        SET content = EXCLUDED.content,
                            embedding = EXCLUDED.embedding,
                            metadata = EXCLUDED.metadata
                    """;
            String factId = toUuid(tenantId, key);
            return pgPool.preparedQuery(sql)
                    .execute(Tuple.of(factId, tenantId, text, vectorStr, metaJson))
                    .replaceWithVoid()
                    .onFailure().invoke(e -> LOG.warnf("storeFact (with vector) failed: %s", e.getMessage()));
        } else {
            // Without embedding — plain text storage, no vector search
            String sql = """
                    INSERT INTO agent_facts
                        (fact_id, tenant_id, content, metadata, created_at)
                    VALUES ($1::uuid, $2, $3, $4::jsonb, NOW())
                    ON CONFLICT (fact_id) DO UPDATE
                        SET content = EXCLUDED.content,
                            metadata = EXCLUDED.metadata
                    """;
            String factId = toUuid(tenantId, key);
            return pgPool.preparedQuery(sql)
                    .execute(Tuple.of(factId, tenantId, text, metaJson))
                    .replaceWithVoid()
                    .onFailure().invoke(e -> LOG.warnf("storeFact (no vector) failed: %s", e.getMessage()));
        }
    }

    @Override
    public Uni<List<MemoryFact>> searchFacts(String tenantId, float[] queryVector, int topK) {
        if (queryVector == null || queryVector.length == 0) {
            // Fallback: return most recent facts when no query vector provided
            return recentFacts(tenantId, topK);
        }

        String vectorStr = vectorToString(queryVector);
        // Cosine similarity: 1 - (embedding <=> query_vector) gives similarity in [0,1]
        String sql = """
                SELECT fact_id::text AS id, content, metadata,
                       1 - (embedding <=> $3::vector) AS similarity
                FROM agent_facts
                WHERE tenant_id = $1
                  AND embedding IS NOT NULL
                ORDER BY embedding <=> $3::vector
                LIMIT $2
                """;
        return pgPool.preparedQuery(sql)
                .execute(Tuple.of(tenantId, topK, vectorStr))
                .map(this::rowsToFacts)
                .onFailure().recoverWithItem(e -> {
                    LOG.warnf("pgvector search failed: %s — falling back to recency", e.getMessage());
                    return List.of();
                })
                .chain(facts -> facts.isEmpty() ? recentFacts(tenantId, topK) : Uni.createFrom().item(facts));
    }

    private Uni<List<MemoryFact>> recentFacts(String tenantId, int topK) {
        String sql = """
                SELECT fact_id::text AS id, content, metadata, 1.0 AS similarity
                FROM agent_facts
                WHERE tenant_id = $1
                ORDER BY created_at DESC
                LIMIT $2
                """;
        return pgPool.preparedQuery(sql)
                .execute(Tuple.of(tenantId, topK))
                .map(this::rowsToFacts)
                .onFailure().recoverWithItem(List.of());
    }

    private List<MemoryFact> rowsToFacts(RowSet<Row> rows) {
        List<MemoryFact> facts = new ArrayList<>();
        rows.forEach(row -> {
            @SuppressWarnings("unchecked")
            Map<String, Object> meta = fromJson(row.getString("metadata"), Map.class);
            facts.add(new MemoryFact(
                    row.getString("id"),
                    row.getString("content"),
                    row.getFloat("similarity"),
                    meta != null ? meta : Map.of()));
        });
        return facts;
    }

    @Override
    public Uni<Void> deleteFact(String tenantId, String key) {
        String factId = toUuid(tenantId, key);
        return pgPool.preparedQuery("DELETE FROM agent_facts WHERE fact_id=$1::uuid AND tenant_id=$2")
                .execute(Tuple.of(factId, tenantId))
                .replaceWithVoid()
                .onFailure().recoverWithItem((Void) null);
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    /** Convert a float[] to PostgreSQL vector literal: "[0.1,0.2,...]" */
    private String vectorToString(float[] v) {
        if (v == null || v.length == 0) return null;
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < v.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(v[i]);
        }
        return sb.append(']').toString();
    }

    /** Derive a stable UUID from tenant+key using UUID5 (SHA-1 namespace). */
    private String toUuid(String tenantId, String key) {
        return UUID.nameUUIDFromBytes((tenantId + ":" + key).getBytes()).toString();
    }

    private String toJson(Object obj) {
        try {
            return JSON_MAPPER.writeValueAsString(obj);
        } catch (Exception e) { return "null"; }
    }

    @SuppressWarnings("unchecked")
    private <T> T fromJson(String json, Class<T> type) {
        if (json == null || json.isBlank()) return null;
        try { return JSON_MAPPER.readValue(json, type); } catch (Exception e) { return null; }
    }

    private static final com.fasterxml.jackson.databind.ObjectMapper JSON_MAPPER =
            new com.fasterxml.jackson.databind.ObjectMapper();
}
