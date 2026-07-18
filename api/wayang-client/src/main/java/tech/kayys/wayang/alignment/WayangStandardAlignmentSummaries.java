package tech.kayys.wayang.alignment;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import tech.kayys.wayang.client.SdkLists;

final class WayangStandardAlignmentSummaries {

    private WayangStandardAlignmentSummaries() {
    }

    static List<WayangStandardAlignmentSummary> mergeByStandardId(
            List<WayangStandardAlignmentSummary> summaries) {
        if (summaries == null || summaries.isEmpty()) {
            return List.of();
        }
        Map<String, WayangStandardAlignmentSummary> merged = new LinkedHashMap<>();
        summaries.stream()
                .filter(summary -> summary != null && !"unknown".equals(summary.standardId()))
                .forEach(summary -> merged.merge(summary.standardId(), summary, WayangStandardAlignmentSummaries::merge));
        return List.copyOf(merged.values());
    }

    private static WayangStandardAlignmentSummary merge(
            WayangStandardAlignmentSummary first,
            WayangStandardAlignmentSummary next) {
        List<String> gapIds = union(first.gapIds(), next.gapIds());
        List<String> gapCategories = union(first.gapCategories(), next.gapCategories());
        List<WayangStandardAlignmentSource> sources = sourceUnion(first.sources(), next.sources());
        return new WayangStandardAlignmentSummary(
                bestDescriptor(first.standard(), next.standard()),
                first.aligned() && next.aligned(),
                Math.max(first.requirementCount(), next.requirementCount()),
                conservativeAlignedCount(first.alignedCount(), next.alignedCount()),
                Math.max(first.gapCount(), next.gapCount()),
                gapIds,
                gapCategories,
                sources);
    }

    private static WayangStandardAlignmentDescriptor bestDescriptor(
            WayangStandardAlignmentDescriptor first,
            WayangStandardAlignmentDescriptor next) {
        return descriptorScore(next) > descriptorScore(first) ? next : first;
    }

    private static int descriptorScore(WayangStandardAlignmentDescriptor descriptor) {
        if (descriptor == null) {
            return 0;
        }
        int score = 0;
        score += present(descriptor.name()) && !descriptor.name().equals(descriptor.standardId()) ? 2 : 0;
        score += present(descriptor.version()) ? 2 : 0;
        score += present(descriptor.binding()) ? 1 : 0;
        score += present(descriptor.specUrl()) ? 2 : 0;
        score += descriptor.attributes().size();
        return score;
    }

    private static boolean present(String value) {
        return value != null && !value.isBlank();
    }

    private static int conservativeAlignedCount(int first, int next) {
        if (first <= 0) {
            return Math.max(0, next);
        }
        if (next <= 0) {
            return first;
        }
        return Math.min(first, next);
    }

    private static List<String> union(List<String> first, List<String> next) {
        Set<String> values = new LinkedHashSet<>();
        values.addAll(SdkLists.copy(first));
        values.addAll(SdkLists.copy(next));
        return List.copyOf(values);
    }

    private static List<WayangStandardAlignmentSource> sourceUnion(
            List<WayangStandardAlignmentSource> first,
            List<WayangStandardAlignmentSource> next) {
        Map<String, WayangStandardAlignmentSource> values = new LinkedHashMap<>();
        SdkLists.copy(first).stream()
                .filter(source -> source != null && !source.empty())
                .forEach(source -> values.putIfAbsent(source.toMap().toString(), source));
        SdkLists.copy(next).stream()
                .filter(source -> source != null && !source.empty())
                .forEach(source -> values.putIfAbsent(source.toMap().toString(), source));
        return List.copyOf(values.values());
    }
}
