package tech.kayys.wayang.a2a.wayang;

import java.util.LinkedHashMap;
import java.util.Map;

final class WayangA2aJsonRpcDiagnosticIssues {

    private WayangA2aJsonRpcDiagnosticIssues() {
    }

    static Map<String, Object> issue(
            String source,
            String code,
            String field,
            String expected,
            String actual,
            String message) {
        Map<String, Object> issue = new LinkedHashMap<>();
        issue.put("source", source);
        issue.put("code", code);
        issue.put("field", field);
        issue.put("expected", expected);
        issue.put("actual", actual == null ? "" : actual);
        issue.put("message", message);
        return WayangA2aMaps.copyMap(issue);
    }

    static Map<String, Object> probeIssue(
            String code,
            String message,
            int statusCode,
            String routeOperation) {
        Map<String, Object> issue = new LinkedHashMap<>();
        issue.put("code", code);
        issue.put("message", message);
        issue.put("statusCode", Math.max(0, statusCode));
        issue.put("routeOperation", routeOperation == null ? "" : routeOperation);
        return WayangA2aMaps.copyMap(issue);
    }
}
