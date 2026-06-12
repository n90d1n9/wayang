package tech.kayys.wayang.gollek.sdk;

import java.util.List;

final class SdkLists {

    private SdkLists() {
    }

    static <T> List<T> copy(List<T> values) {
        return values == null || values.isEmpty() ? List.of() : List.copyOf(values);
    }
}
