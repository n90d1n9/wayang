package tech.kayys.wayang.rag.runtime;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RagRuntimeTextTest {

    @Test
    void trimsNullableTextToEmpty() {
        assertEquals("", RagRuntimeText.trimToEmpty(null));
        assertEquals("", RagRuntimeText.trimToEmpty("  "));
        assertEquals("tenant", RagRuntimeText.trimToEmpty(" tenant "));
    }

    @Test
    void trimsNullableTextToDefault() {
        assertEquals("default", RagRuntimeText.trimToDefault(null, "default"));
        assertEquals("default", RagRuntimeText.trimToDefault(" ", "default"));
        assertEquals("docs", RagRuntimeText.trimToDefault(" docs ", "default"));
    }

    @Test
    void trimsNullableTextToLowercaseEmpty() {
        assertEquals("", RagRuntimeText.trimToLowerEmpty(null));
        assertEquals("", RagRuntimeText.trimToLowerEmpty(" "));
        assertEquals("plugin-a", RagRuntimeText.trimToLowerEmpty(" Plugin-A "));
    }
}
