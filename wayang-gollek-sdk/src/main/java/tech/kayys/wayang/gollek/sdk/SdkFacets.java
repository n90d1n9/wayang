package tech.kayys.wayang.gollek.sdk;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

final class SdkFacets {

    private SdkFacets() {
    }

    static <T> List<String> values(List<T> source, Function<T, String> facet) {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        for (T item : SdkLists.copy(source)) {
            String value = facet.apply(item);
            if (value != null) {
                values.add(value);
            }
        }
        return values.isEmpty() ? List.of() : List.copyOf(values);
    }

    static <T> List<String> textValues(List<T> source, Function<T, String> facet) {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        for (T item : SdkLists.copy(source)) {
            String value = SdkText.trimToEmpty(facet.apply(item));
            if (!value.isEmpty()) {
                values.add(value);
            }
        }
        return values.isEmpty() ? List.of() : List.copyOf(values);
    }

    static <T> List<String> flatValues(List<T> source, Function<T, List<String>> facet) {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        for (T item : SdkLists.copy(source)) {
            values.addAll(SdkLists.copy(facet.apply(item)));
        }
        return values.isEmpty() ? List.of() : List.copyOf(values);
    }

    static <T> Map<String, Integer> counts(List<T> source, Function<T, String> facet) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (T item : SdkLists.copy(source)) {
            String value = facet.apply(item);
            if (value != null) {
                counts.merge(value, 1, Integer::sum);
            }
        }
        return SdkCounts.copy(counts);
    }

    static <T> Map<String, Integer> textCounts(List<T> source, Function<T, String> facet) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (T item : SdkLists.copy(source)) {
            String value = SdkText.trimToEmpty(facet.apply(item));
            if (!value.isEmpty()) {
                counts.merge(value, 1, Integer::sum);
            }
        }
        return SdkCounts.copyPositiveTextKeys(counts);
    }

    static <T> Map<String, Integer> flatCounts(List<T> source, Function<T, List<String>> facet) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (T item : SdkLists.copy(source)) {
            for (String value : SdkLists.copy(facet.apply(item))) {
                counts.merge(value, 1, Integer::sum);
            }
        }
        return SdkCounts.copy(counts);
    }
}
