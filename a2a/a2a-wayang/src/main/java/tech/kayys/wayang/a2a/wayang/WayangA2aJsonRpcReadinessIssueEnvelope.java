package tech.kayys.wayang.a2a.wayang;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static tech.kayys.wayang.a2a.wayang.WayangA2aJsonReportMaps.number;
import static tech.kayys.wayang.a2a.wayang.WayangA2aJsonReportMaps.text;

final class WayangA2aJsonRpcReadinessIssueEnvelope {

    private WayangA2aJsonRpcReadinessIssueEnvelope() {
    }

    static Map<String, Object> wrap(String probe, Map<String, Object> values) {
        Map<String, Object> copy = WayangA2aMaps.copyMap(values);
        Map<String, Object> issue = new LinkedHashMap<>();
        issue.put("probe", text(probe, ""));
        issue.put("source", text(copy.get("source"), probe));
        issue.put("code", text(copy.get("code"), ""));
        issue.put("field", text(copy.get("field"), ""));
        issue.put("expected", text(copy.get("expected"), ""));
        issue.put("actual", text(copy.get("actual"), ""));
        issue.put("message", text(copy.get("message"), ""));
        issue.put("statusCode", number(copy.get("statusCode"), 0));
        issue.put("routeOperation", text(copy.get("routeOperation"), ""));
        issue.put("metadata", metadata(copy));
        return WayangA2aMaps.copyMap(issue);
    }

    static List<Map<String, Object>> wrapAll(String probe, List<Map<String, Object>> issues) {
        if (issues == null || issues.isEmpty()) {
            return List.of();
        }
        return issues.stream()
                .map(issue -> wrap(probe, issue))
                .toList();
    }

    private static Map<String, Object> metadata(Map<String, Object> values) {
        if (values.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> metadata = new LinkedHashMap<>(values);
        metadata.keySet().removeAll(List.of(
                "source",
                "code",
                "field",
                "expected",
                "actual",
                "message",
                "statusCode",
                "routeOperation"));
        return WayangA2aMaps.copyMap(metadata);
    }
}
