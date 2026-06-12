package tech.kayys.wayang.a2a.wayang;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

record WayangA2aJsonRpcReadinessSpecAlignmentChecks(WayangA2aSpecAlignmentSnapshot specAlignment) {

    WayangA2aJsonRpcReadinessSpecAlignmentChecks {
        specAlignment = specAlignment == null
                ? WayangA2aSpecAlignmentSnapshot.defaults()
                : specAlignment;
    }

    static WayangA2aJsonRpcReadinessSpecAlignmentChecks from(
            WayangA2aSpecAlignmentSnapshot specAlignment) {
        return new WayangA2aJsonRpcReadinessSpecAlignmentChecks(specAlignment);
    }

    List<Map<String, Object>> toMaps() {
        List<Map<String, Object>> checks = new ArrayList<>();
        checks.add(summaryRow().toMap());
        specAlignment.categorySummaries().stream()
                .map(WayangA2aJsonRpcReadinessSpecAlignmentChecks::categoryRow)
                .forEach(checks::add);
        return List.copyOf(checks);
    }

    private WayangA2aJsonRpcReadinessProbeCheck summaryRow() {
        return new WayangA2aJsonRpcReadinessProbeCheck(
                WayangA2aJsonRpcReadinessIssueCatalog.PROBE_SPEC_ALIGNMENT,
                true,
                specAlignment.aligned(),
                0,
                "",
                specAlignment.gapCount(),
                "",
                "");
    }

    private static Map<String, Object> categoryRow(WayangA2aSpecAlignmentCategorySummary summary) {
        WayangA2aSpecAlignmentCategorySummary resolved = Objects.requireNonNull(summary, "summary");
        Map<String, Object> values = new LinkedHashMap<>();
        values.put(
                "probe",
                WayangA2aJsonRpcReadinessIssueCatalog.specAlignmentCategoryProbe(resolved.category()));
        values.put("category", resolved.category());
        values.put("required", true);
        values.put("passed", resolved.aligned());
        values.put("statusCode", 0);
        values.put("routeOperation", "");
        values.put("issueCount", resolved.gapCount());
        values.put("gapIds", resolved.gapIds());
        return WayangA2aMaps.copyMap(values);
    }
}
