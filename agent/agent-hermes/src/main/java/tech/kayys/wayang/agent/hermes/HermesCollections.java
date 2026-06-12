package tech.kayys.wayang.agent.hermes;

import java.util.List;
import java.util.Objects;

final class HermesCollections {

    private HermesCollections() {
    }

    static <T> List<T> copyNonNull(List<? extends T> values) {
        return values == null ? List.of() : List.copyOf(values.stream()
                .filter(Objects::nonNull)
                .toList());
    }
}
