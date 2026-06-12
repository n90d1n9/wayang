package tech.kayys.wayang.agent.core.skills.adapters;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.core.skills.support.TestSkillContexts;
import tech.kayys.wayang.agent.spi.skills.SkillContext;
import tech.kayys.wayang.agent.spi.skills.SkillResult;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkillMemoryProviderContractTest {

    @Test
    void storesAndReadsTypedContextValues() {
        SkillMemoryProvider provider = provider();

        provider.storeContext(" answer ", "forty-two").await().indefinitely();
        provider.storeContext(null, "ignored").await().indefinitely();
        provider.storeContext("ignored", null).await().indefinitely();

        assertEquals("forty-two", provider.getContext("answer", String.class).orElseThrow());
        assertTrue(provider.getContext("answer", Integer.class).isEmpty());
        assertTrue(provider.getContext("missing", String.class).isEmpty());
        assertTrue(provider.getContext("answer", null).isEmpty());
        assertFalse(provider.getAllContext().containsKey("ignored"));
    }

    @Test
    void storesLastResultUnderSkillScopedKeys() {
        SkillMemoryProvider provider = provider();
        SkillResult result = SkillResult.builder()
                .skillId("memory-skill")
                .status(SkillResult.Status.SUCCESS)
                .observation("remembered")
                .durationMs(25)
                .build();

        provider.storeResult(result).await().indefinitely();

        assertEquals(result, provider.getLastResult().orElseThrow());
        assertEquals(SkillResult.Status.SUCCESS,
                provider.getContext("last_status_memory-skill", SkillResult.Status.class).orElseThrow());
        assertEquals(true, provider.getContext("last_success_memory-skill", Boolean.class).orElseThrow());
    }

    @Test
    void ignoresNullResultAndKeepsLastResultEmpty() {
        SkillMemoryProvider provider = provider();

        provider.storeResult(null).await().indefinitely();

        assertTrue(provider.getLastResult().isEmpty());
        assertTrue(provider.getAllContext().isEmpty());
    }

    @Test
    void storesMetricsAsImmutableSanitizedSnapshot() {
        SkillMemoryProvider provider = provider();
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put(" executionTime ", 1000L);
        metrics.put("tokenCount", 150);
        metrics.put(null, "ignored");
        metrics.put("ignored", null);

        provider.storeMetrics(metrics).await().indefinitely();
        metrics.put("executionTime", 1L);

        Map<String, Object> allContext = provider.getAllContext();
        @SuppressWarnings("unchecked")
        Map<Object, Object> storedMetrics = assertInstanceOf(Map.class, allContext.get("metrics_memory-skill"));

        assertEquals(1000L, storedMetrics.get("executionTime"));
        assertEquals(150, storedMetrics.get("tokenCount"));
        assertFalse(storedMetrics.containsKey("ignored"));
        assertThrows(UnsupportedOperationException.class, () -> storedMetrics.put("new", "value"));
        assertThrows(UnsupportedOperationException.class, () -> allContext.put("new", "value"));
    }

    @Test
    void clearMemoryRemovesStoredContext() {
        SkillMemoryProvider provider = provider();
        provider.storeContext("key1", "value1").await().indefinitely();
        provider.storeContext("key2", 42).await().indefinitely();

        assertEquals(2, provider.getAllContext().size());

        provider.clearMemory().await().indefinitely();

        assertTrue(provider.getAllContext().isEmpty());
    }

    @Test
    void requiresContext() {
        assertThrows(NullPointerException.class, () -> new SkillMemoryProvider(null));
    }

    private static SkillMemoryProvider provider() {
        SkillContext context = TestSkillContexts.context("memory-skill", null);
        return new SkillMemoryProvider(context);
    }
}
