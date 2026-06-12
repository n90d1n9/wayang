package tech.kayys.wayang.rag.runtime;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.rag.core.RagQuery;
import tech.kayys.wayang.rag.core.RagResult;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RagRuntimeMetadataTest {

    @Test
    void copiesMetadataDefensivelyAndPreservesNullableValues() {
        Map<String, Object> input = new HashMap<>();
        input.put("key", "value");
        input.put("nullable", null);

        Map<String, Object> copied = RagRuntimeMetadata.copy(input);
        input.put("key", "changed");

        assertEquals("value", copied.get("key"));
        assertNull(copied.get("nullable"));
        assertThrows(UnsupportedOperationException.class, () -> copied.put("other", "value"));
    }

    @Test
    void createsMutableBuilderCopies() {
        Map<String, Object> copied = RagRuntimeMetadata.mutableCopy(Map.of("key", "value"));

        copied.put("other", "value");

        assertEquals(Map.of("key", "value", "other", "value"), copied);
    }

    @Test
    void copiesStringMetadataDefensivelyAndSkipsNullEntries() {
        Map<String, String> input = new HashMap<>();
        input.put("collection", "docs");
        input.put("ignored", null);
        input.put(null, "ignored");

        Map<String, String> copied = RagRuntimeMetadata.copyStrings(input);
        input.put("collection", "mutated");

        assertEquals(Map.of("collection", "docs"), copied);
        assertThrows(UnsupportedOperationException.class, () -> copied.put("other", "value"));
    }

    @Test
    void stringifiesObjectMetadataDefensivelyAndSkipsNullEntries() {
        Map<String, Object> input = new HashMap<>();
        input.put("page", 2);
        input.put("source", "manual");
        input.put("ignored", null);
        input.put(null, "ignored");

        Map<String, String> copied = RagRuntimeMetadata.stringifyValues(input);
        input.put("page", 3);

        assertEquals(Map.of("page", "2", "source", "manual"), copied);
        assertThrows(UnsupportedOperationException.class, () -> copied.put("other", "value"));
    }

    @Test
    void copiesResultMetadataDefensively() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("retrieved", 1);
        RagResult result = new RagResult(RagQuery.of("question"), null, "", metadata);

        Map<String, Object> copied = RagRuntimeMetadata.fromResult(result);
        metadata.put("retrieved", 2);

        assertEquals(1, copied.get("retrieved"));
        assertEquals(Map.of(), RagRuntimeMetadata.fromResult(null));
    }
}
