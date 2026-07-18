package tech.kayys.wayang.alignment;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import tech.kayys.wayang.client.SdkLists;
import tech.kayys.wayang.client.SdkMaps;

/**
 * Compact, standard-neutral alignment summary derived from a protocol-specific report map.
 */
public record WayangStandardAlignmentSummary(
        WayangStandardAlignmentDescriptor standard,
        boolean aligned,
        int requirementCount,
        int alignedCount,
        int gapCount,
        List<String> gapIds,
        List<String> gapCategories,
        List<WayangStandardAlignmentSource> sources) {

    public WayangStandardAlignmentSummary {
        standard = standard == null ? WayangStandardAlignmentDescriptor.fromReportMap(Map.of()) : standard;
        requirementCount = Math.max(0, requirementCount);
        alignedCount = Math.max(0, alignedCount);
        gapIds = SdkLists.copy(gapIds);
        gapCategories = SdkLists.copy(gapCategories);
        sources = sources == null
                ? List.of()
                : sources.stream()
                        .filter(source -> source != null && !source.empty())
                        .toList();
        gapCount = Math.max(Math.max(0, gapCount), gapIds.size());
    }

    public WayangStandardAlignmentSummary(
            WayangStandardAlignmentDescriptor standard,
            boolean aligned,
            int requirementCount,
            int alignedCount,
            int gapCount,
            List<String> gapIds,
            List<String> gapCategories) {
        this(standard, aligned, requirementCount, alignedCount, gapCount, gapIds, gapCategories, List.of());
    }

    public static WayangStandardAlignmentSummary fromReportMap(Map<?, ?> report) {
        Map<String, Object> resolved = WayangStandardAlignmentReportMaps.resolve(report);
        return new WayangStandardAlignmentSummary(
                WayangStandardAlignmentDescriptor.fromReportMap(resolved),
                WayangStandardAlignmentMaps.bool(resolved, "aligned"),
                WayangStandardAlignmentMaps.number(resolved, "requirementCount"),
                WayangStandardAlignmentMaps.number(resolved, "alignedCount"),
                WayangStandardAlignmentMaps.number(resolved, "gapCount"),
                WayangStandardAlignmentMaps.stringList(resolved, "gapIds"),
                WayangStandardAlignmentMaps.stringList(resolved, "gapCategories"),
                WayangStandardAlignmentSource.fromReportMap(resolved));
    }

    public String standardId() {
        return standard.standardId();
    }

    public boolean hasGaps() {
        return gapCount > 0 || !gapIds.isEmpty();
    }

    public boolean hasSources() {
        return !sources.isEmpty();
    }

    public int sourceCount() {
        return sources.size();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("standard", standard.toMap());
        values.put("aligned", aligned);
        values.put("requirementCount", requirementCount);
        values.put("alignedCount", alignedCount);
        values.put("gapCount", gapCount);
        values.put("gapIds", gapIds);
        values.put("gapCategories", gapCategories);
        values.put("sourceCount", sourceCount());
        values.put("sources", sources.stream()
                .map(WayangStandardAlignmentSource::toMap)
                .toList());
        return SdkMaps.orderedCopy(values);
    }
}
