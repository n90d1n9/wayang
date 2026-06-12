package tech.kayys.wayang.agenticcommerce.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Checkout response decoder for Agentic Commerce Protocol HTTP bindings.
 */
public final class AgenticCommerceCheckoutHttpResponses {

    private AgenticCommerceCheckoutHttpResponses() {
    }

    public static AgenticCommerceCheckoutHttpResult decode(
            AgenticCommerceHttpRequest request,
            AgenticCommerceHttpResponse response) {
        return decode(request, response, AgenticCommerceResponseValidator.checkout());
    }

    public static AgenticCommerceCheckoutHttpResult decode(
            AgenticCommerceHttpRequest request,
            AgenticCommerceHttpResponse response,
            AgenticCommerceResponseValidator validator) {
        AgenticCommerceResponseValidator resolvedValidator = validator == null
                ? AgenticCommerceResponseValidator.checkout()
                : validator;
        AgenticCommerceResponseValidationReport validation = resolvedValidator.validate(request, response);
        List<AgenticCommerceValidationIssue> issues = new ArrayList<>(validation.issues());
        Map<String, Object> body = parseBody(response, issues);
        Map<String, Object> sessionBody = sessionBody(body);
        AgenticCommerceCheckoutSession checkoutSession = body.isEmpty()
                ? AgenticCommerceCheckoutSession.empty()
                : AgenticCommerceCheckoutSession.fromMap(sessionBody);
        AgenticCommerceError error = error(response, body);
        List<AgenticCommerceMessage> messages = messages(body, checkoutSession);
        Map<String, Object> metadata = metadata(body, validation, checkoutSession, error);
        return new AgenticCommerceCheckoutHttpResult(
                request,
                response,
                validation,
                checkoutSession,
                error,
                messages,
                body,
                issues,
                metadata);
    }

    private static Map<String, Object> parseBody(
            AgenticCommerceHttpResponse response,
            List<AgenticCommerceValidationIssue> issues) {
        if (response.body().isBlank()) {
            return Map.of();
        }
        try {
            return AgenticCommerceJson.readObject(response.body());
        } catch (IllegalArgumentException exception) {
            issues.add(AgenticCommerceValidationIssue.of(
                    "invalid_json_body",
                    "body",
                    "Response body is not valid JSON object content.",
                    "JSON object",
                    exception.getMessage()));
            return Map.of();
        }
    }

    private static Map<String, Object> sessionBody(Map<String, Object> body) {
        Map<String, Object> nested = AgenticCommerceValues.map(
                body,
                "checkout_session",
                "checkoutSession",
                "session");
        return nested.isEmpty() ? body : nested;
    }

    private static AgenticCommerceError error(
            AgenticCommerceHttpResponse response,
            Map<String, Object> body) {
        if (body.isEmpty()) {
            return new AgenticCommerceError("", "", "", Map.of(), Map.of());
        }
        Map<String, Object> nestedError = AgenticCommerceValues.map(body, "error");
        if (!nestedError.isEmpty()) {
            return AgenticCommerceError.fromMap(body);
        }
        if (response.statusCode() < 400) {
            return new AgenticCommerceError("", "", "", Map.of(), Map.of());
        }
        AgenticCommerceError parsed = AgenticCommerceError.fromMap(body);
        if (!parsed.isEmpty()) {
            return parsed;
        }
        return new AgenticCommerceError(
                "http_error",
                "http_" + response.statusCode(),
                "Agentic Commerce checkout request failed.",
                body,
                Map.of());
    }

    private static List<AgenticCommerceMessage> messages(
            Map<String, Object> body,
            AgenticCommerceCheckoutSession checkoutSession) {
        List<AgenticCommerceMessage> parsed = AgenticCommerceValues.maps(body, "messages").stream()
                .map(AgenticCommerceMessage::fromMap)
                .filter(message -> !message.isEmpty())
                .toList();
        return parsed.isEmpty() ? checkoutSession.messages() : parsed;
    }

    private static Map<String, Object> metadata(
            Map<String, Object> body,
            AgenticCommerceResponseValidationReport validation,
            AgenticCommerceCheckoutSession checkoutSession,
            AgenticCommerceError error) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("bodyPresent", !body.isEmpty());
        values.put("routeMatched", validation.route().isPresent());
        values.put("checkoutSessionPresent", !checkoutSession.isEmpty());
        values.put("errorPresent", !error.isEmpty());
        values.put("validationIssueCount", validation.issueCount());
        return AgenticCommerceMaps.copy(values);
    }
}
