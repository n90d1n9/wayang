package tech.kayys.wayang.rag.runtime;

import java.util.List;

final class RagRuntimeLists {

    private RagRuntimeLists() {
    }

    static <T> List<T> copy(List<? extends T> values) {
        return values == null || values.isEmpty() ? List.of() : List.copyOf(values);
    }

    static <T> List<T> orEmpty(List<T> values) {
        return values == null ? List.of() : values;
    }
}
