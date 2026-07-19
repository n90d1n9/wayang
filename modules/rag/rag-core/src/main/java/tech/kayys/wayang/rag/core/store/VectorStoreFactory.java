package tech.kayys.wayang.rag.core.store;

import com.fasterxml.jackson.databind.ObjectMapper;

import javax.sql.DataSource;
import java.util.Locale;
import java.util.Objects;

public final class VectorStoreFactory {

    private VectorStoreFactory() {
    }

    public static <T> VectorStore<T> create(
            VectorStoreOptions options,
            DataSource dataSource,
            PayloadCodec<T> payloadCodec,
            ObjectMapper objectMapper) {

        Objects.requireNonNull(options, "options must not be null");
        String backend = normalizeBackend(options.backend());

        if ("in-memory".equals(backend)) {
            return new InMemoryVectorStore<>();
        }

        if ("faiss".equals(backend)) {
            return new FaissVectorStore<>(options.dimensions());
        }

        if ("pgvector".equals(backend)) {
            if (dataSource == null) {
                throw new IllegalArgumentException("dataSource is required for pgvector backend");
            }
            if (payloadCodec == null) {
                throw new IllegalArgumentException("payloadCodec is required for pgvector backend");
            }
            if (objectMapper == null) {
                throw new IllegalArgumentException("objectMapper is required for pgvector backend");
            }

            PgVectorStore<T> store = new PgVectorStore<>(
                    dataSource,
                    payloadCodec,
                    objectMapper,
                    options.tableName(),
                    options.dimensions());
            store.initialize(options.ensureExtension(), options.createIvfFlatIndex());
            return store;
        }

        throw new IllegalArgumentException("Unsupported vector backend: " + options.backend());
    }

    private static String normalizeBackend(String backend) {
        if (backend == null || backend.isBlank()) {
            return "in-memory";
        }

        String normalized = backend.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "memory", "inmemory", "in-memory" -> "in-memory";
            case "postgres", "postgresql", "pgvector" -> "pgvector";
            case "faiss" -> "faiss";
            default -> normalized;
        };
    }
}
