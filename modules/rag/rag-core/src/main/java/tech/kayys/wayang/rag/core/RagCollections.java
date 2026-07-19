package tech.kayys.wayang.rag.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class RagCollections {

    private RagCollections() {
    }

    static <T> List<T> copyList(List<T> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return Collections.unmodifiableList(new ArrayList<>(values));
    }
}
