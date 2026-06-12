package tech.kayys.wayang.a2a.wayang;

import tech.kayys.wayang.a2a.core.A2aProtocol;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

final class WayangA2aSpecAlignmentReportProjection {

    private WayangA2aSpecAlignmentReportProjection() {
    }

    static Map<String, Object> report(WayangA2aSpecAlignmentReport report) {
        WayangA2aSpecAlignmentReport resolved = Objects.requireNonNull(report, "report");
        WayangA2aSpecAlignmentCategorySummaries categorySummaries =
                WayangA2aSpecAlignmentCategorySummaries.fromRequirements(resolved.requirements());
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("protocol", "a2a");
        values.put("protocolVersion", A2aProtocol.VERSION);
        values.put("binding", A2aProtocol.BINDING_JSONRPC);
        values.put("standard", WayangA2aSpecAlignmentReport.standardDescriptor());
        values.put("aligned", resolved.aligned());
        values.put("requirementCount", resolved.requirementCount());
        values.put("alignedCount", resolved.alignedCount());
        values.put("gapCount", resolved.gapCount());
        values.put("requirementIds", resolved.requirementIds());
        values.put("gapIds", resolved.gapIds());
        values.put("gapCategories", categorySummaries.gapCategories());
        values.put("categorySummaries", categorySummaries.maps());
        values.put("routeCatalog", resolved.routeCatalog().toMap());
        values.put("requirements", resolved.requirements().stream()
                .map(WayangA2aSpecAlignmentRequirement::toMap)
                .toList());
        return WayangA2aMaps.copyMap(values);
    }
}
