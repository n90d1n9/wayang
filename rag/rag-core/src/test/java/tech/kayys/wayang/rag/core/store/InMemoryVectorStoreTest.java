package tech.kayys.wayang.rag.core.store;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryVectorStoreTest {

        @Test
        void shouldReturnMostSimilarVectorWithFilter() {
                InMemoryVectorStore<String> store = new InMemoryVectorStore<>();
                store.upsert("tenant-a", "id-1", new float[] { 1f, 0f }, "doc-1", Map.of("collection", "finance"));
                store.upsert("tenant-a", "id-2", new float[] { 0f, 1f }, "doc-2", Map.of("collection", "engineering"));
                store.upsert("tenant-a", "id-3", new float[] { 0.8f, 0.2f }, "doc-3", Map.of("collection", "finance"));

                List<VectorSearchHit<String>> hits = store.search(
                                "tenant-a",
                                new float[] { 1f, 0f },
                                2,
                                0.1,
                                Map.of("collection", "finance"));

                assertEquals(2, hits.size());
                assertEquals("doc-1", hits.get(0).payload());
                assertTrue(hits.get(0).score() >= hits.get(1).score());
        }

        @Test
        void shouldDeleteEntry() {
                InMemoryVectorStore<String> store = new InMemoryVectorStore<>();
                store.upsert("tenant-a", "id-1", new float[] { 1f, 0f }, "doc-1", Map.of());

                boolean deleted = store.delete("tenant-a", "id-1");
                List<VectorSearchHit<String>> hits = store.search("tenant-a", new float[] { 1f, 0f }, 5, 0.0, Map.of());

                assertTrue(deleted);
                assertTrue(hits.isEmpty());
        }

        @Test
        void shouldRejectDimensionMismatchWithinNamespace() {
                InMemoryVectorStore<String> store = new InMemoryVectorStore<>();
                store.upsert("tenant-a", "id-1", new float[] { 1f, 0f }, "doc-1", Map.of());

                assertThrows(
                                IllegalArgumentException.class,
                                () -> store.upsert("tenant-a", "id-2", new float[] { 1f, 0f, 0f }, "doc-2", Map.of()));
                assertThrows(
                                IllegalArgumentException.class,
                                () -> store.search("tenant-a", new float[] { 1f, 0f, 0f }, 5, 0.0, Map.of()));
        }

        @Test
        void shouldRejectTenantModelAndVersionContractMismatch() {
                InMemoryVectorStore<String> store = new InMemoryVectorStore<>();
                store.upsert(
                                "tenant-a",
                                "id-1",
                                new float[] { 1f, 0f },
                                "doc-1",
                                Map.of("tenantId", "tenant-a", "embeddingModel", "hash-2", "embeddingDimension", 2,
                                                "embeddingVersion", "v1"));

                assertThrows(
                                IllegalArgumentException.class,
                                () -> store.upsert(
                                                "tenant-a",
                                                "id-2",
                                                new float[] { 1f, 0f },
                                                "doc-2",
                                                Map.of("tenantId", "tenant-b", "embeddingModel", "hash-2",
                                                                "embeddingDimension", 2)));
                assertThrows(
                                IllegalArgumentException.class,
                                () -> store.search(
                                                "tenant-a",
                                                new float[] { 1f, 0f },
                                                5,
                                                0.0,
                                                Map.of("embeddingModel", "tfidf-2")));
                assertThrows(
                                IllegalArgumentException.class,
                                () -> store.search(
                                                "tenant-a",
                                                new float[] { 1f, 0f },
                                                5,
                                                0.0,
                                                Map.of("embeddingVersion", "v2")));
        }
}
