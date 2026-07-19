package tech.kayys.wayang.client;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

final public class SdkFacets {

    private SdkFacets() {
    }

    public static <T> List<String> values(List<T> source, Function<T, String> facet) {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        for (T item : SdkLists.copy(source)) {
            String value = facet.apply(item);
            if (value != null) {
                values.add(value);
            }
        }
        return values.isEmpty() ? List.of() : List.copyOf(values);
    }

    public static <T> List<String> textValues(List<T> source, Function<T, String> facet) {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        for (T item : SdkLists.copy(source)) {
            String value = SdkText.trimToEmpty(facet.apply(item));
            if (!value.isEmpty()) {
                values.add(value);
            }
        }
        return values.isEmpty() ? List.of() : List.copyOf(values);
    }

    public static <T> List<String> flatValues(List<T> source, Function<T, List<String>> facet) {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        for (T item : SdkLists.copy(source)) {
            values.addAll(SdkLists.copy(facet.apply(item)));
        }
        return values.isEmpty() ? List.of() : List.copyOf(values);
    }

    public static <T> Map<String, Integer> counts(List<T> source, Function<T, String> facet) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (T item : SdkLists.copy(source)) {
            String value = facet.apply(item);
            if (value != null) {
                counts.merge(value, 1, Integer::sum);
            }
        }
        return SdkCounts.copy(counts);
    }

    public static <T> Map<String, Integer> textCounts(List<T> source, Function<T, String> facet) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (T item : SdkLists.copy(source)) {
            String value = SdkText.trimToEmpty(facet.apply(item));
            if (!value.isEmpty()) {
                counts.merge(value, 1, Integer::sum);
            }
        }
        return SdkCounts.copyPositiveTextKeys(counts);
    }

    public static <T> Map<String, Integer> flatCounts(List<T> source, Function<T, List<String>> facet) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (T item : SdkLists.copy(source)) {
            for (String value : SdkLists.copy(facet.apply(item))) {
                counts.merge(value, 1, Integer::sum);
            }
        }
        return SdkCounts.copy(counts);
    }
}
