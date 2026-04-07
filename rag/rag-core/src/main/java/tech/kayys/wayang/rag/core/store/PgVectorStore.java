package tech.kayys.wayang.rag.core.store;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class PgVectorStore<T> implements VectorStore<T> {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final DataSource dataSource;
    private final PayloadCodec<T> payloadCodec;
    private final ObjectMapper objectMapper;
    private final String tableName;
    private final int dimensions;

    public PgVectorStore(
            DataSource dataSource,
            PayloadCodec<T> payloadCodec,
            ObjectMapper objectMapper,
            String tableName,
            int dimensions) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
        this.payloadCodec = Objects.requireNonNull(payloadCodec, "payloadCodec must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        this.tableName = validateTableName(tableName);
        if (dimensions <= 0) {
            throw new IllegalArgumentException("dimensions must be > 0");
        }
        this.dimensions = dimensions;
    }

    public void initialize(boolean ensureExtension, boolean createIvfFlatIndex) {
        try (Connection connection = dataSource.getConnection()) {
            if (ensureExtension) {
                try (PreparedStatement statement = connection.prepareStatement("CREATE EXTENSION IF NOT EXISTS vector")) {
                    statement.execute();
                }
            }

            String createTableSql = """
                    CREATE TABLE IF NOT EXISTS %s (
                        namespace TEXT NOT NULL,
                        id TEXT NOT NULL,
                        embedding vector(%d) NOT NULL,
                        payload JSONB NOT NULL,
                        metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
                        PRIMARY KEY (namespace, id)
                    )
                    """.formatted(tableName, dimensions);
            try (PreparedStatement statement = connection.prepareStatement(createTableSql)) {
                statement.execute();
            }

            if (createIvfFlatIndex) {
                String createIndexSql = """
                        CREATE INDEX IF NOT EXISTS %s_embedding_ivfflat_cosine
                        ON %s USING ivfflat (embedding vector_cosine_ops)
                        """.formatted(tableName, tableName);
                try (PreparedStatement statement = connection.prepareStatement(createIndexSql)) {
                    statement.execute();
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to initialize pgvector store", e);
        }
    }

    @Override
    public void upsert(String namespace, String id, float[] vector, T payload, Map<String, Object> metadata) {
        Objects.requireNonNull(namespace, "namespace must not be null");
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(vector, "vector must not be null");
        Objects.requireNonNull(payload, "payload must not be null");
        if (vector.length != dimensions) {
            throw new IllegalArgumentException(
                    "Vector dimension mismatch: expected " + dimensions + " but got " + vector.length);
        }

        String sql = """
                INSERT INTO %s(namespace, id, embedding, payload, metadata)
                VALUES (?, ?, CAST(? AS vector), CAST(? AS jsonb), CAST(? AS jsonb))
                ON CONFLICT(namespace, id)
                DO UPDATE SET
                    embedding = EXCLUDED.embedding,
                    payload = EXCLUDED.payload,
                    metadata = EXCLUDED.metadata
                """.formatted(tableName);

        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, namespace);
            statement.setString(2, id);
            statement.setString(3, toVectorLiteral(vector));
            statement.setString(4, payloadCodec.serialize(payload));
            statement.setString(5, toJson(metadata == null ? Map.of() : metadata));
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to upsert vector entry", e);
        }
    }

    @Override
    public List<VectorSearchHit<T>> search(
            String namespace,
            float[] queryVector,
            int topK,
            double minScore,
            Map<String, Object> filters) {

        Objects.requireNonNull(namespace, "namespace must not be null");
        Objects.requireNonNull(queryVector, "queryVector must not be null");
        if (queryVector.length != dimensions) {
            throw new IllegalArgumentException(
                    "Query vector dimension mismatch: expected " + dimensions + " but got " + queryVector.length);
        }
        if (topK <= 0) {
            return List.of();
        }

        StringBuilder sql = new StringBuilder("""
                SELECT id, payload, metadata, (1 - (embedding <=> CAST(? AS vector))) AS score
                FROM %s
                WHERE namespace = ?
                """.formatted(tableName));

        boolean hasFilters = filters != null && !filters.isEmpty();
        if (hasFilters) {
            sql.append(" AND metadata @> CAST(? AS jsonb)");
        }

        sql.append(" AND (1 - (embedding <=> CAST(? AS vector))) >= ?");
        sql.append(" ORDER BY embedding <=> CAST(? AS vector)");
        sql.append(" LIMIT ?");

        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql.toString())) {

            int parameterIndex = 1;
            statement.setString(parameterIndex++, toVectorLiteral(queryVector));
            statement.setString(parameterIndex++, namespace);
            if (hasFilters) {
                statement.setString(parameterIndex++, toJson(filters));
            }
            statement.setString(parameterIndex++, toVectorLiteral(queryVector));
            statement.setDouble(parameterIndex++, minScore);
            statement.setString(parameterIndex++, toVectorLiteral(queryVector));
            statement.setInt(parameterIndex, topK);

            List<VectorSearchHit<T>> hits = new ArrayList<>();
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    String id = resultSet.getString("id");
                    T payload = payloadCodec.deserialize(resultSet.getString("payload"));
                    Map<String, Object> metadata = fromJson(resultSet.getString("metadata"));
                    double score = resultSet.getDouble("score");
                    hits.add(new VectorSearchHit<>(id, payload, score, metadata));
                }
            }
            return hits;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to search vectors", e);
        }
    }

    @Override
    public boolean delete(String namespace, String id) {
        Objects.requireNonNull(namespace, "namespace must not be null");
        Objects.requireNonNull(id, "id must not be null");

        String sql = "DELETE FROM %s WHERE namespace = ? AND id = ?".formatted(tableName);
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, namespace);
            statement.setString(2, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to delete vector entry", e);
        }
    }

    @Override
    public void clear(String namespace) {
        Objects.requireNonNull(namespace, "namespace must not be null");

        String sql = "DELETE FROM %s WHERE namespace = ?".formatted(tableName);
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, namespace);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to clear namespace", e);
        }
    }

    static String toVectorLiteral(float[] vector) {
        if (vector == null || vector.length == 0) {
            throw new IllegalArgumentException("vector must not be null or empty");
        }

        StringBuilder builder = new StringBuilder(vector.length * 8 + 2);
        builder.append('[');
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(Float.toString(vector[i]));
        }
        builder.append(']');
        return builder.toString();
    }

    private static String validateTableName(String tableName) {
        if (tableName == null || tableName.isBlank()) {
            throw new IllegalArgumentException("tableName must not be blank");
        }
        if (!tableName.matches("[a-zA-Z0-9_]+")) {
            throw new IllegalArgumentException("tableName may only contain letters, digits, and underscore");
        }
        return tableName;
    }

    private String toJson(Map<String, Object> map) {
        try {
            return objectMapper.writeValueAsString(map == null ? Map.of() : map);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to encode metadata as json", e);
        }
    }

    private Map<String, Object> fromJson(String json) {
        try {
            if (json == null || json.isBlank()) {
                return Map.of();
            }
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to decode metadata json", e);
        }
    }
}
