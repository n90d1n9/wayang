package tech.kayys.wayang.a2a.wayang;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static tech.kayys.wayang.a2a.wayang.WayangA2aJsonReportMaps.number;
import static tech.kayys.wayang.a2a.wayang.WayangA2aJsonReportMaps.text;

/**
 * Per-category rollup for A2A spec-alignment requirements.
 */
public record WayangA2aSpecAlignmentCategorySummary(
        String category,
        int requirementCount,
        int alignedCount,
        int gapCount,
        List<String> gapIds) {

    public WayangA2aSpecAlignmentCategorySummary {
        category = category == null || category.isBlank() ? "unknown" : category.trim();
        requirementCount = Math.max(0, requirementCount);
        alignedCount = Math.max(0, alignedCount);
        gapCount = Math.max(Math.max(0, gapCount), gapIds == null ? 0 : gapIds.size());
        gapIds = gapIds == null
                ? List.of()
                : gapIds.stream()
                        .map(WayangA2aMaps::optional)
                        .filter(Objects::nonNull)
                        .toList();
    }

    public static WayangA2aSpecAlignmentCategorySummary from(
            String category,
            List<WayangA2aSpecAlignmentRequirement> requirements) {
        List<WayangA2aSpecAlignmentRequirement> resolved = requirements == null
                ? List.of()
                : requirements.stream()
                        .filter(Objects::nonNull)
                        .toList();
        List<String> gaps = resolved.stream()
                .filter(requirement -> !requirement.aligned())
                .map(WayangA2aSpecAlignmentRequirement::id)
                .toList();
        return new WayangA2aSpecAlignmentCategorySummary(
                category,
                resolved.size(),
                resolved.size() - gaps.size(),
                gaps.size(),
                gaps);
    }

    public static WayangA2aSpecAlignmentCategorySummary fromMap(Map<?, ?> values) {
        Map<String, Object> copy = WayangA2aMaps.copyMap(values);
        return new WayangA2aSpecAlignmentCategorySummary(
                text(copy.get("category"), "unknown"),
                number(copy.get("requirementCount"), 0),
                number(copy.get("alignedCount"), 0),
                number(copy.get("gapCount"), 0),
                WayangA2aMaps.stringList(copy.get("gapIds")));
    }

    public boolean aligned() {
        return gapCount == 0;
    }

    public boolean hasGaps() {
        return !aligned();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("category", category);
        values.put("aligned", aligned());
        values.put("requirementCount", requirementCount);
        values.put("alignedCount", alignedCount);
        values.put("gapCount", gapCount);
        values.put("gapIds", gapIds);
        return WayangA2aMaps.copyMap(values);
    }
}
