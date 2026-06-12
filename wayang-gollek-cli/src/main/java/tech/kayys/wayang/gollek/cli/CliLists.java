package tech.kayys.wayang.gollek.cli;

import java.util.List;

final class CliLists {

    private CliLists() {
    }

    static <T> List<T> copy(List<T> values) {
        return values == null || values.isEmpty() ? List.of() : List.copyOf(values);
    }
}
