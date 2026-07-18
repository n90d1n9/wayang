package tech.kayys.wayang.alignment;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import tech.kayys.wayang.client.SdkMaps;

final class WayangStandardAlignmentReportMaps {

    private static final List<String> CARRIER_KEYS = List.of(
            "specAlignment",
            "standardAlignment",
            "alignment",
            "spec_alignment",
            "standard_alignment");
    private static final List<String> COLLECTION_KEYS = List.of(
            "standards",
            "alignments",
            "alignmentReports",
            "standardAlignments",
            "specAlignments",
            "alignment_reports",
            "standard_alignments",
            "spec_alignments",
            "reports");

    private WayangStandardAlignmentReportMaps() {
    }

    static List<Map<String, Object>> expand(Map<?, ?> report) {
        Map<String, Object> copied = WayangStandardAlignmentMaps.copy(report);
        List<Map<String, Object>> nested = nestedAlignments(copied);
        if (!nested.isEmpty()) {
            return nested;
        }

        Map<String, Object> attributes = WayangStandardAlignmentMaps.map(copied.get("attributes"));
        nested = nestedAlignments(attributes);
        if (!nested.isEmpty()) {
            return nested;
        }

        Map<String, Object> resolved = resolve(copied);
        return resolved.isEmpty() ? List.of() : List.of(resolved);
    }

    static Map<String, Object> resolve(Map<?, ?> report) {
        Map<String, Object> copied = WayangStandardAlignmentMaps.copy(report);
        if (hasStandardIdentity(copied)) {
            return withSources(copied, copied);
        }

        Map<String, Object> nested = nestedAlignment(copied);
        if (!nested.isEmpty()) {
            return withSources(nested, nested, copied);
        }

        Map<String, Object> attributes = WayangStandardAlignmentMaps.map(copied.get("attributes"));
        nested = nestedAlignment(attributes);
        if (!nested.isEmpty()) {
            return withSources(nested, nested, attributes, copied);
        }

        return hasAlignmentMetrics(copied) ? withSources(copied, copied) : Map.of();
    }

    private static List<Map<String, Object>> nestedAlignments(Map<String, Object> values) {
        for (String key : COLLECTION_KEYS) {
            List<Map<String, Object>> candidates = alignmentList(values.get(key));
            if (!candidates.isEmpty()) {
                return candidates;
            }
        }
        return List.of();
    }

    private static List<Map<String, Object>> alignmentList(Object value) {
        if (!(value instanceof Collection<?> collection)) {
            return List.of();
        }
        return collection.stream()
                .map(WayangStandardAlignmentMaps::map)
                .map(candidate -> withSources(resolve(candidate), candidate))
                .filter(candidate -> hasStandardIdentity(candidate) || hasAlignmentMetrics(candidate))
                .toList();
    }

    private static Map<String, Object> nestedAlignment(Map<String, Object> values) {
        for (String key : CARRIER_KEYS) {
            Map<String, Object> candidate = WayangStandardAlignmentMaps.map(values.get(key));
            if (hasStandardIdentity(candidate) || hasAlignmentMetrics(candidate)) {
                return candidate;
            }
        }
        return Map.of();
    }

    private static boolean hasStandardIdentity(Map<String, Object> values) {
        return !WayangStandardAlignmentMaps.map(values.get("standard")).isEmpty()
                || !WayangStandardAlignmentMaps
                        .firstText(values, "standardId", "protocol", "standardName")
                        .isEmpty();
    }

    private static boolean hasAlignmentMetrics(Map<String, Object> values) {
        return values.containsKey("aligned")
                || values.containsKey("requirementCount")
                || values.containsKey("alignedCount")
                || values.containsKey("gapCount")
                || !WayangStandardAlignmentMaps.stringList(values, "gapIds").isEmpty()
                || !WayangStandardAlignmentMaps.stringList(values, "gapCategories").isEmpty();
    }

    private static Map<String, Object> withSources(Map<String, Object> alignment, Map<String, Object>... carriers) {
        if (alignment.isEmpty()) {
            return Map.of();
        }
        java.util.LinkedHashMap<String, WayangStandardAlignmentSource> sources = new java.util.LinkedHashMap<>();
        WayangStandardAlignmentSource.fromReportMap(alignment)
                .forEach(source -> sources.putIfAbsent(source.toMap().toString(), source));
        for (Map<String, Object> carrier : carriers) {
            WayangStandardAlignmentSource.fromReportMap(carrier)
                    .forEach(source -> sources.putIfAbsent(source.toMap().toString(), source));
        }
        if (sources.isEmpty()) {
            return alignment;
        }
        java.util.LinkedHashMap<String, Object> values = new java.util.LinkedHashMap<>(alignment);
        values.put("sources", sources.values().stream()
                .map(WayangStandardAlignmentSource::toMap)
                .toList());
        return SdkMaps.orderedCopy(values);
    }
}
