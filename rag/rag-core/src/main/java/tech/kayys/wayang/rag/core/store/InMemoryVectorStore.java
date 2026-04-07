package tech.kayys.wayang.rag.core.store;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryVectorStore<T> implements VectorStore<T> {

    private final Map<String, Map<String, Entry<T>>> namespaces = new ConcurrentHashMap<>();
    private final Map<String, Integer> namespaceDimensions = new ConcurrentHashMap<>();
    private final Map<String, NamespaceContract> namespaceContracts = new ConcurrentHashMap<>();

    @Override
    public void upsert(String namespace, String id, float[] vector, T payload, Map<String, Object> metadata) {
        Objects.requireNonNull(namespace, "namespace must not be null");
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(vector, "vector must not be null");
        Objects.requireNonNull(payload, "payload must not be null");
        if (vector.length == 0) {
            throw new IllegalArgumentException("vector must not be empty");
        }
        Integer expectedDimension = namespaceDimensions.computeIfAbsent(namespace, key -> vector.length);
        if (expectedDimension != vector.length) {
            throw new IllegalArgumentException(
                    "Vector dimension mismatch for namespace '" + namespace
                            + "': expected " + expectedDimension + " but got " + vector.length);
        }
        Map<String, Object> safeMetadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        validateUpsertContract(namespace, vector.length, safeMetadata);
        Map<String, Entry<T>> ns = namespaces.computeIfAbsent(namespace, key -> new ConcurrentHashMap<>());
        ns.put(id, new Entry<>(id, vector, payload, safeMetadata));
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
        if (topK <= 0) {
            return List.of();
        }
        Integer expectedDimension = namespaceDimensions.get(namespace);
        if (expectedDimension != null && queryVector.length != expectedDimension) {
            throw new IllegalArgumentException(
                    "Query vector dimension mismatch for namespace '" + namespace
                            + "': expected " + expectedDimension + " but got " + queryVector.length);
        }
        validateFilterContract(namespace, queryVector.length, filters == null ? Map.of() : filters);

        Map<String, Entry<T>> ns = namespaces.getOrDefault(namespace, Map.of());
        List<VectorSearchHit<T>> hits = new ArrayList<>(ns.size());
        for (Entry<T> entry : ns.values()) {
            if (!matchesFilters(entry.metadata, filters)) {
                continue;
            }
            double score = cosineSimilarity(queryVector, entry.vector);
            if (score < minScore) {
                continue;
            }
            hits.add(new VectorSearchHit<>(entry.id, entry.payload, score, entry.metadata));
        }

        hits.sort(Comparator.comparingDouble(VectorSearchHit<T>::score).reversed());
        return hits.size() <= topK ? hits : hits.subList(0, topK);
    }

    @Override
    public boolean delete(String namespace, String id) {
        Map<String, Entry<T>> ns = namespaces.get(namespace);
        if (ns == null) {
            return false;
        }
        return ns.remove(id) != null;
    }

    @Override
    public void clear(String namespace) {
        namespaces.remove(namespace);
        namespaceDimensions.remove(namespace);
        namespaceContracts.remove(namespace);
    }

    private void validateUpsertContract(String namespace, int vectorDimension, Map<String, Object> metadata) {
        Object tenantId = metadata.get("tenantId");
        if (tenantId != null && !Objects.equals(namespace, String.valueOf(tenantId))) {
            throw new IllegalArgumentException(
                    "Metadata tenantId mismatch for namespace '" + namespace + "': " + tenantId);
        }
        Object metadataDimension = metadata.get("embeddingDimension");
        if (metadataDimension != null && parseInt(metadataDimension, -1) != vectorDimension) {
            throw new IllegalArgumentException(
                    "Metadata embeddingDimension mismatch for namespace '" + namespace
                            + "': expected " + vectorDimension + " but got " + metadataDimension);
        }
        String metadataModel = asString(metadata.get("embeddingModel"));
        String metadataVersion = asString(metadata.get("embeddingVersion"));
        namespaceContracts.compute(namespace, (key, existing) -> {
            if (existing == null) {
                return new NamespaceContract(vectorDimension, metadataModel, metadataVersion);
            }
            existing.validate(vectorDimension, metadataModel, metadataVersion);
            return existing;
        });
    }

    private void validateFilterContract(String namespace, int queryDimension, Map<String, Object> filters) {
        if (filters == null || filters.isEmpty()) {
            return;
        }
        Object tenantId = filters.get("tenantId");
        if (tenantId != null && !Objects.equals(namespace, String.valueOf(tenantId))) {
            throw new IllegalArgumentException(
                    "Filter tenantId mismatch for namespace '" + namespace + "': " + tenantId);
        }
        Object filterDimension = filters.get("embeddingDimension");
        if (filterDimension != null && parseInt(filterDimension, -1) != queryDimension) {
            throw new IllegalArgumentException(
                    "Filter embeddingDimension mismatch for namespace '" + namespace
                            + "': expected " + queryDimension + " but got " + filterDimension);
        }
        NamespaceContract contract = namespaceContracts.get(namespace);
        if (contract == null) {
            return;
        }
        contract.validate(
                queryDimension,
                asString(filters.get("embeddingModel")),
                asString(filters.get("embeddingVersion")));
    }

    private static boolean matchesFilters(Map<String, Object> metadata, Map<String, Object> filters) {
        if (filters == null || filters.isEmpty()) {
            return true;
        }
        for (Map.Entry<String, Object> filter : filters.entrySet()) {
            Object candidate = metadata.get(filter.getKey());
            if (!Objects.equals(candidate, filter.getValue())) {
                return false;
            }
        }
        return true;
    }

    private static double cosineSimilarity(float[] a, float[] b) {
        if (a.length != b.length) {
            return -1.0;
        }
        double dot = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < a.length; i++) {
            dot += (double) a[i] * b[i];
            normA += (double) a[i] * a[i];
            normB += (double) b[i] * b[i];
        }
        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private static String asString(Object value) {
        if (value == null) {
            return null;
        }
        String s = String.valueOf(value).trim();
        return s.isEmpty() ? null : s;
    }

    private static int parseInt(Object value, int fallback) {
        if (value == null) {
            return fallback;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static final class NamespaceContract {
        private final int dimension;
        private String model;
        private String version;

        private NamespaceContract(int dimension, String model, String version) {
            this.dimension = dimension;
            this.model = model;
            this.version = version;
        }

        private synchronized void validate(int requestedDimension, String requestedModel, String requestedVersion) {
            if (requestedDimension != dimension) {
                throw new IllegalArgumentException(
                        "Embedding dimension contract mismatch: expected " + dimension + " but got " + requestedDimension);
            }
            if (requestedModel != null) {
                if (model == null) {
                    model = requestedModel;
                } else if (!Objects.equals(model, requestedModel)) {
                    throw new IllegalArgumentException(
                            "Embedding model contract mismatch: expected '" + model + "' but got '" + requestedModel + "'");
                }
            }
            if (requestedVersion != null) {
                if (version == null) {
                    version = requestedVersion;
                } else if (!Objects.equals(version, requestedVersion)) {
                    throw new IllegalArgumentException(
                            "Embedding version contract mismatch: expected '" + version + "' but got '" + requestedVersion
                                    + "'");
                }
            }
        }
    }

    private record Entry<T>(
            String id,
            float[] vector,
            T payload,
            Map<String, Object> metadata) {
    }
}
