package tech.kayys.wayang.agenticcommerce.core;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Validation result for a framework-neutral Agentic Commerce HTTP response.
 */
public record AgenticCommerceResponseValidationReport(
        AgenticCommerceHttpRequest request,
        AgenticCommerceHttpResponse response,
        Optional<AgenticCommerceHttpRoute> route,
        List<AgenticCommerceValidationIssue> issues) {

    public AgenticCommerceResponseValidationReport {
        request = Objects.requireNonNull(request, "request");
        response = Objects.requireNonNull(response, "response");
        route = route == null ? Optional.empty() : route;
        issues = issues == null
                ? List.of()
                : issues.stream()
                        .filter(Objects::nonNull)
                        .toList();
    }

    public boolean valid() {
        return issues.isEmpty();
    }

    public int issueCount() {
        return issues.size();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("valid", valid());
        values.put("issueCount", issueCount());
        values.put("operation", route.map(AgenticCommerceHttpRoute::operation).orElse(""));
        values.put("request", requestMap());
        values.put("response", responseMap());
        values.put("issues", issues.stream()
                .map(AgenticCommerceValidationIssue::toMap)
                .toList());
        return AgenticCommerceMaps.copy(values);
    }

    private Map<String, Object> requestMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("method", request.method());
        values.put("path", request.path());
        values.put("idempotencyKeyPresent", !request.header(AgenticCommerceProtocol.HEADER_IDEMPOTENCY_KEY)
                .orElse("")
                .isBlank());
        values.put("attributes", request.attributes());
        return AgenticCommerceMaps.copy(values);
    }

    private Map<String, Object> responseMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("statusCode", response.statusCode());
        values.put("contentType", response.contentType());
        values.put("bodyPresent", !response.body().isBlank());
        values.put("requestId", response.requestId());
        values.put("idempotencyKey", response.idempotencyKey());
        values.put("headers", response.headers());
        values.put("attributes", response.attributes());
        return AgenticCommerceMaps.copy(values);
    }
}
