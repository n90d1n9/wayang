package tech.kayys.gamelan.memory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SemanticMemoryStoreTest {

    @Mock EmbeddingService embeddings;
    private SemanticMemoryStore storeInstance;

    @BeforeEach
    void setUp(@TempDir Path tmp) throws Exception {
        storeInstance = new SemanticMemoryStore();
        injectField(storeInstance, "embeddings", embeddings);
        // Inject a temp VectorStore
        VectorStore vs = new VectorStore(tmp.resolve("test.vec"), "test");
        injectField(storeInstance, "store", vs);
    }

    @Test
    void storeAndSearchWithVectors() {
        when(embeddings.isAvailable()).thenReturn(true);
        // Return aligned vectors so "java" query finds "java-version" fact
        when(embeddings.embed("java-version. Java 21")).thenReturn(new float[]{1f, 0f, 0f});
        when(embeddings.embed("java")).thenReturn(new float[]{1f, 0f, 0f});

        storeInstance.store("java-version", "Java 21", "FACT", "my-project");

        List<SemanticMemoryStore.SemanticFact> results =
                storeInstance.search("java", 5, "my-project");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).topic()).isEqualTo("java-version");
        assertThat(results.get(0).fact()).isEqualTo("Java 21");
        assertThat(results.get(0).type()).isEqualTo("FACT");
    }

    @Test
    void fallsBackToKeywordSearchWhenEmbeddingUnavailable() {
        when(embeddings.isAvailable()).thenReturn(false);
        when(embeddings.embed(anyString())).thenReturn(new float[0]);

        storeInstance.store("flyway", "Uses Flyway for DB migrations", "FACT", "proj");

        // Keyword search should find "flyway" by token overlap
        List<SemanticMemoryStore.SemanticFact> results =
                storeInstance.search("flyway migration", 5, "proj");

        assertThat(results).isNotEmpty();
        assertThat(results.get(0).fact()).contains("Flyway");
    }

    @Test
    void storeOverwritesSameTopic() {
        when(embeddings.embed(anyString())).thenReturn(new float[]{1f, 0f});
        when(embeddings.isAvailable()).thenReturn(true);

        storeInstance.store("db", "MySQL 8", "FACT", "p");
        storeInstance.store("db", "PostgreSQL 15", "FACT", "p");

        assertThat(storeInstance.size()).isEqualTo(1);
        when(embeddings.embed("db")).thenReturn(new float[]{1f, 0f});
        var results = storeInstance.search("db", 5, "p");
        assertThat(results.get(0).fact()).isEqualTo("PostgreSQL 15");
    }

    @Test
    void projectIsolationWorks() {
        when(embeddings.embed(anyString())).thenReturn(new float[]{1f, 0f});
        when(embeddings.isAvailable()).thenReturn(true);

        storeInstance.store("key", "project-a fact", "FACT", "project-a");
        storeInstance.store("key", "project-b fact", "FACT", "project-b");

        when(embeddings.embed("key")).thenReturn(new float[]{1f, 0f});
        var results = storeInstance.search("key", 10, "project-a");
        // Should include project-a result; project-b should be filtered out
        assertThat(results).anyMatch(f -> f.fact().contains("project-a"));
        assertThat(results).noneMatch(f -> f.fact().contains("project-b"));
    }

    @Test
    void globalFactsIncludedForAllProjects() {
        when(embeddings.embed(anyString())).thenReturn(new float[]{1f, 0f});
        when(embeddings.isAvailable()).thenReturn(true);

        storeInstance.store("global-fact", "applies everywhere", "PREFERENCE", "_global");
        storeInstance.store("local-fact",  "local only",         "FACT",       "proj-x");

        when(embeddings.embed("fact")).thenReturn(new float[]{1f, 0f});
        var results = storeInstance.search("fact", 10, "proj-y"); // different project
        // Global facts should be visible to any project
        assertThat(results).anyMatch(f -> f.fact().contains("applies everywhere"));
    }

    @Test
    void deleteRemovesFact() {
        when(embeddings.embed(anyString())).thenReturn(new float[]{1f, 0f});
        storeInstance.store("to-delete", "remove me", "FACT", "p");
        assertThat(storeInstance.size()).isEqualTo(1);
        storeInstance.delete("to-delete");
        assertThat(storeInstance.size()).isEqualTo(0);
    }

    @Test
    void allReturnsFacts() {
        when(embeddings.embed(anyString())).thenReturn(new float[]{1f, 0f});
        storeInstance.store("fact-a", "value a", "FACT", "p");
        storeInstance.store("fact-b", "value b", "FACT", "p");
        assertThat(storeInstance.all()).hasSize(2);
    }

    @Test
    void emptyQueryReturnsEmpty() {
        assertThat(storeInstance.search("", 5, "p")).isEmpty();
        assertThat(storeInstance.search(null, 5, "p")).isEmpty();
    }

    private void injectField(Object target, String name, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }
}
