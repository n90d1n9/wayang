package tech.kayys.wayang.agent.core.skills.adapters;

import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import tech.kayys.wayang.agent.spi.skills.SkillContext;
import tech.kayys.wayang.agent.spi.skills.SkillMetadata;
import tech.kayys.wayang.agent.spi.skills.SkillResult;
import tech.kayys.wayang.agent.spi.skills.SkillResult.Status;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SkillMemoryProvider Tests")
class SkillMemoryProviderTest {

    private SkillContext mockContext;
    private SkillMemoryProvider provider;

    @BeforeEach
    void setUp() {
        SkillMetadata mockMetadata = new SkillMetadata(
            "test-skill",
            "Test Skill",
            "Test skill for memory provider",
            "1.0.0",
            "test",
            List.of("test"),
            null
        );

        mockContext = new SkillContext() {
            @Override
            public String skillId() {
                return "test-skill";
            }

            @Override
            public String userId() {
                return "test-user";
            }

            @Override
            public SkillMetadata metadata() {
                return mockMetadata;
            }

            @Override
            public Map<String, Object> variables() {
                return Map.of();
            }

            @Override
            public long timeoutMs() {
                return 5000;
            }
        };

        provider = new SkillMemoryProvider(mockContext);
    }

    @Test
    @DisplayName("Should store and retrieve context")
    void testStoreAndRetrieveContext() {
        provider.storeContext("key1", "value1").await().indefinitely();
        
        Optional<String> retrieved = provider.getContext("key1", String.class);
        
        assertTrue(retrieved.isPresent());
        assertEquals("value1", retrieved.get());
    }

    @Test
    @DisplayName("Should return empty for non-existent key")
    void testGetNonExistentKey() {
        Optional<String> retrieved = provider.getContext("nonexistent", String.class);
        
        assertTrue(retrieved.isEmpty());
    }

    @Test
    @DisplayName("Should store and retrieve skill result")
    void testStoreAndRetrieveResult() {
        SkillResult result = new SkillResult(
            "test-skill",
            "invoc-123",
            Status.SUCCESS,
            "Test result",
            true
        );

        provider.storeResult(result).await().indefinitely();
        
        Optional<SkillResult> retrieved = provider.getLastResult();
        
        assertTrue(retrieved.isPresent());
        assertEquals("Test result", retrieved.get().observation());
        assertTrue(retrieved.get().success());
    }

    @Test
    @DisplayName("Should store metrics")
    void testStoreMetrics() {
        Map<String, Object> metrics = Map.of(
            "executionTime", 1000L,
            "tokenCount", 150,
            "costEstimate", 0.05
        );

        provider.storeMetrics(metrics).await().indefinitely();
        
        var allContext = provider.getAllContext();
        assertTrue(allContext.containsKey("metrics_test-skill"));
    }

    @Test
    @DisplayName("Should get all context")
    void testGetAllContext() {
        provider.storeContext("key1", "value1").await().indefinitely();
        provider.storeContext("key2", 42).await().indefinitely();
        
        Map<String, Object> allContext = provider.getAllContext();
        
        assertEquals(2, allContext.size());
        assertTrue(allContext.containsKey("key1"));
        assertTrue(allContext.containsKey("key2"));
    }

    @Test
    @DisplayName("Should clear memory")
    void testClearMemory() {
        provider.storeContext("key1", "value1").await().indefinitely();
        provider.storeContext("key2", "value2").await().indefinitely();
        
        Map<String, Object> beforeClear = provider.getAllContext();
        assertEquals(2, beforeClear.size());
        
        provider.clearMemory().await().indefinitely();
        
        Map<String, Object> afterClear = provider.getAllContext();
        assertTrue(afterClear.isEmpty());
    }

    @Test
    @DisplayName("Should store context asynchronously")
    void testStoreContextAsync() {
        Uni<Void> store = provider.storeContext("async-key", "async-value");
        
        assertNotNull(store);
        store.await().indefinitely();
        
        Optional<String> retrieved = provider.getContext("async-key", String.class);
        assertTrue(retrieved.isPresent());
    }

    @Test
    @DisplayName("Should support multiple types")
    void testMultipleTypes() {
        provider.storeContext("stringKey", "stringValue").await().indefinitely();
        provider.storeContext("intKey", 42).await().indefinitely();
        provider.storeContext("boolKey", true).await().indefinitely();
        
        Optional<String> stringVal = provider.getContext("stringKey", String.class);
        Optional<Integer> intVal = provider.getContext("intKey", Integer.class);
        Optional<Boolean> boolVal = provider.getContext("boolKey", Boolean.class);
        
        assertTrue(stringVal.isPresent());
        assertTrue(intVal.isPresent());
        assertTrue(boolVal.isPresent());
        
        assertEquals("stringValue", stringVal.get());
        assertEquals(42, intVal.get());
        assertEquals(true, boolVal.get());
    }
}
