package tech.kayys.wayang.a2a.wayang;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static tech.kayys.wayang.a2a.wayang.WayangA2aHttpResponseHeaders.routeOperationHeader;
import static tech.kayys.wayang.a2a.wayang.WayangA2aJsonRpcDiagnosticIssues.issue;

final class WayangA2aJsonRpcProbeResponseChecks {

    private WayangA2aJsonRpcProbeResponseChecks() {
    }

    static List<Map<String, Object>> responseIssues(
            WayangA2aHttpResponse response,
            String source,
            String expectedRouteOperation,
            String routeMismatchCode,
            String responseLabel) {
        WayangA2aHttpResponse resolved = Objects.requireNonNull(response, "response");
        String routeOperation = routeOperationHeader(resolved);
        String label = responseLabel == null ? "" : responseLabel.trim();
        List<Map<String, Object>> issues = new ArrayList<>();
        if (!resolved.successful()) {
            issues.add(issue(
                    "http",
                    "http_unsuccessful",
                    "httpSuccessful",
                    "true",
                    String.valueOf(false),
                    "A2A JSON-RPC " + label + " was not HTTP-successful."));
        }
        if (!expectedRouteOperation.equals(routeOperation)) {
            issues.add(issue(
                    source,
                    routeMismatchCode,
                    "routeOperation",
                    expectedRouteOperation,
                    routeOperation,
                    "A2A JSON-RPC " + label + " used an unexpected route operation."));
        }
        if (!WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON.equals(resolved.contentType())) {
            issues.add(issue(
                    source,
                    "json_content_mismatch",
                    "contentType",
                    WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON,
                    resolved.contentType(),
                    "A2A JSON-RPC " + label + " was not JSON."));
        }
        return List.copyOf(issues);
    }

    static Map<String, Object> countMissingIssue(
            String source,
            String code,
            String field,
            int actual,
            String subject,
            String itemPlural) {
        return issue(
                source,
                code,
                field,
                "> 0",
                String.valueOf(actual),
                "A2A JSON-RPC " + subject + " did not expose any " + itemPlural + ".");
    }
}
