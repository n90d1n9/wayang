package tech.kayys.gamelan.memory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class VectorStoreTest {

    private VectorStore store;

    @BeforeEach
    void setUp(@TempDir Path tmp) {
        store = new VectorStore(tmp.resolve("test.vec"), "test");
    }

    @Test
    void upsertAndRetrieve() {
        float[] vec = {1f, 0f, 0f};
        store.upsert("id1", "hello world", vec, Map.of("type", "FACT"));
        assertThat(store.size()).isEqualTo(1);
        assertThat(store.get("id1")).isPresent();
        assertThat(store.get("id1").get().text()).isEqualTo("hello world");
    }

    @Test
    void upsertOverwritesExistingId() {
        float[] v1 = {1f, 0f};
        float[] v2 = {0f, 1f};
        store.upsert("same-id", "original", v1, Map.of());
        store.upsert("same-id", "updated",  v2, Map.of());
        assertThat(store.size()).isEqualTo(1);
        assertThat(store.get("same-id").get().text()).isEqualTo("updated");
    }

    @Test
    void deleteRemovesEntry() {
        store.upsert("del", "text", new float[]{1f, 0f}, Map.of());
        assertThat(store.delete("del")).isTrue();
        assertThat(store.get("del")).isEmpty();
        assertThat(store.delete("del")).isFalse(); // idempotent
    }

    @Test
    void searchReturnsMostSimilar() {
        store.upsert("a", "a", new float[]{1f, 0f, 0f}, Map.of());
        store.upsert("b", "b", new float[]{0f, 1f, 0f}, Map.of());
        store.upsert("c", "c", new float[]{0f, 0f, 1f}, Map.of());

        // Query aligned with "a"
        List<VectorStore.SearchResult> results =
                store.search(new float[]{1f, 0f, 0f}, 3, 0.0f);
        assertThat(results).isNotEmpty();
        assertThat(results.get(0).id()).isEqualTo("a");
    }

    @Test
    void searchRespectsMinScore() {
        store.upsert("x", "x", new float[]{1f, 0f}, Map.of());
        // Orthogonal query → cosine = 0
        List<VectorStore.SearchResult> results =
                store.search(new float[]{0f, 1f}, 5, 0.5f);
        assertThat(results).isEmpty();
    }

    @Test
    void searchFiltersOnMetadata() {
        store.upsert("fact-1",  "fact one",   new float[]{1f, 0f}, Map.of("type", "FACT"));
        store.upsert("proc-1",  "procedure",  new float[]{1f, 0f}, Map.of("type", "PROCEDURE"));

        List<VectorStore.SearchResult> results =
                store.search(new float[]{1f, 0f}, 5, 0.0f, Map.of("type", "FACT"));
        assertThat(results).hasSize(1);
        assertThat(results.get(0).id()).isEqualTo("fact-1");
    }

    @Test
    void clearRemovesAll() {
        store.upsert("a", "a", new float[]{1f}, Map.of());
        store.upsert("b", "b", new float[]{1f}, Map.of());
        store.clear();
        assertThat(store.size()).isEqualTo(0);
    }

    @Test
    void filterByMetaWorks() {
        store.upsert("p1", "p1", new float[]{1f}, Map.of("project", "proj-a"));
        store.upsert("p2", "p2", new float[]{1f}, Map.of("project", "proj-b"));

        assertThat(store.filterByMeta("project", "proj-a")).hasSize(1);
        assertThat(store.filterByMeta("project", "proj-a").get(0).id()).isEqualTo("p1");
    }

    @Test
    void emptyQueryVectorReturnsEmptyResults() {
        store.upsert("x", "x", new float[]{1f, 0f}, Map.of());
        assertThat(store.search(new float[0], 5, 0f)).isEmpty();
    }

    @Test
    void cosineSimilarityCorrect() {
        float[] a = {1f, 0f, 0f};
        float[] b = {1f, 0f, 0f};
        assertThat(VectorStore.cosineSimilarity(a, b)).isCloseTo(1.0f, org.assertj.core.data.Offset.offset(0.001f));

        float[] c = {0f, 1f, 0f};
        assertThat(VectorStore.cosineSimilarity(a, c)).isCloseTo(0.0f, org.assertj.core.data.Offset.offset(0.001f));

        float[] d = {1f, 1f, 0f};  // 45 degrees from a
        assertThat(VectorStore.cosineSimilarity(a, d)).isCloseTo(0.7071f, org.assertj.core.data.Offset.offset(0.001f));
    }

    @Test
    void cosineSimilarityHandlesZeroVector() {
        assertThat(VectorStore.cosineSimilarity(new float[]{0f, 0f}, new float[]{1f, 0f}))
                .isEqualTo(0f);
    }
}
