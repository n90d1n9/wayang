package tech.kayys.wayang.agenticcommerce.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * HTTP-level checkout smoke probe result with decoded summary and probe issues.
 */
public record AgenticCommerceCheckoutHttpSmokeProbeResult(
        AgenticCommerceHttpResponse response,
        AgenticCommerceCheckoutHttpSmokeSummary summary,
        List<AgenticCommerceCheckoutHttpIssueSummary> issues,
        Map<String, Object> metadata) {

    public AgenticCommerceCheckoutHttpSmokeProbeResult {
        response = Objects.requireNonNull(response, "response");
        summary = summary == null ? AgenticCommerceCheckoutHttpSmokeSummary.fromMap(Map.of()) : summary;
        issues = issues == null
                ? List.of()
                : issues.stream()
                        .filter(Objects::nonNull)
                        .toList();
        metadata = AgenticCommerceMaps.copy(metadata);
    }

    public static AgenticCommerceCheckoutHttpSmokeProbeResult fromResponse(
            AgenticCommerceHttpResponse response) {
        AgenticCommerceHttpResponse resolved = Objects.requireNonNull(response, "response");
        List<AgenticCommerceCheckoutHttpIssueSummary> issues = new ArrayList<>();
        AgenticCommerceCheckoutHttpSmokeSummary summary = decodeSummary(resolved, issues);
        if (resolved.statusCode() < 200 || resolved.statusCode() >= 300) {
            issues.add(issue(
                    "probe_http_status",
                    "statusCode",
                    "Checkout smoke probe HTTP status is not successful.",
                    "2xx",
                    String.valueOf(resolved.statusCode())));
        }
        if (!resolved.contentType().isBlank() && !resolved.contentType(AgenticCommerceProtocol.MIME_JSON)) {
            issues.add(issue(
                    "probe_content_type",
                    AgenticCommerceProtocol.HEADER_CONTENT_TYPE,
                    "Checkout smoke probe Content-Type is not JSON.",
                    AgenticCommerceProtocol.MIME_JSON,
                    resolved.contentType()));
        }
        return new AgenticCommerceCheckoutHttpSmokeProbeResult(
                resolved,
                summary,
                issues,
                metadata(resolved, summary, issues));
    }

    public static AgenticCommerceCheckoutHttpSmokeProbeResult fromSummary(
            AgenticCommerceCheckoutHttpSmokeSummary summary) {
        AgenticCommerceCheckoutHttpSmokeSummary resolved = summary == null
                ? AgenticCommerceCheckoutHttpSmokeSummary.fromMap(Map.of())
                : summary;
        AgenticCommerceHttpResponse response = AgenticCommerceHttpResponse
                .json(resolved.passed() ? 200 : 500, AgenticCommerceJson.write(resolved.toMap()))
                .withAttributes(Map.of("operation", AgenticCommerceProtocol.OPERATION_CHECKOUT_SMOKE));
        return fromResponse(response);
    }

    public boolean valid() {
        return issues.isEmpty();
    }

    public boolean passed() {
        return valid() && summary.passed() && response.statusCode() >= 200 && response.statusCode() < 300;
    }

    public boolean failed() {
        return !passed();
    }

    public int exitCode() {
        return passed() ? 0 : 1;
    }

    public int issueCount() {
        return issues.size() + summary.issueCount();
    }

    public String operation() {
        String operation = AgenticCommerceValues.text(response.attributes(), "operation");
        return operation.isBlank() ? AgenticCommerceProtocol.OPERATION_CHECKOUT_SMOKE : operation;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("valid", valid());
        values.put("passed", passed());
        values.put("failed", failed());
        values.put("exitCode", exitCode());
        values.put("operation", operation());
        values.put("statusCode", response.statusCode());
        values.put("contentType", response.contentType());
        values.put("requestId", response.requestId());
        values.put("summaryPassed", summary.passed());
        values.put("summaryExitCode", summary.exitCode());
        values.put("issueCount", issueCount());
        values.put("probeIssueCount", issues.size());
        values.put("summaryIssueCount", summary.issueCount());
        values.put("summary", summary.toMap());
        AgenticCommerceValues.putList(values, "issues", issues.stream()
                .map(AgenticCommerceCheckoutHttpIssueSummary::toMap)
                .toList());
        AgenticCommerceValues.putMap(values, "metadata", metadata);
        return AgenticCommerceMaps.copy(values);
    }

    private static AgenticCommerceCheckoutHttpSmokeSummary decodeSummary(
            AgenticCommerceHttpResponse response,
            List<AgenticCommerceCheckoutHttpIssueSummary> issues) {
        if (response.body().isBlank()) {
            issues.add(issue(
                    "probe_missing_body",
                    "body",
                    "Checkout smoke probe response body is required.",
                    "summary JSON",
                    ""));
            return AgenticCommerceCheckoutHttpSmokeSummary.fromMap(Map.of());
        }
        try {
            return AgenticCommerceCheckoutHttpSmokeSummary.fromJson(response.body());
        } catch (IllegalArgumentException exception) {
            issues.add(issue(
                    "probe_invalid_json",
                    "body",
                    "Checkout smoke probe response body is not valid summary JSON.",
                    "summary JSON",
                    exception.getMessage()));
            return AgenticCommerceCheckoutHttpSmokeSummary.fromMap(Map.of());
        }
    }

    private static Map<String, Object> metadata(
            AgenticCommerceHttpResponse response,
            AgenticCommerceCheckoutHttpSmokeSummary summary,
            List<AgenticCommerceCheckoutHttpIssueSummary> issues) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("protocol", "agentic-commerce");
        values.put("operation", AgenticCommerceProtocol.OPERATION_CHECKOUT_SMOKE);
        values.put("routeCount", summary.routeCount());
        values.put("httpSuccessful", response.statusCode() >= 200 && response.statusCode() < 300);
        values.put("summaryPassed", summary.passed());
        values.put("probeIssueCount", issues.size());
        return AgenticCommerceMaps.copy(values);
    }

    private static AgenticCommerceCheckoutHttpIssueSummary issue(
            String code,
            String field,
            String message,
            String expected,
            String actual) {
        return AgenticCommerceCheckoutHttpIssueSummary.fromIssue(
                "probe",
                "",
                AgenticCommerceProtocol.OPERATION_CHECKOUT_SMOKE,
                AgenticCommerceValidationIssue.of(code, field, message, expected, actual));
    }
}
