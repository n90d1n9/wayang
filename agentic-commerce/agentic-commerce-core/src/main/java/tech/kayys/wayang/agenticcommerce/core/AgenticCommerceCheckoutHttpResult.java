package tech.kayys.wayang.agenticcommerce.core;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Decoded Agentic Commerce checkout HTTP response.
 */
public record AgenticCommerceCheckoutHttpResult(
        AgenticCommerceHttpRequest request,
        AgenticCommerceHttpResponse response,
        AgenticCommerceResponseValidationReport validation,
        AgenticCommerceCheckoutSession checkoutSession,
        AgenticCommerceError error,
        List<AgenticCommerceMessage> messages,
        Map<String, Object> body,
        List<AgenticCommerceValidationIssue> issues,
        Map<String, Object> metadata) {

    public AgenticCommerceCheckoutHttpResult {
        request = Objects.requireNonNull(request, "request");
        response = Objects.requireNonNull(response, "response");
        validation = Objects.requireNonNull(validation, "validation");
        checkoutSession = checkoutSession == null ? AgenticCommerceCheckoutSession.empty() : checkoutSession;
        error = error == null ? new AgenticCommerceError("", "", "", Map.of(), Map.of()) : error;
        messages = messages == null
                ? List.of()
                : messages.stream()
                        .filter(message -> message != null && !message.isEmpty())
                        .toList();
        body = AgenticCommerceMaps.copy(body);
        issues = issues == null
                ? List.of()
                : issues.stream()
                        .filter(Objects::nonNull)
                        .toList();
        metadata = AgenticCommerceMaps.copy(metadata);
    }

    public boolean valid() {
        return issues.isEmpty();
    }

    public boolean successful() {
        return valid()
                && response.statusCode() >= 200
                && response.statusCode() < 300
                && !hasError();
    }

    public boolean hasCheckoutSession() {
        return !checkoutSession.isEmpty();
    }

    public boolean hasError() {
        return !error.isEmpty();
    }

    public int issueCount() {
        return issues.size();
    }

    public String operation() {
        return validation.route()
                .map(AgenticCommerceHttpRoute::operation)
                .orElseGet(() -> AgenticCommerceValues.text(response.attributes(), "operation"));
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("valid", valid());
        values.put("successful", successful());
        values.put("operation", operation());
        values.put("statusCode", response.statusCode());
        values.put("issueCount", issueCount());
        if (hasCheckoutSession()) {
            values.put("checkoutSession", checkoutSession.toMap());
        }
        if (hasError()) {
            values.put("error", error.toMap());
        }
        AgenticCommerceValues.putList(values, "messages", messages.stream()
                .map(AgenticCommerceMessage::toMap)
                .toList());
        AgenticCommerceValues.putMap(values, "body", body);
        values.put("validation", validation.toMap());
        AgenticCommerceValues.putList(values, "issues", issues.stream()
                .map(AgenticCommerceValidationIssue::toMap)
                .toList());
        AgenticCommerceValues.putMap(values, "metadata", metadata);
        return AgenticCommerceMaps.copy(values);
    }
}
