package tech.kayys.wayang.rag.runtime;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RagRuntimeListsTest {

    @Test
    void copiesNullableListsDefensively() {
        List<String> values = new ArrayList<>(List.of("a"));

        List<String> copied = RagRuntimeLists.copy(values);
        values.add("b");

        assertEquals(List.of("a"), copied);
        assertEquals(List.of(), RagRuntimeLists.copy(null));
        assertThrows(UnsupportedOperationException.class, () -> copied.add("c"));
    }

    @Test
    void returnsEmptyViewForNullLists() {
        List<String> values = new ArrayList<>(List.of("a"));

        assertEquals(List.of(), RagRuntimeLists.orEmpty(null));
        assertSame(values, RagRuntimeLists.orEmpty(values));
    }
}
